package com.rei.chairlift;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ImportCustomizer;

import com.rei.chairlift.util.NamingUtils;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;

public class TemplateConfig {
    public static final String CONFIG_GROOVY = "/config.groovy";
    public static final String DEFAULT_INCLUDES = "**/*";
    public static final String DEFAULT_PROCESSED = "**/*";

    private ChairliftConfig globalConfig;
    
    private Map<String, ParameterInfo> parameterInfo = new LinkedHashMap<>();
    private Map<String, Object> parameterValues;
    
    private List<String> includedFiles = new ArrayList<>();
    private List<String> excludedFiles = new ArrayList<>();
    
    private List<String> processedFiles = new ArrayList<>();
    private List<String> unprocessedFiles = new ArrayList<>();
    
    public TemplateConfig(ChairliftConfig globalConfig) {
        this.globalConfig = globalConfig;
        parameterValues = new LinkedHashMap<>(globalConfig.getSuppliedParameters());
    }

    public void addParameterInfo(ParameterInfo param) {
        parameterInfo.put(param.getName(), param);
    }
    
    public List<String> getIncludedFiles() {
        return includedFiles;
    }

    public List<String> getExcludedFiles() {
        return excludedFiles;
    }

    public List<String> getProcessedFiles() {
        return processedFiles;
    }

    public List<String> getUnprocessedFiles() {
        return unprocessedFiles;
    }

    public ChairliftConfig getGlobalConfig() {
        return globalConfig;
    }
    
    public Map<String, ParameterInfo> getParameterInfo() {
        return parameterInfo;
    }
    
    public Map<String, Object> getParameterValues() {
        return parameterValues;
    }
    
    public void setParameterValue(String name, Object value) {
        parameterValues.put(name, value);
    }
    
    @SuppressWarnings("unchecked")
    public static TemplateConfig load(TemplateArchive archive, ChairliftConfig globalConfig, Path projectDir)
            throws IOException {
        TemplateConfig config = new TemplateConfig(globalConfig);
        
        Binding binding = getBinding(archive, globalConfig, projectDir);
        config.parameterValues.putAll(binding.getVariables());
        
        CompilerConfiguration compilerConfig = getCompilerConfig();
        GroovyShell shell = new GroovyShell(TemplateConfig.class.getClassLoader(), binding, compilerConfig);
        ChairliftConfigScript script = (ChairliftConfigScript) shell.parse(archive.read(CONFIG_GROOVY));
        script.setConfig(config);
        script.run();

        if (config.getIncludedFiles().isEmpty()) {
            config.getIncludedFiles().add(DEFAULT_INCLUDES);
        }
        
        if (config.getProcessedFiles().isEmpty()) {
            config.getProcessedFiles().add(DEFAULT_PROCESSED);
        }        
        
        return config;
    }

    private static CompilerConfiguration getCompilerConfig() {
        CompilerConfiguration compilerConfig = new CompilerConfiguration();
        ImportCustomizer imports = new ImportCustomizer();
        imports.addStaticStars(NamingUtils.class.getName());
        compilerConfig.addCompilationCustomizers(imports);
        compilerConfig.setScriptBaseClass(ChairliftConfigScript.class.getName());
        return compilerConfig;
    }

    private static Binding getBinding(TemplateArchive archive, ChairliftConfig globalConfig, Path projectDir) {
        Binding binding = new Binding();
        binding.setProperty("globalConfig", globalConfig);
        binding.setProperty("projectDir", projectDir);
        
        binding.setProperty("templateVersion", archive.getVersion());
        binding.setProperty("templateArtifactId", archive.getArtifactId());
        binding.setProperty("templateGroupId", archive.getGroupId());
        binding.setProperty("templateClassifier", archive.getClassifier());
        return binding;
    }
}
