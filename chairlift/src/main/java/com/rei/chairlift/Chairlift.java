package com.rei.chairlift;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.codehaus.groovy.control.CompilationFailedException;
import org.eclipse.aether.artifact.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rei.chairlift.util.AntPathMatcher;
import com.rei.chairlift.util.GroovyScriptUtils;

import groovy.text.SimpleTemplateEngine;

public class Chairlift {
    
    private static final Logger logger = LoggerFactory.getLogger(Chairlift.class);
    
    private ChairliftConfig globalConfig;
    private static final SimpleTemplateEngine TEMPLATE_ENGINE = new SimpleTemplateEngine();
    private static final Predicate<Path> NEVER_COPY = p -> {
        String filename = p.getFileName().toString();
        return !filename.equals(TemplateConfig.CONFIG_GROOVY) && 
               !filename.equals(TemplateConfig.POSTINSTALL_GROOVY) &&
               !filename.endsWith(".class") &&
               !p.toString().startsWith(TemplateConfig.SUBTEMPLATE_PREFIX);
    };
    
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    public Chairlift(ChairliftConfig globalConfig) {
        this.globalConfig = globalConfig;
    }
    
        
    public String generate(Artifact templateArtifact, Path projectDir) throws IOException {
        return generate(templateArtifact, null, projectDir);
    }
    
    public String generate(Artifact templateArtifact, String subtemplate, Path projectDir) throws IOException {
        TemplateArchive archive = new TemplateArchive(templateArtifact);
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
            } catch (CompilationFailedException | ClassNotFoundException | IOException e) {
                throw new RuntimeException("error processing template!", e);
            }
        };
    }
    
    private void runPostInstallScript(Path projectDir, String root, TemplateArchive archive, TemplateConfig config) throws IOException {
        archive.read(root + TemplateConfig.POSTINSTALL_GROOVY).ifPresent(scriptText -> {
            try {
                logger.info("running {}", TemplateConfig.POSTINSTALL_GROOVY);
                GroovyScriptUtils.runScript(config, 
                                            GroovyScriptUtils.getBinding(archive, globalConfig, projectDir), 
                                            scriptText);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Predicate<Path> anyMatch(List<String> patterns) {
        return path -> patterns.stream().anyMatch(pattern -> {
            return PATH_MATCHER.match(pattern, toMatchPath(path));
        });
    }

    private String toMatchPath(Path path) {
        String sanitized = path.toString().replace('\\', '/');
        if (sanitized.startsWith("/")) {
            return sanitized.substring(1);
        }
        return sanitized;
    }

}
