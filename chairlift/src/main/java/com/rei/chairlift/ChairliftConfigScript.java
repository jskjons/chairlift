package com.rei.chairlift;

import groovy.lang.Script;

public class ChairliftConfigScript extends Script {

    private TemplateConfig config;
    private ChairliftConfig globalConfig;

    @Override
    public Object run() {
        throw new UnsupportedOperationException("not meant to be used directly, use as a base script only!");
    }

    public void setConfig(TemplateConfig config) {
        this.config = config;
    }

    public void setGlobalConfig(ChairliftConfig globalConfig) {
        this.globalConfig = globalConfig;
    }

}
