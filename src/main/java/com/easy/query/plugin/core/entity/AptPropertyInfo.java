package com.easy.query.plugin.core.entity;

/**
 * create time 2023/9/16 12:12
 * 文件说明
 *
 * @author xuejiaming
 */
public class AptPropertyInfo {
    /**
     * 属性名
     */
    private final String propertyName;
    /**
     * 注释内容
     */
    private final String comment;
    /**
     * 代理对象泛型
     */
    private final String proxyEntityName;
    /**
     * 属性类型
     */
    private final String propertyType;
    /**
     * 对象名
     */
    private final String entityName;

    public AptPropertyInfo(String proxyEntityName,String propertyName, String propertyType, String comment, String entityName){

        this.proxyEntityName = proxyEntityName;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.comment = comment;
        this.entityName = entityName;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public String getComment() {
        return comment;
    }

    public String getProxyEntityName() {
        return proxyEntityName;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public String getEntityName() {
        return entityName;
    }
}
