package com.easy.query.plugin.core.entity;

import com.easy.query.plugin.core.util.StrUtil;

/**
 * create time 2024/1/30 13:32
 * 文件说明
 *
 * @author xuejiaming
 */
public class AnonymousParseResult {
    private final String propertyName;
    private final String methodName;
    private final String propertyFullType;
    private final String propertyType;
    private final String targetInvokeName;

    public AnonymousParseResult(String propertyName, String propertyType, String targetInvokeName) {

        this.propertyName = propertyName;
        this.propertyFullType = propertyType;
        this.propertyType = propertyType.substring(propertyType.lastIndexOf(".") + 1);
        this.methodName = StrUtil.upperFirst(propertyName);
        this.targetInvokeName = targetInvokeName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public String getTargetInvokeName() {
        return targetInvokeName;
    }

    public String getPropertyFullType() {
        return propertyFullType;
    }

    public String getMethodName() {
        return methodName;
    }
}