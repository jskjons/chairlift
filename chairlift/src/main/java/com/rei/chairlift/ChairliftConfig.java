package com.rei.chairlift;

import java.util.HashMap;
import java.util.Map;

public class ChairliftConfig {
    private boolean interactive;
    private Map<String, String> suppliedParameters = new HashMap<>();
    
    public ChairliftConfig() {
    }
    
    public ChairliftConfig(boolean interactive, Map<String, String> suppliedParameters) {
        this.interactive = interactive;
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
}