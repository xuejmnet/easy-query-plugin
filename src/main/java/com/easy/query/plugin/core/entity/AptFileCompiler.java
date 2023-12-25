package com.easy.query.plugin.core.entity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * create time 2023/11/8 15:09
 * 文件说明
 *
 * @author xuejiaming
 */
public class AptFileCompiler {
    private final String entityClassName;
    private final String entityClassProxyName;
    private final AptSelectorInfo selectorInfo;
    private final boolean kt;
    private final String packageName;
    private Set<String> imports;

    public AptFileCompiler(String packageName,String entityClassName,String entityClassProxyName,AptSelectorInfo selectorInfo,boolean kt) {
        this.packageName = packageName;
        this.entityClassName = entityClassName;
        this.entityClassProxyName = entityClassProxyName;
        this.selectorInfo = selectorInfo;
        this.kt = kt;
        this.imports = new LinkedHashSet<>();
    }

    public String getEntityClassProxyName() {
        return entityClassProxyName;
    }

    public String getEntityClassName() {
        return entityClassName;
    }

    public String getPackageName() {
        return packageName;
    }

    public Set<String> getImports() {
        return imports;
    }

    public void addImports(String fullClassPackageName) {
        if(fullClassPackageName!=null){
            imports.add("import " + fullClassPackageName + ";");
        }
    }
    public AptSelectorInfo getSelectorInfo() {
        return selectorInfo;
    }

    public boolean isJava() {
        return !kt;
    }
}
