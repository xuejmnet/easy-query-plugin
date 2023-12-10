package com.easy.query.plugin.core.entity;

import com.easy.query.plugin.core.util.StrUtil;

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
    public String getPropertyNameGetMethodName() {
        return "get"+ StrUtil.toUpperCaseFirstOne(propertyName);
    }

    public String getComment() {
        return comment;
    }
}
