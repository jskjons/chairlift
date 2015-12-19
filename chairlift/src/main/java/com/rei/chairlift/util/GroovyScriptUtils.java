package com.rei.chairlift.util;

import java.io.IOException;
import java.nio.file.Path;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import com.rei.chairlift.ChairliftConfig;
import com.rei.chairlift.ChairliftScript;
import com.rei.chairlift.TemplateArchive;
import com.rei.chairlift.TemplateConfig;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class GroovyScriptUtils {
    public static void runScript(TemplateConfig config, Binding binding, String scriptText) throws IOException {
        GroovyShell shell = new GroovyShell(GroovyScriptUtils.class.getClassLoader(), binding, GroovyScriptUtils.getCompilerConfig());
        ChairliftScript script = (ChairliftScript) shell.parse(scriptText);
        script.setConfig(config);
        script.run();
    }
    
    public static CompilerConfiguration getCompilerConfig() {
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
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
