package com.easy.query.plugin.core.entity.struct;

import com.easy.query.plugin.core.entity.ClassNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * create time 2024/3/7 09:56
 * 文件说明
 *
 * @author xuejiaming
 */
public class StructDTOEntityContext {
    private final Project project;
    private final String path;
    private final String packageName;
    private final Module module;
    private final Map<String, PsiClass> entityClass;

    public StructDTOEntityContext(Project project, String path, String packageName, Module module, Map<String, PsiClass> entityClass) {
        this.project = project;

        this.path = path;
        this.packageName = packageName;
        this.module = module;
        this.entityClass = entityClass;
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

    public Map<String, PsiClass> getEntityClass() {
        return entityClass;
    }
}
