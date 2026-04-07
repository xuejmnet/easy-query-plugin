package com.easy.query.plugin.core.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import com.easy.query.plugin.config.EasyQueryProjectSettingKey;
import com.easy.query.plugin.core.config.AppSettings;
import com.easy.query.plugin.core.entity.AnnoAttrCompareResult;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * PsiJavaField 工具类
 *
 * @author link2fun
 */
public class PsiJavaFieldUtil {

    /**
     * 从实体复制字段, 并根据dtoSchema 进行精简
     *
     * @param entityField 实体字段
     * @param dtoSchema   DTO schema
     * @return
     */
    public static PsiField copyAndPureFieldBySchema(PsiField entityField, String dtoSchema) {
        PsiField dtoField = copyField(entityField);
        pureFieldBySchema(dtoField, dtoSchema);
        return dtoField;
    }


    /**
     * copy field from entity
     *
     * @param entityField entity field
     * @return dtoField
     */
    public static PsiField copyField(PsiField entityField) {
        Project project = entityField.getProject();
        // 项目设置, 是否保留DTO上的@Column注解 value 值
        Boolean featureKeepDtoColumnAnnotation = EasyQueryConfigUtil.getProjectSettingBool(project, EasyQueryProjectSettingKey.DTO_KEEP_ANNO_COLUMN, true);

        // 使用 PsiElementFactory 创建新字段，避免操作编译后的元素
        PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);
        
        // 构建字段文本（包含注释、注解、修饰符、类型和名称）
        StringBuilder fieldTextBuilder = new StringBuilder();
        
        // 添加文档注释（如果存在）
        PsiDocComment docComment = entityField.getDocComment();
        if (docComment != null) {
            fieldTextBuilder.append(docComment.getText()).append("\n");
        }
        
        // 处理注解
        PsiAnnotation[] annotations = entityField.getAnnotations();
        for (PsiAnnotation annotation : annotations) {
            String qualifiedName = annotation.getQualifiedName();
            if (qualifiedName == null) {
                continue;
            }
            
            if ("com.easy.query.core.annotation.Navigate".equals(qualifiedName)) {
                // 只保留 value 属性
                List<JvmAnnotationAttribute> attrList = annotation.getAttributes().stream()
                        .filter(attr -> cn.hutool.core.util.StrUtil.equalsAny(attr.getAttributeName(), "value"))
                        .collect(Collectors.toList());
                if (CollectionUtil.isEmpty(attrList)) {
                    fieldTextBuilder.append("@Navigate\n");
                } else {
                    String attrText = attrList.stream().map(attr -> ((PsiNameValuePair) attr).getText())
                            .collect(Collectors.joining(", "));
                    fieldTextBuilder.append("@Navigate(").append(attrText).append(")\n");
                }
            } else if ("com.easy.query.core.annotation.Column".equals(qualifiedName)) {
                // 处理 @Column 注解，移除主键相关信息
                AnnoAttrCompareResult attrCompareResult = EasyQueryElementUtil.compareColumnAnnoAttr(annotation, null, featureKeepDtoColumnAnnotation);
                Map<String, PsiNameValuePair> fixedAttrMap = attrCompareResult.getFixedAttrMap();
                
                if (MapUtil.isNotEmpty(fixedAttrMap)) {
                    String attrText = fixedAttrMap.values().stream().map(attr -> attr.getText())
                            .collect(Collectors.joining(", "));
                    fieldTextBuilder.append("@Column(").append(attrText).append(")\n");
                }
                // 如果 attrMap 为空，则不添加该注解
            } else {
                // 其他注解直接保留
                fieldTextBuilder.append(annotation.getText()).append("\n");
            }
        }
        
        // 添加修饰符、类型和字段名
        String modifiers = entityField.getModifierList() != null ? entityField.getModifierList().getText() : "private";
        String typeText = entityField.getType().getCanonicalText();
        String fieldName = entityField.getName();
        
        fieldTextBuilder.append(modifiers).append(" ").append(typeText).append(" ").append(fieldName).append(";");
        
        // 使用 PsiElementFactory 创建新字段
        PsiField dtoField = elementFactory.createFieldFromText(fieldTextBuilder.toString(), entityField.getContainingClass());
        
        return dtoField;
    }

    public static Boolean keepField(PsiField field) {
        return keepField(field, true);
    }

    /**
     * 是否是需要被忽略的字段
     * @param dtoField
     * @return
     */
    public static boolean ignoreField(PsiField dtoField){

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
        // 如果字段上有 com.easy.query.core.annotation.ColumnIgnore 注解, 保留
        PsiAnnotation easyWhereCondition = dtoField.getAnnotation("com.easy.query.core.annotation.EasyWhereCondition");
        if (easyWhereCondition != null) {
            return true;
        }
        return false;
    }
    /**
     * 是否保留 DTO 上的字段 <br/>
     * 1. 如果是静态字段<br/>
     * 2. 如果有一些特殊的注解, NavigateFlat NavigateJoin ColumnIgnore
     * 3. 如果 SuppressWarnings EasyQueryFieldMissMatch 则认定为自定义的DTO字段, 修改DTO的时候保留<br?
     *
     * @param dtoField DTO的字段
     * @return 是否保留
     */
    public static Boolean keepField(PsiField dtoField, Boolean keepSuppressWarningsField) {

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

    /**
     * 通过不同类型的 schema 精简 DTO 字段
     *
     * @param dtoField  DTO字段， 在原字段上修改， 如果是从实体来的字段, 请先 copy()
     * @param dtoSchema DTO schema normal/request/response/excel
     */
    public static void pureFieldBySchema(PsiField dtoField, String dtoSchema) {

        AppSettings.State appSettings = AppSettings.getInstance().getState();
        if (appSettings == null) {
            return;
        }
        PsiAnnotation[] annotations = dtoField.getAnnotations();
        List<String> removeAnnoList = appSettings.getRemoveAnnoList(dtoSchema);
        if (CollectionUtil.isEmpty(removeAnnoList)) {
            return;
        }
        for (PsiAnnotation annotation : annotations) {
            String annotationQualifiedName = annotation.getQualifiedName();
            if (removeAnnoList.contains(annotationQualifiedName)) {
                annotation.delete();
            }

            // 针对一些注解需要进行精简
            // @Nav
        }
    }
}
