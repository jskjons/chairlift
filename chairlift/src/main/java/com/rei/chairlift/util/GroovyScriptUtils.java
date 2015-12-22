package com.rei.chairlift.util;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import com.rei.chairlift.ChairliftConfig;
import com.rei.chairlift.ChairliftScript;
import com.rei.chairlift.TemplateArchive;
import com.rei.chairlift.TemplateConfig;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class GroovyScriptUtils {
    public static void runScript(TemplateConfig config, Binding binding, List<URL> classpath, String scriptText) throws IOException {
        GroovyShell shell = new GroovyShell(GroovyScriptUtils.class.getClassLoader(), binding, GroovyScriptUtils.getCompilerConfig(classpath));
        ChairliftScript script = (ChairliftScript) shell.parse(scriptText);
        script.setConfig(config);
        script.run();
    }
    
    public static CompilerConfiguration getCompilerConfig(List<URL> classpath) {
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        if (!classpath.isEmpty()) {
            compilerConfig.setClasspathList(classpath.stream().map(URL::toString).collect(toList()));
        }
        
        ImportCustomizer imports = new ImportCustomizer();
        imports.addStaticStars(NamingUtils.class.getName());
        compilerConfig.addCompilationCustomizers(imports);
        compilerConfig.setScriptBaseClass(ChairliftScript.class.getName());
        return compilerConfig;
    }

    public static Binding getBinding(TemplateArchive archive, ChairliftConfig globalConfig, Path projectDir) {
        Binding binding = new Binding();
        binding.setProperty("globalConfig", globalConfig);
        binding.setProperty("projectDir", projectDir.toFile());
        
        binding.setProperty("templateVersion", archive.getVersion());
        binding.setProperty("templateArtifactId", archive.getArtifactId());
        binding.setProperty("templateGroupId", archive.getGroupId());
        binding.setProperty("templateClassifier", archive.getClassifier());
        return binding;
    }
}
