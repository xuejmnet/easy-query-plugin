package com.easy.query.plugin.core.config;

import com.alibaba.fastjson2.JSON;
import com.easy.query.plugin.core.constant.EasyQueryConstant;
import com.intellij.ui.tabs.TabInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * create time 2023/11/30 11:35
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyQueryConfig {
    /**
     * 作者
     */
    private String author;
    /**
     * 版本
     */
    private String since;
    /**
     * 表前缀
     */
    private String tablePrefix;
    /**
     * 是否生成builder
     */
    private boolean builder;
    /**
     * 是否生成data
     */
    private boolean data;
    /**
     * 是否生成allArgsConstructor
     */
    private boolean allArgsConstructor;
    /**
     * 是否生成noArgsConstructor
     */
    private boolean noArgsConstructor;
    /**
     * 是否生成swagger
     */
    private boolean swagger;

    private boolean swagger3;
    /**
     * 实体模板
     */
    private String modelTemplate;

    //=============包名

    /**
     * 实体包名
     */
    private String modelPackage;

    //=============文件路径

    /**
     * 实体文件路径
     *
     * @return
     */
    private String modelModule;

    private String modelSuffix;
    private String domainPath;

    private String modelSuperClass;

    public String getModelSuperClass() {
        return modelSuperClass;
    }

    public void setModelSuperClass(String modelSuperClass) {
        this.modelSuperClass = modelSuperClass;
    }
//
//    public String getDataSource() {
//        return dataSource;
//    }
//
//    public void setDataSource(String dataSource) {
//        this.dataSource = dataSource;
//    }
//
    public String getSuffix() {
        if(modelSuffix==null){
            return "";
        }
        return modelSuffix;
    }


    public Map<String, String> getPackages() {
        Map<String, String> data = new HashMap<>();
        data.put(EasyQueryConstant.ENTITY, modelPackage);
        return data;
    }

    public String getModelSuffix() {
        return modelSuffix;
    }

    public void setModelSuffix(String modelSuffix) {
        this.modelSuffix = modelSuffix;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getSince() {
        return since;
    }

    public void setSince(String since) {
        this.since = since;
    }

    public String getTablePrefix() {
        return tablePrefix;
    }

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
    }

    public boolean isBuilder() {
        return builder;
    }

    public void setBuilder(boolean builder) {
        this.builder = builder;
    }

    public boolean isData() {
        return data;
    }

    public void setData(boolean data) {
        this.data = data;
    }

    public boolean isAllArgsConstructor() {
        return allArgsConstructor;
    }

    public void setAllArgsConstructor(boolean allArgsConstructor) {
        this.allArgsConstructor = allArgsConstructor;
    }

    public boolean isNoArgsConstructor() {
        return noArgsConstructor;
    }

    public void setNoArgsConstructor(boolean noArgsConstructor) {
        this.noArgsConstructor = noArgsConstructor;
    }

    public boolean isSwagger() {
        return swagger;
    }

    public void setSwagger(boolean swagger) {
        this.swagger = swagger;
    }

    public String getModelTemplate() {
        return modelTemplate;
    }

    public void setModelTemplate(String modelTemplate) {
        this.modelTemplate = modelTemplate;
    }

    public String getModelPackage() {
        return modelPackage;
    }

    public void setModelPackage(String modelPackage) {
        this.modelPackage = modelPackage;
    }

    public String getModelModule() {
        return modelModule;
    }

    public void setModelModule(String modelModule) {
        this.modelModule = modelModule;
    }

    public boolean isSwagger3() {
        return swagger3;
    }

    public void setSwagger3(boolean swagger3) {
        this.swagger3 = swagger3;
    }

    public String getDomainPath() {
        return domainPath;
    }

    public void setDomainPath(String domainPath) {
        this.domainPath = domainPath;
    }
}
