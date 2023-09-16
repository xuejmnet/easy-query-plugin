package com.easy.query.plugin.core.config;


import cn.hutool.core.util.StrUtil;

public class CustomConfig {
    /**
     * 全局启用apt开关
     */
    private Boolean enable;

    /**
     * 是否停止向上级合并配
     */
    private boolean stopBubbling;

    /**
     * APT 代码生成路径
     */
    private String genPath;

    /**
     * APT 代码生成文件字符集
     */
    private String charset;

    /**
     * 是否所有的类都生成在 Tables 类里
     */
    private boolean allInTablesEnable;

    /**
     * Tables 包名
     */
    private String allInTablesPackage;

    /**
     * Tables 类名
     */
    private String allInTablesClassName;

    /**
     * 开启 Mapper 自动生成
     */
    private boolean mapperGenerateEnable;

    /**
     * 开启 @Mapper 注解
     */
    private boolean mapperAnnotation;

    /**
     * 自定义 Mapper 的父类
     */
    private String mapperBaseClass;

    /**
     * 自定义 Mapper 生成的包名
     */
    private String mapperPackage;

    /**
     * 生成辅助类的字段风格
     */
    private String tableDefPropertiesNameStyle;

    /**
     * 生成的表对应的变量后缀
     */
    private String tableDefInstanceSuffix;

    /**
     * 生成的 TableDef 类的后缀
     */
    private String tableDefClassSuffix;

    /**
     * 过滤 Entity 后缀
     */
    private String tableDefIgnoreEntitySuffixes;


    public Boolean isEnable() {
        return enable;
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public boolean isStopBubbling() {
        return stopBubbling;
    }

    public void setStopBubbling(boolean stopBubbling) {
        this.stopBubbling = stopBubbling;
    }

    public String getGenPath() {
        return genPath;
    }

    public void setGenPath(String genPath) {
        this.genPath = genPath;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public boolean isAllInTablesEnable() {
        return allInTablesEnable;
    }

    public void setAllInTablesEnable(boolean allInTablesEnable) {
        this.allInTablesEnable = allInTablesEnable;
    }

    public String getAllInTablesPackage() {
        return allInTablesPackage;
    }

    public void setAllInTablesPackage(String allInTablesPackage) {
        this.allInTablesPackage = allInTablesPackage;
    }

    public String getAllInTablesClassName() {
        return allInTablesClassName;
    }

    public void setAllInTablesClassName(String allInTablesClassName) {
        this.allInTablesClassName = allInTablesClassName;
    }

    public boolean isMapperGenerateEnable() {
        return mapperGenerateEnable;
    }

    public void setMapperGenerateEnable(boolean mapperGenerateEnable) {
        this.mapperGenerateEnable = mapperGenerateEnable;
    }

    public boolean isMapperAnnotation() {
        return mapperAnnotation;
    }

    public void setMapperAnnotation(boolean mapperAnnotation) {
        this.mapperAnnotation = mapperAnnotation;
    }

    public String getMapperBaseClass() {
        return mapperBaseClass;
    }

    public void setMapperBaseClass(String mapperBaseClass) {
        this.mapperBaseClass = mapperBaseClass;
    }

    public String getMapperPackage() {
        return mapperPackage;
    }

    public void setMapperPackage(String mapperPackage) {
        this.mapperPackage = mapperPackage;
    }

    public String getTableDefPropertiesNameStyle() {
        return tableDefPropertiesNameStyle;
    }

    public void setTableDefPropertiesNameStyle(String tableDefPropertiesNameStyle) {
        this.tableDefPropertiesNameStyle = tableDefPropertiesNameStyle;
    }

    public String getTableDefInstanceSuffix() {
        return tableDefInstanceSuffix;
    }

    public void setTableDefInstanceSuffix(String tableDefInstanceSuffix) {
        this.tableDefInstanceSuffix = tableDefInstanceSuffix;
    }

    public String getTableDefClassSuffix() {
        return tableDefClassSuffix;
    }

    public void setTableDefClassSuffix(String tableDefClassSuffix) {
        this.tableDefClassSuffix = tableDefClassSuffix;
    }

    public String getTableDefIgnoreEntitySuffixes() {
        return tableDefIgnoreEntitySuffixes;
    }

    public void setTableDefIgnoreEntitySuffixes(String tableDefIgnoreEntitySuffixes) {
        this.tableDefIgnoreEntitySuffixes = tableDefIgnoreEntitySuffixes;
    }

    public static String getConfig(String value, String defaultJava, String defaultKt, boolean isMaven) {
        if (StrUtil.isNotBlank(value)) {
            return value;
        }
        return isMaven ? defaultJava : defaultKt;

    }

    @Override
    public String toString() {
        return "CustomConfig{" +
                "enable=" + enable +
                ", stopBubbling=" + stopBubbling +
                ", genPath='" + genPath + '\'' +
                ", charset='" + charset + '\'' +
                ", allInTablesEnable=" + allInTablesEnable +
                ", allInTablesPackage='" + allInTablesPackage + '\'' +
                ", allInTablesClassName='" + allInTablesClassName + '\'' +
                ", mapperGenerateEnable=" + mapperGenerateEnable +
                ", mapperAnnotation=" + mapperAnnotation +
                ", mapperBaseClass='" + mapperBaseClass + '\'' +
                ", mapperPackage='" + mapperPackage + '\'' +
                ", tableDefPropertiesNameStyle='" + tableDefPropertiesNameStyle + '\'' +
                ", tableDefInstanceSuffix='" + tableDefInstanceSuffix + '\'' +
                ", tableDefClassSuffix='" + tableDefClassSuffix + '\'' +
                ", tableDefIgnoreEntitySuffixes='" + tableDefIgnoreEntitySuffixes + '\'' +
                '}';
    }
}
