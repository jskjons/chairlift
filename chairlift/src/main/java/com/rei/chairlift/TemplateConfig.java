package com.rei.chairlift;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.rei.chairlift.util.GroovyScriptUtils;

import groovy.lang.Binding;

public class TemplateConfig {
    public static final String SUBTEMPLATE_PREFIX = "subtemplate-";
    public static final String CONFIG_GROOVY = "config.groovy";
    public static final String POSTINSTALL_GROOVY = "postinstall.groovy";
    public static final String DEFAULT_INCLUDES = "**/*";
    public static final String DEFAULT_PROCESSED = "**/*";
    public static final String DEFAULT_TEMPLATE = "template";

    private ChairliftConfig globalConfig;
    
    private Map<String, ParameterInfo> parameterInfo = new LinkedHashMap<>();
    private Map<String, Object> parameterValues;
    
    private List<String> includedFiles = new ArrayList<>();
    private List<String> excludedFiles = new ArrayList<>();
    
    private List<String> processedFiles = new ArrayList<>();
    private List<String> unprocessedFiles = new ArrayList<>();
    
    private List<String> subtemplates = new ArrayList<>();
    private String basePath;
    
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
    
    public List<String> getSubtemplates() {
        return Collections.unmodifiableList(subtemplates);
    }
    
    public String getBasePath() {
        return basePath;
    }

    @SuppressWarnings("unchecked")
    public static TemplateConfig load(TemplateArchive archive, String subtemplate, ChairliftConfig globalConfig, Path projectDir)
            throws IOException {
        TemplateConfig config = new TemplateConfig(globalConfig);
        String basePath = "/" + (subtemplate != null ? SUBTEMPLATE_PREFIX + subtemplate : DEFAULT_TEMPLATE);

        if (subtemplate != null && !archive.exists(SUBTEMPLATE_PREFIX + subtemplate)) {
            throw new IllegalArgumentException("no subtemplate with name " + subtemplate);
        }
        
        Binding binding = GroovyScriptUtils.getBinding(archive, globalConfig, projectDir);
        config.parameterValues.putAll(binding.getVariables());
        
        String configPath = basePath + "/" + CONFIG_GROOVY;
        try {
            GroovyScriptUtils.runScript(config, binding, archive.getClasspath(), archive.read(configPath).get());
        } catch (Exception e) {
            throw new RuntimeException("error while running " + configPath, e);
        }

        if (config.getIncludedFiles().isEmpty()) {
            config.getIncludedFiles().add(DEFAULT_INCLUDES);
        }
        
        if (config.getProcessedFiles().isEmpty()) {
            config.getProcessedFiles().add(DEFAULT_PROCESSED);
        }
        
        if (subtemplate == null) {
            config.subtemplates = findSubtemplates(archive, config);
        }
        
        config.basePath = basePath;
        
        return config;
    }

    private static List<String> findSubtemplates(TemplateArchive archive, TemplateConfig config) throws IOException {
        return archive.list("/").stream()
                .filter(s -> s.startsWith(SUBTEMPLATE_PREFIX))
                .map(s -> s.replace(SUBTEMPLATE_PREFIX, ""))
                .collect(toList());
    }
}