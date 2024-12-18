package com.easy.query.plugin.core.entity.struct;

import com.easy.query.plugin.core.entity.ClassNode;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import lombok.Getter;
import lombok.Setter;

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
    @Getter
    private final Project project;
    @Getter
    private final String path;
    @Getter
    private final String packageName;
    @Getter
    private final Module module;
    @Getter
    private final Map<String, PsiClass> entityClass;

    /** 修改的时候的 dtoClassName 保存的时候用于回填 */
    @Getter
    @Setter
    private String dtoClassName;

    @Getter
    @Setter
    private PsiClass dtoPsiClass;

    public StructDTOEntityContext(Project project, String path, String packageName, Module module,
            Map<String, PsiClass> entityClass) {
        this.project = project;

        this.path = path;
        this.packageName = packageName;
        this.module = module;
        this.entityClass = entityClass;
    }

}
