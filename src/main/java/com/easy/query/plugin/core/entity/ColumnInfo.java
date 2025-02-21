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
    private String firstLowName;
    private String lowName;

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
    private String jdbcTypeStr;

    /**
     * 是否必填
     */
    private boolean notNull;
    private int size;

    public boolean isNotNull() {
        return notNull;
    }

    public void setNotNull(boolean notNull) {
        this.notNull = notNull;
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

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getJdbcTypeStr() {
        return jdbcTypeStr;
    }

    public void setJdbcTypeStr(String jdbcTypeStr) {
        this.jdbcTypeStr = jdbcTypeStr;
    }

    public String getFirstLowName() {
        return firstLowName;
    }

    public void setFirstLowName(String firstLowName) {
        this.firstLowName = firstLowName;
    }

    public String getLowName() {
        return lowName;
    }

    public void setLowName(String lowName) {
        this.lowName = lowName;
    }

    @Override
    public String toString() {
        return "ColumnInfo{" +
            "name='" + name + '\'' +
            ", firstLowName='" + firstLowName + '\'' +
            ", lowName='" + lowName + '\'' +
            ", fieldName='" + fieldName + '\'' +
            ", fieldType='" + fieldType + '\'' +
            ", type='" + type + '\'' +
            ", comment='" + comment + '\'' +
            ", primaryKey=" + primaryKey +
            ", isAutoIncrement=" + isAutoIncrement +
            ", methodName='" + methodName + '\'' +
            ", jdbcTypeStr='" + jdbcTypeStr + '\'' +
            ", notNull=" + notNull +
            ", size=" + size +
            '}';
    }
}