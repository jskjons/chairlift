package com.rei.chairlift;

import java.util.LinkedHashMap;
import java.util.Map;

public class TemplateParams {
    private Map<String,  TemplateParam> params = new LinkedHashMap<>();
    
    public TemplateParam get(String name) {
        return params.get(name);
    }
    
    public void add(String name, String description, Object value) {
        params.put(name, new TemplateParam(name, description, value, value));
    }
    
    public void prompt(String name, String description, Object defaultValue) {
        TemplateParam param = new TemplateParam(name, description, null, defaultValue);
        params.put(name, param);
        
    }
}
