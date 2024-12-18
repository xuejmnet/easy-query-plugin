package com.easy.query.plugin.action;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.struct.StructDTOEntityContext;
import com.easy.query.plugin.core.util.MyModuleUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.windows.EntitySelectDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import javax.swing.*;
import java.io.File;
import java.util.*;

/**
 * create time 2024/11/23 14:52
 * 文件说明
 *
 * @author xuejiaming
 */
public class StructDTOModifyAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        // 从上下文中拿到当前的文件
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_FILE);
        if (!(psiElement instanceof PsiJavaFileImpl)) {
            // 不是java文件, 报错不处理
            Messages.showMessageDialog(project, "当前文件不是Java文件, 不处理", "错误", Messages.getErrorIcon());
            return;
        }

        PsiJavaFileImpl psiJavaFile = (PsiJavaFileImpl) psiElement;
        String path = psiJavaFile.getVirtualFile().getPath();
        if (StrUtil.isBlank(path)) {
            // 文件路径为空, 报错不处理
            Messages.showMessageDialog(project, "当前文件路径为空, 不处理", "错误", Messages.getErrorIcon());
            return;
        }

        // 如果当前文件路径不在 src/main/java 目录下, 报错不处理
        if (!path.contains("src/main/java")) {
            Messages.showMessageDialog(project, "当前文件不在 src/main/java 目录下, 不处理", "错误", Messages.getErrorIcon());
            return;
        }

        // 获取当前的 psiClass
        // psiClass 应该只有一个, 否则报错不处理
        PsiClass[] psiClasses = psiJavaFile.getClasses();
        if (psiClasses.length != 1) {
            Messages.showMessageDialog(project, "当前文件中应该只有一个类", "错误", Messages.getErrorIcon());
            return;
        }

        PsiClass dtoPsiClass = psiClasses[0];
        // 获取当前的DTO 文档注释
        String dtoClassDocComment = Optional.ofNullable(dtoPsiClass.getDocComment()).map(PsiDocComment::getText)
            .orElse("");
        // 尝试从 文档注释中获取 实体类名
        if (!ReUtil.contains("\\{@link *(\\S+) *\\}", dtoClassDocComment)) {
            Messages.showMessageDialog(project, "当前DTO类没有指定实体类", "错误", Messages.getErrorIcon());
            return;
        }

        String mainEntityClass;
        String mainEntityClassFromLink = ReUtil.getGroup1("\\{@link *(\\S+) *\\}", dtoClassDocComment);
        // 如果 mainEntityClass 中不包含 . , 则需要通过 import 去进行二次匹配
        if (!mainEntityClassFromLink.contains(".")) {
            Set<String> importInfoSet = PsiJavaFileUtil.getQualifiedNameImportSet((PsiJavaFile) dtoPsiClass.getContainingFile());
            mainEntityClass = importInfoSet
                .stream().filter(s -> s.endsWith("." + mainEntityClassFromLink))
                .findFirst().orElseGet(() -> {
                    // 说明是一个包下面的
                    String samePackageEntityClass = ((PsiJavaFile) dtoPsiClass.getParent()).getPackageName() + "." + mainEntityClassFromLink;
                    // 看看这个 samePackageEntityClass 是否存在
                    if (Objects.isNull(PsiJavaFileUtil.getPsiClass(project, samePackageEntityClass))) {
                        // 说明不是同一个包下面的, 那就可能是 import xx.xx.* 里面带着的
                        for (String packageName : importInfoSet) {
                            if (packageName.endsWith("*")) {
                                // 说明是通配符导入, 需要去掉 *
                                packageName = packageName.substring(0, packageName.length() - 1);
                                // 看看这个包下面有没有这个类
                                String entityClass = packageName +  mainEntityClassFromLink;
                                if (Objects.nonNull(PsiJavaFileUtil.getPsiClass(project, entityClass))) {
                                    return entityClass;
                                }
                            }
                        }

                    }
                    return samePackageEntityClass;
                });


        } else {
            mainEntityClass = mainEntityClassFromLink;
        }

        Collection<PsiClass> entityClass = PsiJavaFileUtil.getAnnotationPsiClass(project,
            "com.easy.query.core.annotation.Table");
        Map<String, PsiClass> entityWithClass = new HashMap<>();

        for (PsiClass entityPsiClass : entityClass) {
            if (StrUtil.equals(entityPsiClass.getQualifiedName(), mainEntityClass)) {
                entityWithClass.put(entityPsiClass.getQualifiedName(), entityPsiClass);
            }
        }

        if (entityWithClass.isEmpty()) {
            Messages.showMessageDialog(project, "DTO 类中指定的实体类不存在", "错误", Messages.getErrorIcon());
            return;
        }

        Module[] modules = MyModuleUtil.getModules(project);
        Module module = Arrays.stream(modules).filter(o -> {
            String modulePath = MyModuleUtil.getModulePath(o, JavaModuleSourceRootTypes.SOURCES);
            return StringUtils.isNotBlank(modulePath) && path.startsWith(modulePath);
        }).findFirst().orElse(null);
        if (module == null) {
            Messages.showErrorDialog(project, "无法找到对应模块", "错误提示");
            return;
        }

        String comPath = StrUtil.subAfter(path, "src/main/java/", true);

        File comFile = new File(comPath);
        String packageName = comFile.getParent().replace("\\", ".").replaceAll("/", ".");
        String dtoClassName = FileUtil.mainName(comFile);


        StructDTOEntityContext dtoStructContext = new StructDTOEntityContext(project, path, packageName, module, entityWithClass);
        dtoStructContext.setDtoClassName(dtoClassName); // 暂存现在的类名, 方便后面回填
        dtoStructContext.setDtoPsiClass(dtoPsiClass); // 暂存DTO PsiClass 方便后面从 psiClass 中获取方法信息

        EntitySelectDialog entitySelectDialog = new EntitySelectDialog(dtoStructContext);
        SwingUtilities.invokeLater(() -> {
//            entitySelectDialog.setVisible(true);
            // 跳过选择实体窗口, 直接进入字段选择
            entitySelectDialog.ok0(mainEntityClass);
            entitySelectDialog.dispose();
        });


    }
}
