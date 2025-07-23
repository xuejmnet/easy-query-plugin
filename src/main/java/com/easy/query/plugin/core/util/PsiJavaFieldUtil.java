package com.easy.query.plugin.core.util;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import com.easy.query.plugin.config.EasyQueryProjectSettingKey;
import com.easy.query.plugin.core.config.AppSettings;
import com.easy.query.plugin.core.entity.AnnoAttrCompareResult;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
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

        // 先拷贝一份
        PsiField dtoField = (PsiField) entityField.copy();

        // 处理一下 @Navigate 注解
        PsiAnnotation psiAnnoNavigate = dtoField.getAnnotation("com.easy.query.core.annotation.Navigate");
        if (psiAnnoNavigate != null) {
            // 这个注解只保留 value 属性
            List<JvmAnnotationAttribute> attrList = psiAnnoNavigate.getAttributes().stream()
                    .filter(attr -> cn.hutool.core.util.StrUtil.equalsAny(attr.getAttributeName(), "value"))
                    .collect(Collectors.toList());
            String attrText = attrList.stream().map(attr -> ((PsiNameValuePair) attr).getText())
                    .collect(Collectors.joining(", "));
            // 再拼成 @Navigate 注解文本
            String replacement = "@Navigate(" + attrText + ")";
            PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);
            PsiAnnotation newAnno = elementFactory.createAnnotationFromText(replacement, dtoField);
            psiAnnoNavigate.replace(newAnno);

        }

        // 处理一下 @Column 注解
        PsiAnnotation psiAnnoColumn = dtoField.getAnnotation("com.easy.query.core.annotation.Column");
        if (psiAnnoColumn != null) {
            // 这个注解移除主键相关信息

            AnnoAttrCompareResult attrCompareResult = EasyQueryElementUtil.compareColumnAnnoAttr(psiAnnoColumn, null, featureKeepDtoColumnAnnotation);

            Map<String, PsiNameValuePair> fixedAttrMap = attrCompareResult.getFixedAttrMap();


            // 如果 attrList 为空， 则不添加这个注解了
            if (MapUtil.isEmpty(fixedAttrMap)) {
                psiAnnoColumn.delete();
            } else {
                // 如果只有 value , 则不需要保留, // FIXME @Column value 当前版本需要始终保留, 因为DTO 关联的是数据库字段, 不可删除, 等后续支持 关联属性再增加设置来匹配
//                if (attrList.size() == 1 && cn.hutool.core.util.StrUtil.equalsAny(attrList.get(0).getAttributeName(), "value")) {
//                    psiAnnoColumn.delete();
//                }else{
                String attrText = fixedAttrMap.values().stream().map(attr -> attr.getText())
                        .collect(Collectors.joining(", "));
                // 再拼成 @Navigate 注解文本
                String replacement = "@Column(" + attrText + ")";
                PsiElementFactory elementFactory = PsiElementFactory.getInstance(project);
                PsiAnnotation newAnno = elementFactory.createAnnotationFromText(replacement, dtoField);
                psiAnnoColumn.replace(newAnno);
//                }

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
