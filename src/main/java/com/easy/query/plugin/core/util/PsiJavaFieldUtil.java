package com.easy.query.plugin.core.util;

import cn.hutool.core.collection.CollectionUtil;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.psi.*;
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
            if (StrUtil.startWithAny(qualifiedName, "com.baomidou.mybatisplus.annotation", "javax.persistence","javax.validation", "org.hibernate.annotations")) {
                dtoField.getAnnotations()[i].delete();
            }
        }


        PsiAnnotation psiAnnoColumnIgnore = dtoField.getAnnotation("com.easy.query.core.annotation.ColumnIgnore");
        PsiAnnotation psiAnnoNavigateFlat = dtoField.getAnnotation("com.easy.query.core.annotation.NavigateFlat");



        return dtoField;
    }

    public static Boolean keepField(PsiField field) {
        return keepField(field, true);
    }

    /**
     * 是否保留 DTO 上的字段 <br/>
     * 1. 如果是静态字段<br/>
     * 2. 如果有一些特殊的注解, NavigateFlat NavigateJoin ColumnIgnore
     * 3. 如果 SuppressWarnings EasyQueryFieldMissMatch 则认定为自定义的DTO字段, 修改DTO的时候保留<br?
     * @param dtoField DTO的字段
     * @return 是否保留
     */
    public static Boolean keepField(PsiField dtoField, Boolean keepSuppressWarningsField) {

        // 看看是否是静态字段
        if (dtoField.hasModifierProperty(PsiModifier.STATIC)) {
            return true;
        }

        // 如果字段上有 com.easy.query.core.annotation.NavigateFlat 注解, 保留
        PsiAnnotation navigateFlat = dtoField.getAnnotation("com.easy.query.core.annotation.NavigateFlat");
        if (navigateFlat != null) {
            return true;
        }
        // 如果字段上有 com.easy.query.core.annotation.NavigateJoin 注解, 保留
        PsiAnnotation navigateJoin = dtoField.getAnnotation("com.easy.query.core.annotation.NavigateJoin");
        if (navigateJoin != null) {
            return true;
        }
        // 如果字段上有 com.easy.query.core.annotation.ColumnIgnore 注解, 保留
        PsiAnnotation columnIgnore = dtoField.getAnnotation("com.easy.query.core.annotation.ColumnIgnore");
        if (columnIgnore != null) {
            return true;
        }


        if (!keepSuppressWarningsField) {
            return false;
        }




        // 看看字段上是否有 java.lang.SuppressWarnings 注解
        PsiAnnotation suppressWarnings = dtoField.getAnnotation("java.lang.SuppressWarnings");
        // 如果有的话,获取一下 value
        if (suppressWarnings == null) {
            // 没有这个注解, 说明不是自定义字段
            return false;
        }
        JvmAnnotationAttribute attrValue = suppressWarnings.getAttributes().stream().filter(attr -> attr.getAttributeName().equals("value")).findFirst().orElse(null);
        if (!(attrValue instanceof PsiNameValuePairImpl)) {
            return false;
        }

        boolean keepField = false;
        if (((PsiNameValuePairImpl) attrValue).getDetachedValue() instanceof PsiArrayInitializerMemberValue) {
            // 如果是数组的话, 依次判断
            PsiArrayInitializerMemberValue attributeValue = (PsiArrayInitializerMemberValue) ((PsiNameValuePairImpl) attrValue).getDetachedValue();
            if (attributeValue == null) {
                return false;
            }
            PsiAnnotationMemberValue[] initializers = attributeValue.getInitializers();
            for (PsiAnnotationMemberValue initializer : initializers) {
                keepField = keepField || initializer.getText().equals("\"EasyQueryFieldMissMatch\"");
            }

        } else if (((PsiNameValuePair) attrValue).getLiteralValue() != null) {
            keepField = ((PsiNameValuePair) attrValue).getLiteralValue().equals("EasyQueryFieldMissMatch");
        }
        return keepField;
    }
}
