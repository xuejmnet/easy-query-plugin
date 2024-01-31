package com.easy.query.plugin.core.entity;

import com.intellij.openapi.project.Project;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * create time 2024/1/30 14:45
 * 文件说明
 *
 * @author xuejiaming
 */
public class AnonymousParseContext {
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

    private boolean accessors;
    private boolean requiredArgsConstructor;

    private final Map<String, AnonymousParseResult> anonymousParseResultMap;
    private int start;
    private int end;
    private String anonymousName;
    private String packageWithClassName;
    private String packageWithSimpleClassName;

    private boolean entityProxy=true;
    private boolean entityFileProxy=false;
    private Set<String> importClassList;
    private String modelPackage;
    private Project project;
    private String moduleName;

    public AnonymousParseContext() {
        this.anonymousParseResultMap = new LinkedHashMap<>();
        this.importClassList = new LinkedHashSet<>();
    }

    public Map<String, AnonymousParseResult> getAnonymousParseResultMap() {
        return anonymousParseResultMap;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public String getAnonymousName() {
        return anonymousName;
    }

    public void setAnonymousName(String anonymousName) {
        this.anonymousName = anonymousName;
    }

    public String getPackageWithClassName() {
        return packageWithClassName;
    }

    public void setPackageWithClassName(String packageWithClassName) {
        this.packageWithClassName = packageWithClassName;
    }

    public String getPackageWithSimpleClassName() {
        return packageWithSimpleClassName;
    }

    public void setPackageWithSimpleClassName(String packageWithSimpleClassName) {
        this.packageWithSimpleClassName = packageWithSimpleClassName;
    }

    public boolean isEntityProxy() {
        return entityProxy;
    }

    public void setEntityProxy(boolean entityProxy) {
        this.entityProxy = entityProxy;
    }

    public boolean isEntityFileProxy() {
        return entityFileProxy;
    }

    public void setEntityFileProxy(boolean entityFileProxy) {
        this.entityFileProxy = entityFileProxy;
    }

    public Set<String> getImportClassList() {
        return importClassList;
    }

    public String getModelPackage() {
        return modelPackage;
    }

    public void setModelPackage(String modelPackage) {
        this.modelPackage = modelPackage;
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

    public boolean isAccessors() {
        return accessors;
    }

    public void setAccessors(boolean accessors) {
        this.accessors = accessors;
    }

    public boolean isRequiredArgsConstructor() {
        return requiredArgsConstructor;
    }

    public void setRequiredArgsConstructor(boolean requiredArgsConstructor) {
        this.requiredArgsConstructor = requiredArgsConstructor;
    }

    public void setImportClassList(Set<String> importClassList) {
        this.importClassList = importClassList;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }
}
