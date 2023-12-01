package com.easy.query.plugin.core.entity;

/**
 * create time 2023/11/30 17:22
 * 文件说明
 *
 * @author xuejiaming
 */
public class MatchTypeMapping {
    private String type;

    private String javaField;
    private String columType;

    public MatchTypeMapping(String type, String javaField, String columType) {
        this.type = type;
        this.javaField = javaField;
        this.columType = columType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getJavaField() {
        return javaField;
    }

    public void setJavaField(String javaField) {
        this.javaField = javaField;
    }

    public String getColumType() {
        return columType;
    }

    public void setColumType(String columType) {
        this.columType = columType;
    }
}

