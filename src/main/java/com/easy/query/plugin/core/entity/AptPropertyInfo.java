package com.easy.query.plugin.core.entity;

import com.easy.query.plugin.core.enums.BeanPropTypeEnum;
import com.easy.query.plugin.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

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
    private final PropertyColumn propertyColumn;
    /**
     * 注释内容
     */
    private final String comment;
    /**
     * 属性类型
     */
    /**
     * 对象名
     */
    private final String entityName;
    private final boolean valueObject;
    private final String owner;
    private final boolean includeProperty;
    private final boolean includeManyProperty;
    private final BeanPropTypeEnum beanPropType;
    private final String sqlColumn;
    private final String sqlColumnMethod;
    private final String proxyPropertyName;

    public AptPropertyInfo(String propertyName, PropertyColumn propertyColumn, String comment, String entityName, boolean valueObject, String owner, boolean includeProperty, boolean includeManyProperty, String proxyPropertyName, BeanPropTypeEnum beanPropType){

        this.propertyName = propertyName;
        this.propertyColumn = propertyColumn;
        this.comment = comment;
        this.entityName = entityName;
        this.valueObject = valueObject;
        this.owner = owner;
        this.includeProperty = includeProperty;
        this.includeManyProperty = includeManyProperty;
        this.beanPropType = beanPropType;
        this.sqlColumn = includeProperty?"SQLNavigateColumn":propertyColumn.getSqlColumnName();
        this.sqlColumnMethod = includeProperty?"getNavigate":propertyColumn.getSQLColumnMethod();
        this.proxyPropertyName = proxyPropertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
    public String getProxyPropertyName() {
        if(StringUtils.isNotBlank(proxyPropertyName)){
            return proxyPropertyName;
        }
        return propertyName;
    }
    public String getPropertyNameGetMethodName() {
        if(beanPropType==BeanPropTypeEnum.IS){
            return "is"+ StrUtil.toUpperCaseFirstOne(propertyName);
        }
        return "get"+ StrUtil.toUpperCaseFirstOne(propertyName);
    }

    public String getComment() {
        return comment;
    }

    public String getPropertyType() {
        return propertyColumn.getPropertyType();
    }

    public String getPropertyTypeClass() {
        return propertyColumn.getPropertyTypeClass(includeProperty);
    }

    public String getEntityName() {
        return entityName;
    }

    public boolean isValueObject() {
        return valueObject;
    }

    public String getOwner() {
        return owner;
    }


    public String getSqlColumn() {
        return sqlColumn;
    }

    public boolean isIncludeAndHasNavigateProxyName(){
        return includeProperty&&getNavigateProxyName()!=null;
    }

    public String getSqlColumnMethod() {
        return sqlColumnMethod;
    }

    public boolean isIncludeProperty() {
        return includeProperty;
    }

    public boolean isIncludeManyProperty() {
        return includeManyProperty;
    }
    public String getNavigateProxyName(){
        return propertyColumn.getNavigateProxyName();
    }
    public boolean isAnyType(){
        return propertyColumn.isAnyType();
    }

}
