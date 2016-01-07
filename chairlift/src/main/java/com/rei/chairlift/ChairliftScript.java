package com.rei.chairlift;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import groovy.lang.Script;

public class ChairliftScript extends Script {

    private TemplateConfig config;

    public void setConfig(TemplateConfig config) {
        this.config = config;
    }
    
    public Map<String, Object> getParams() {
        return config.getParameterValues();
    }
    
    public Object param(String name, String description, Object defaultValue) {
        Object value = getParamValue(name, description, defaultValue);
        config.addParameterInfo(new ParameterInfo(name, description, defaultValue));
        config.setParameterValue(name, value);
        return value;
    }

    public void includeFiles(String... patterns) {
        config.getIncludedFiles().addAll(Arrays.asList(patterns));
    }
    
    public void excludeFiles(String... patterns) {
        config.getExcludedFiles().addAll(Arrays.asList(patterns));
    }
    
    public void processFiles(String... patterns) {
        config.getProcessedFiles().addAll(Arrays.asList(patterns));
    }
    
    public void passthroughFiles(String... patterns) {
        config.getUnprocessedFiles().addAll(Arrays.asList(patterns));
    }
    
    private Object getParamValue(String name, String description, Object defaultValue) {
        if (config.getGlobalConfig().getSuppliedParameters().containsKey(name)) {
            String value = config.getGlobalConfig().getSuppliedParameters().get(name);
            return convert(defaultValue, value);
        }
        
        if (config.getGlobalConfig().isInteractive()) {
            return convert(defaultValue, System.console().readLine("value for '%s' (%s) [%s]: ", name, description, defaultValue));
        }
        return defaultValue;
    }

    private Object convert(Object defaultValue, String value) {
        Class<? extends Object> paramType = defaultValue.getClass();
        if (paramType.equals(Boolean.TYPE) || paramType.equals(Boolean.class)) {
            return value.equalsIgnoreCase("y") || value.equalsIgnoreCase("true");
        }
        
        if (defaultValue instanceof Number) {
            return new BigDecimal(value);
        }
        
        return value;
    }

    @Override
    public Object run() {
        throw new UnsupportedOperationException("not meant to be used directly, use as a base script only!");
    }
}
