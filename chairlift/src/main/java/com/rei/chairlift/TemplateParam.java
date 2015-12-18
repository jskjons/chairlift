package com.rei.chairlift;

public class TemplateParam {
    private String name;
    private String description;
    private Object value;
    private Object defaultValue;
    
    public TemplateParam(String name, String description, Object value, Object defaultValue) {
        this.name = name;
        this.description = description;
        this.value = value;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(Object defaultValue) {
        this.defaultValue = defaultValue;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
