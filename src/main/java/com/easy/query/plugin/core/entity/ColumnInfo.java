package com.easy.query.plugin.core.entity;

/**
 * create time 2023/11/28 22:41
 * 文件说明
 *
 * @author xuejiaming
 */
public class ColumnInfo {
    /**
     * 列名
     */
    private String name;

    /**
     * 字段名
     */
    private String fieldName;
    private String fieldType;
    /**
     * 数据类型
     */
    private String type;
    /**
     * 列注释
     */
    private String comment;
    /**
     * 是主键
     */
    private boolean primaryKey;

    /**
     * 是否自动增长
     */
    private boolean isAutoIncrement;

    /**
     * 方法名称
     */
    private String methodName;

    /**
     * 是否必填
     */
    private boolean notNull;
    /**
     * 是否逻辑删除
     */
    private boolean logicDelete;

    private boolean tenant;

    private boolean version;

    private String insertValue;

    private String updateValue;

    public String getInsertValue() {
        return insertValue;
    }

    public void setInsertValue(String insertValue) {
        this.insertValue = insertValue;
    }

    public String getUpdateValue() {
        return updateValue;
    }

    public void setUpdateValue(String updateValue) {
        this.updateValue = updateValue;
    }

    public boolean isTenant() {
        return tenant;
    }

    public void setTenant(boolean tenant) {
        this.tenant = tenant;
    }

    public boolean isVersion() {
        return version;
    }

    public void setVersion(boolean version) {
        this.version = version;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public void setNotNull(boolean notNull) {
        notNull = notNull;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getFieldType() {
        return fieldType;
    }

    public void setFieldType(String fieldType) {
        this.fieldType = fieldType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public boolean isAutoIncrement() {
        return isAutoIncrement;
    }

    public void setAutoIncrement(boolean autoIncrement) {
        isAutoIncrement = autoIncrement;
    }


    public boolean isLogicDelete() {
        return logicDelete;
    }

    public void setLogicDelete(boolean logicDelete) {
        this.logicDelete = logicDelete;
    }

    @Override
    public String toString() {
        return "ColumnInfo{" +
                "name='" + name + '\'' +
                ", fieldName='" + fieldName + '\'' +
                ", fieldType='" + fieldType + '\'' +
                ", type='" + type + '\'' +
                ", comment='" + comment + '\'' +
                ", primaryKey=" + primaryKey +
                ", isAutoIncrement=" + isAutoIncrement +
                ", methodName='" + methodName + '\'' +
                ", notNull=" + notNull +
                ", logicDelete=" + logicDelete +
                '}';
    }
}