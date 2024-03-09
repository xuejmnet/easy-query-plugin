package com.easy.query.plugin.core.entity.struct;

import com.easy.query.plugin.core.entity.PropAppendable;
import com.easy.query.plugin.core.entity.StructDTOApp;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * create time 2024/3/7 10:19
 * 文件说明
 *
 * @author xuejiaming
 */
public class RenderStructDTOContext {
    private final Project project;
    private final String path;
    private final String packageName;
    private String dtoName;
    private final StructDTOApp dtoApp;
    private final Module module;
    private boolean data;
    private final List<PropAppendable> entities;
    private final Set<String> imports;

    public RenderStructDTOContext(Project project, String path, String packageName, String dtoName, StructDTOApp dtoApp, Module module) {
        this.project = project;

        this.path = path;
        this.packageName = packageName;
        this.dtoName = dtoName;
        this.dtoApp = dtoApp;
        this.module = module;
        this.entities = new ArrayList<>();
        this.imports = new LinkedHashSet<>();
    }

    public Project getProject() {
        return project;
    }

    public String getPath() {
        return path;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getDtoName() {
        return dtoName;
    }

    public void setDtoName(String dtoName) {
        this.dtoName = dtoName;
    }

    public StructDTOApp getDtoApp() {
        return dtoApp;
    }

    public Module getModule() {
        return module;
    }

    public boolean hasEntities() {
        return entities.size()>0;
    }

    public List<PropAppendable> getEntities() {
        return entities;
    }

    public boolean isData() {
        return data;
    }

    public void setData(boolean data) {
        this.data = data;
    }

    public Set<String> getImports() {
        return imports;
    }

    public String getEntityClassName(){
        return dtoApp.getEntityName();
    }
}
