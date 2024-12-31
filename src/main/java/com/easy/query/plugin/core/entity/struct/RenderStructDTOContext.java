package com.easy.query.plugin.core.entity.struct;

import com.easy.query.plugin.core.entity.PropAppendable;
import com.easy.query.plugin.core.entity.StructDTOApp;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * create time 2024/3/7 10:19
 * 构建DTO的上下文
 *
 * @author xuejiaming
 */
@Getter
public class RenderStructDTOContext {
    private final Project project;
    private final String path;
    private final String packageName;

    @Setter
    private String author;

    /** 需要一个 rootPsiClass, 是顶级DTO对应的实体 */
    @Getter
    @Setter
    private PsiClass rootEntityPsiClass;

    /** 顶级DTO的 psiClass, 修改时不为null, 方便用于替换 */
    @Getter
    @Setter
    private PsiClass rootDtoPsiClass;

    @Setter
    private String dtoName;
    private final StructDTOApp dtoApp;
    private final Module module;
    @Setter
    private boolean data;
    private final List<PropAppendable> entities;
    private final Set<String> imports;

    /** 是否删除现有文件 */
    @Setter
    private Boolean deleteExistsFile = false;

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

    public boolean hasEntities() {
        return !entities.isEmpty();
    }

    public String getEntityClassName(){
        return dtoApp.getEntityName();
    }

}
