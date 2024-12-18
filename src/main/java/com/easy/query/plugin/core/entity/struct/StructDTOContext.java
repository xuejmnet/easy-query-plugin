package com.easy.query.plugin.core.entity.struct;

import com.easy.query.plugin.core.entity.ClassNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * create time 2024/3/7 09:56
 * 文件说明
 *
 * @author xuejiaming
 */
@Getter
public class StructDTOContext {
    private final Project project;
    private final String path;
    private final String packageName;
    private final Module module;
    private final Map<String, Map<String,ClassNode>> entityProps;
    private final Set<String> imports;
    @Setter
    private boolean success;

    /** 修改DTO的时候选中的DTO类名 */
    @Setter
    private String dtoClassName;
    /** 修改DTO的时候选中的dto PsiClass */
    @Setter
    private PsiClass dtoPsiClass;

    public StructDTOContext(Project project, String path, String packageName, Module module, Map<String, Map<String,ClassNode>> entityProps) {
        this.project = project;

        this.path = path;
        this.packageName = packageName;
        this.module = module;
        this.entityProps = entityProps;
        this.imports=new LinkedHashSet<>();
    }

}
