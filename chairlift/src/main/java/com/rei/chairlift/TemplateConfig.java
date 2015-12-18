package com.rei.chairlift;

import java.io.IOException;
import java.nio.file.Path;

import org.codehaus.groovy.control.CompilerConfiguration;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class TemplateConfig {
    private static final String CONFIG_GROOVY = "/config.groovy";
    
    private TemplateParams parameters = new TemplateParams();
    
    public TemplateParams getParams() {
        return parameters;
    }

    public static TemplateConfig load(TemplateArchive archive, ChairliftConfig globalConfig, Path projectDir) throws IOException {
        TemplateConfig config = new TemplateConfig();
                
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        Binding binding = new Binding();
        binding.setProperty("globalConfig", globalConfig);
        binding.setProperty("projectDir", projectDir);
        binding.setProperty("templateVersion", archive.getVersion());
        binding.setProperty("templateArtifactId", archive.getArtifactId());
        binding.setProperty("templateGroupId", archive.getGroupId());
        binding.setProperty("templateClassifier", archive.getClassifier());
        
        compilerConfig.setScriptBaseClass(ChairliftConfigScript.class.getName());
        GroovyShell shell = new GroovyShell(TemplateConfig.class.getClassLoader(), compilerConfig);
        ChairliftConfigScript script = (ChairliftConfigScript) shell.parse(archive.read(CONFIG_GROOVY));
        script.setConfig(config);
        script.setGlobalConfig(globalConfig);
        script.run();
        
        return config;
    }
}
