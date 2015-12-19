package com.rei.chairlift;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rei.chairlift.util.GroovyScriptUtils;

import groovy.lang.Binding;

public class TemplateConfig {
    public static final String CONFIG_GROOVY = "/config.groovy";
    public static final String POSTINSTALL_GROOVY_RELATIVE = "postinstall.groovy";
    public static final String POSTINSTALL_GROOVY = "/" + POSTINSTALL_GROOVY_RELATIVE;
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
        
        Binding binding = GroovyScriptUtils.getBinding(archive, globalConfig, projectDir);
        config.parameterValues.putAll(binding.getVariables());
        
        GroovyScriptUtils.runScript(config, binding, archive.read(CONFIG_GROOVY).get());

        if (config.getIncludedFiles().isEmpty()) {
            config.getIncludedFiles().add(DEFAULT_INCLUDES);
        }
        
        if (config.getProcessedFiles().isEmpty()) {
            config.getProcessedFiles().add(DEFAULT_PROCESSED);
        }        
        
        return config;
    }
}