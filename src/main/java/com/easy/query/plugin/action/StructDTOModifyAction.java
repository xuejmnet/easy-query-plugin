package com.easy.query.plugin.action;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.struct.StructDTOEntityContext;
import com.easy.query.plugin.core.util.MyModuleUtil;
import com.easy.query.plugin.core.util.PsiJavaClassUtil;
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
import com.intellij.psi.impl.source.PsiJavaFileImpl;
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
        PsiClass mainEntityClass = PsiJavaClassUtil.getLinkPsiClass(dtoPsiClass);
        if (Objects.isNull(mainEntityClass)) {
            Messages.showMessageDialog(project, "无法从当前DTO类的文档中提取 @link 信息", "错误", Messages.getErrorIcon());
            return;
        }


        Collection<PsiClass> entityClass = PsiJavaFileUtil.getAnnotationPsiClass(project,
                "com.easy.query.core.annotation.Table");
        Map<String, PsiClass> entityWithClass = new HashMap<>();

        for (PsiClass entityPsiClass : entityClass) {
            if (StrUtil.equals(entityPsiClass.getQualifiedName(), mainEntityClass.getQualifiedName())) {
                entityWithClass.put(entityPsiClass.getQualifiedName(), entityPsiClass);
            }
        }

        if (entityWithClass.isEmpty()) {
            Messages.showMessageDialog(project, "DTO 类中指定的实体类不存在", "错误", Messages.getErrorIcon());
            return;
        }

        Module module = MyModuleUtil.getModuleForFile(project, psiElement.getContainingFile().getVirtualFile());
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
            entitySelectDialog.ok0(mainEntityClass.getQualifiedName());
            entitySelectDialog.dispose();
        });


    }
}
