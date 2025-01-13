package com.easy.query.plugin.action;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ArrayUtil;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MyBatisPlusToEasyQueryAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        if (!(psiFile instanceof PsiJavaFile)) {
            // Not a Java file
            Messages.showMessageDialog(project, "This is not a Java file", "Easy Query", Messages.getInformationIcon());
            return;
        }

        PsiClass[] psiClasses = ((PsiJavaFile) psiFile).getClasses();
        if (ArrayUtil.isEmpty(psiClasses)) {
            // No class in the file
            Messages.showMessageDialog(project, "No class in the file", "Easy Query", Messages.getInformationIcon());
        }

        // 不能有多个类
        if (CollectionUtil.size(psiClasses) > 1) {
            Messages.showMessageDialog(project, "There are more than one class in the file", "Easy Query", Messages.getInformationIcon());
            return;
        }


        PsiClass currentClass = ArrayUtil.firstNonNull(psiClasses);

        // 处理 MyBatisPlus 迁移
        PsiAnnotation myBatisPlusAnnoTableName = currentClass.getAnnotation("com.baomidou.mybatisplus.annotation.TableName");
        if (Objects.isNull(myBatisPlusAnnoTableName)) {
            Messages.showMessageDialog(project, "This class is not a MyBatisPlus entity", "Easy Query", Messages.getInformationIcon());
            return;
        }

        // 说明确实是一个 MyBatisPlus 标记的数据库实体

        WriteCommandAction.runWriteCommandAction(project, () -> {
            PsiModifierList modifierList = currentClass.getModifierList();
            PsiImportList importList = ((PsiJavaFile) psiFile).getImportList();
            // 1. 看看上面有没有 EasyQuery 的 Table注解, 没有的话, 加上
            if (currentClass.getAnnotation("com.easy.query.core.annotation.Table") == null) {

                // 缺少注解, 加上
                importList.add(JavaPsiFacade.getElementFactory(project).createImportStatement(Objects.requireNonNull(JavaPsiFacade.getInstance(project).findClass("com.easy.query.core.annotation.Table", GlobalSearchScope.allScope(currentClass.getProject())))));
                importList.add(JavaPsiFacade.getElementFactory(project).createImportStatement(Objects.requireNonNull(JavaPsiFacade.getInstance(project).findClass("com.easy.query.core.annotation.Column", GlobalSearchScope.allScope(currentClass.getProject())))));


                String tableName = ((PsiNameValuePair) myBatisPlusAnnoTableName.findAttribute("value")).getLiteralValue();
                modifierList.addAnnotation("Table(value=\"" + tableName + "\")");


            }

            // 2. 字段上加上 Column 注解
            PsiField[] fields = currentClass.getFields();

            for (PsiField field : fields) {

                PsiAnnotation annoColumn = field.getAnnotation("com.easy.query.core.annotation.Column");
                PsiAnnotation annoVersion = field.getAnnotation("com.easy.query.core.annotation.Version");
                PsiAnnotation annoLogicDelete = field.getAnnotation("com.easy.query.core.annotation.LogicDelete");

                PsiAnnotation mpAnnoTableId = field.getAnnotation("com.baomidou.mybatisplus.annotation.TableId");
                PsiAnnotation mpAnnoTableField = field.getAnnotation("com.baomidou.mybatisplus.annotation.TableField");
                PsiAnnotation mpAnnoTableLogic = field.getAnnotation("com.baomidou.mybatisplus.annotation.TableLogic");
                PsiAnnotation mpAnnoVersion = field.getAnnotation("com.baomidou.mybatisplus.annotation.Version");

                PsiModifierList fieldModifierList = field.getModifierList();

                if (Objects.isNull(annoColumn)) {
                    // 已经有注解的了, 不用加了
                    if (Objects.nonNull(mpAnnoTableId)) {
                        // 主键
                        JvmAnnotationAttribute valueAttr = mpAnnoTableId.findAttribute("value");
                        if (Objects.nonNull(valueAttr)) {
                            fieldModifierList.addAnnotation("Column(value=\"" + ((PsiNameValuePair) valueAttr).getLiteralValue() + "\", primaryKey=true)");
                        }
                    } else if (Objects.nonNull(mpAnnoTableField)) {
                        // 普通字段
                        JvmAnnotationAttribute valueAttr = mpAnnoTableField.findAttribute("value");
                        if (Objects.nonNull(valueAttr)) {
                            fieldModifierList.addAnnotation("Column(value=\"" + ((PsiNameValuePair) valueAttr).getLiteralValue() + "\")");
                        }
                    }
                }


                if (Objects.nonNull(mpAnnoTableLogic) && Objects.isNull(annoLogicDelete)) {
                    // 逻辑删除字段
                    importList.add(JavaPsiFacade.getElementFactory(project).createImportStatement(Objects.requireNonNull(JavaPsiFacade.getInstance(project).findClass("com.easy.query.core.annotation.LogicDelete", GlobalSearchScope.allScope(currentClass.getProject())))));
                    fieldModifierList.addAnnotation("LogicDelete");
                }

                if (Objects.nonNull(mpAnnoVersion) && Objects.isNull(annoVersion)) {
                    // 乐观锁字段, 需要区分字段类型, 先不自动识别类型
                    fieldModifierList.addAnnotation("com.easy.query.core.annotation.Version");
                }


            }


            // 3. 看看是否添加了 @EntityProxy 注解, 没有的话, 加上
            if (currentClass.getAnnotation("com.easy.query.core.annotation.EntityProxy") == null) {
                importList.add(JavaPsiFacade.getElementFactory(project).createImportStatement(Objects.requireNonNull(JavaPsiFacade.getInstance(project).findClass("com.easy.query.core.annotation.EntityProxy", GlobalSearchScope.allScope(currentClass.getProject())))));
                modifierList.addAnnotation("EntityProxy");

                // 加完了之后实现以下接口

            }


        });


    }
}
