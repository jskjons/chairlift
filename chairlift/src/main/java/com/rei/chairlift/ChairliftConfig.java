package com.rei.chairlift;

import java.util.HashMap;
import java.util.Map;

public class ChairliftConfig {
    private boolean interactive;
    private Map<String, String> suppliedParameters = new HashMap<>();
    private boolean resolveDependencies;
    
    public ChairliftConfig() {
    }
    
    public ChairliftConfig(boolean interactive, boolean resolveDependencies, Map<String, String> suppliedParameters) {
        this.interactive = interactive;
        this.resolveDependencies = resolveDependencies;
        this.suppliedParameters = suppliedParameters;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public void setInteractive(boolean interactive) {
        this.interactive = interactive;
    }

    public Map<String, String> getSuppliedParameters() {
        return suppliedParameters;
    }

    public void setSuppliedParameters(Map<String, String> suppliedParameters) {
        this.suppliedParameters = suppliedParameters;
    }

    public boolean isResolveDependencies() {
        return resolveDependencies;
    }

    public void setResolveDependencies(boolean resolveDependencies) {
        this.resolveDependencies = resolveDependencies;
    }
}