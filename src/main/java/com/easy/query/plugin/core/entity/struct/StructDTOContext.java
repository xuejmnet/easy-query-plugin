package com.easy.query.plugin.core.entity.struct;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * create time 2024/3/7 09:56
 * 文件说明
 *
 * @author xuejiaming
 */
public class StructDTOContext {
    private final Project project;
    private final String path;
    private final String packageName;
    private final Module module;
    private final Set<String> imports;

    public StructDTOContext(Project project, String path, String packageName, Module module) {
        this.project = project;

        this.path = path;
        this.packageName = packageName;
        this.module = module;
        this.imports=new LinkedHashSet<>();
    }

    public String getPath() {
        return path;
    }

    public String getPackageName() {
        return packageName;
    }

    public Project getProject() {
        return project;
    }

    public Module getModule() {
        return module;
    }

    public Set<String> getImports() {
        return imports;
    }
}