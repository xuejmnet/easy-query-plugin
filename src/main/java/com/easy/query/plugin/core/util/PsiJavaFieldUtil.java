package com.easy.query.plugin.core.util;

import cn.hutool.core.collection.CollectionUtil;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;

import java.util.List;
import java.util.stream.Collectors;

/**
 * PsiJavaField 工具类
 * @author link2fun
 */
public class PsiJavaFieldUtil {


    /**
     * copy field from entity
     * @param entityField entity field
     * @return dtoField
     */
    public static PsiField copyField(PsiField entityField) {
        // 先拷贝一份
        PsiField dtoField = (PsiField) entityField.copy();

        // 处理一下 @Navigate 注解
        PsiAnnotation psiAnnoNavigate = dtoField.getAnnotation("com.easy.query.core.annotation.Navigate");
        if (psiAnnoNavigate != null) {
            // 这个注解只保留 value 属性
            List<JvmAnnotationAttribute> attrList = psiAnnoNavigate.getAttributes().stream()
                    .filter(attr -> cn.hutool.core.util.StrUtil.equalsAny(attr.getAttributeName(), "value"))
                    .collect(Collectors.toList());
            String attrText = attrList.stream().map(attr -> ((PsiNameValuePairImpl) attr).getText())
                    .collect(Collectors.joining(", "));
            // 再拼成 @Navigate 注解文本
            String replacement = "@Navigate(" + attrText + ")";
            PsiElementFactory elementFactory = PsiElementFactory.getInstance(entityField.getProject());
            PsiAnnotation newAnno = elementFactory.createAnnotationFromText(replacement, dtoField);
            psiAnnoNavigate.replace(newAnno);

        }

        // 处理一下 @Column 注解
        PsiAnnotation psiAnnoColumn = dtoField.getAnnotation("com.easy.query.core.annotation.Column");
        if (psiAnnoColumn != null) {
            // 这个注解移除主键相关信息

            List<JvmAnnotationAttribute> attrList = psiAnnoColumn.getAttributes().stream()
                    .filter(attr -> !cn.hutool.core.util.StrUtil.equalsAny(attr.getAttributeName(), "primaryKey","generatedKey","generatedSQLColumnGenerator","primaryKeyGenerator"))
                    .collect(Collectors.toList());

            // 如果 attrList 为空， 则不添加这个注解了
            if (CollectionUtil.isEmpty(attrList)){
                psiAnnoColumn.delete();
            }else{
                String attrText = attrList.stream().map(attr -> ((PsiNameValuePairImpl) attr).getText())
                        .collect(Collectors.joining(", "));
                // 再拼成 @Navigate 注解文本
                String replacement = "@Column(" + attrText + ")";
                PsiElementFactory elementFactory = PsiElementFactory.getInstance(entityField.getProject());
                PsiAnnotation newAnno = elementFactory.createAnnotationFromText(replacement, dtoField);
                psiAnnoColumn.replace(newAnno);
            }


        }

        // 移除 MyBatisPlus 等其他ORM的注解
        for (int i = dtoField.getAnnotations().length - 1; i >= 0; i--) {
            String qualifiedName = dtoField.getAnnotations()[i].getQualifiedName();
            if (StrUtil.startWithAny(qualifiedName, "com.baomidou.mybatisplus.annotation", "javax.persistence", "org.hibernate.annotations")) {
                dtoField.getAnnotations()[i].delete();
            }
        }


        PsiAnnotation psiAnnoColumnIgnore = dtoField.getAnnotation("com.easy.query.core.annotation.ColumnIgnore");
        PsiAnnotation psiAnnoNavigateFlat = dtoField.getAnnotation("com.easy.query.core.annotation.NavigateFlat");



        return dtoField;
    }

}
