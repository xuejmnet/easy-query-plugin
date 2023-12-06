package com.easy.query.plugin.core.entity;

/**
 * create time 2023/12/6 10:00
 * 文件说明
 *
 * @author xuejiaming
 */
public class AptSelectPropertyInfo {
    private final String propertyName;
    private final String comment;

    public AptSelectPropertyInfo(String propertyName, String comment){

        this.propertyName = propertyName;
        this.comment = comment;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getComment() {
        return comment;
    }
}
