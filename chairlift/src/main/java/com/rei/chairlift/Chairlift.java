package com.rei.chairlift;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rei.chairlift.util.AntPathMatcher;
import com.rei.chairlift.util.GroovyScriptUtils;
import com.rei.chairlift.util.NamingUtils;

import groovy.lang.GroovyRuntimeException;
import groovy.lang.GroovyShell;
import groovy.text.SimpleTemplateEngine;

public class Chairlift {
    
    private static final String TEMPLATE_ERROR_MESSAGE = "error processing template!";

    private static final Logger logger = LoggerFactory.getLogger(Chairlift.class);
    
    private ChairliftConfig globalConfig;
    private static final SimpleTemplateEngine TEMPLATE_ENGINE = createTemplateEngine();

    
    private static final Predicate<Path> NEVER_COPY = p -> {
        if (p.getFileName() == null) {
            return true;
        }
        String filename = p.getFileName().toString();
        return !filename.equals(TemplateConfig.CONFIG_GROOVY) && 
               !filename.equals(TemplateConfig.POSTINSTALL_GROOVY) &&
               !filename.endsWith(".class") &&
               !filename.endsWith(".retain");
    };
    
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private DependencyResolver dependencyResolver = new DependencyResolver();
    
    public Chairlift(ChairliftConfig globalConfig) {
        this.globalConfig = globalConfig;
    }
    
    public String generate(String gavSpec, Path projectDir) throws IOException, ArtifactResolutionException {
        return generate(gavSpec, null, projectDir);
    }
    
    public String generate(String gavSpec, String subtemplate, Path projectDir) throws IOException, ArtifactResolutionException {
        return generate(dependencyResolver.resolveSingleArtifact(gavSpec), subtemplate, projectDir);
    }
    
    public String generate(Artifact templateArtifact, Path projectDir) throws IOException {
        return generate(templateArtifact, null, projectDir);
    }
    
    public String generate(Artifact templateArtifact, String subtemplate, Path projectDir) throws IOException {
        List<URL> classpath = resolveClasspath(templateArtifact);
        
        try (TemplateArchive archive = new TemplateArchive(templateArtifact, classpath)) {
            TemplateConfig config = TemplateConfig.load(archive, subtemplate, globalConfig, projectDir);
            
            archive.unpackTo(config.getBasePath(), projectDir, getCopyFilters(config), 
                    getProcessFilters(config), 
                    getRenameTransformer(config), 
                    getTemplateProcessor(config));
            
            runPostInstallScript(projectDir, config.getBasePath(), archive, config);
            
            Path readme = projectDir.resolve("README.md");
            if (Files.exists(readme)) {
                return new String(Files.readAllBytes(readme));
            }
            return "No readme assoicated with project!";
        }
    }

    List<Predicate<Path>> getCopyFilters(TemplateConfig config) {
        return Arrays.asList(NEVER_COPY, anyMatch(config.getIncludedFiles()), anyMatch(config.getExcludedFiles()).negate());
    }
    
    List<Predicate<Path>> getProcessFilters(TemplateConfig config) {
        return Arrays.asList(anyMatch(config.getProcessedFiles()), anyMatch(config.getUnprocessedFiles()).negate());
    }

    Function<Path, Path> getRenameTransformer(TemplateConfig config) {
        return in -> {
            String[] path = new String[] {in.toString()};
            config.getParameterValues().forEach((n, v) -> path[0] = path[0].replace("__" + n + "__", v.toString()));
            return Paths.get(path[0]);
        };
    }
    
    Function<String, String> getTemplateProcessor(TemplateConfig config) {
        return input -> {
            try {
                return TEMPLATE_ENGINE.createTemplate(input)
                                      .make(config.getParameterValues())
                                      .writeTo(new StringWriter())
                                      .toString();
            } catch (ClassNotFoundException | IOException | GroovyRuntimeException e) {
                throw new TemplatingException(TEMPLATE_ERROR_MESSAGE, e);
            }
        };
    }
    
    private void runPostInstallScript(Path projectDir, String root, TemplateArchive archive, TemplateConfig config) throws IOException {
        archive.read(root + "/" + TemplateConfig.POSTINSTALL_GROOVY).ifPresent(scriptText -> {
            try {
                logger.info("running {}", TemplateConfig.POSTINSTALL_GROOVY);
                GroovyScriptUtils.runScript(config, 
                                            GroovyScriptUtils.getBinding(archive, globalConfig, projectDir),
                                            archive.getClasspath(),
                                            scriptText);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Predicate<Path> anyMatch(List<String> patterns) {
        return path -> patterns.stream().anyMatch(pattern -> {
            String matchPath = toMatchPath(path);
            boolean matched = PATH_MATCHER.match(pattern, matchPath);
            logger.debug("pattern {} {} {}", pattern, matched ? "matched" : "did not match", matchPath);
            return matched;
        });
    }

    private String toMatchPath(Path path) {
        String sanitized = path.toString().replace('\\', '/');
        if (sanitized.startsWith("/")) {
            return sanitized.substring(1);
        }
        return sanitized;
    }

    private List<URL> resolveClasspath(Artifact artifact) {
        if (!globalConfig.isResolveDependencies()) {
            return Collections.emptyList();
        }
        
        return dependencyResolver.resolveDependencies(artifact).stream().map(a -> {
            try {
                return a.getFile().toURI().toURL();
            } catch (MalformedURLException e) {
                throw new IllegalStateException(e);
            }
        }).collect(toList());
    }
    
    private static SimpleTemplateEngine createTemplateEngine() {
        CompilerConfiguration compilerConfig = new CompilerConfiguration().addCompilationCustomizers(
                new ImportCustomizer().addStaticStars(NamingUtils.class.getName()));
        
        return new SimpleTemplateEngine(new GroovyShell(compilerConfig));
    }

}
