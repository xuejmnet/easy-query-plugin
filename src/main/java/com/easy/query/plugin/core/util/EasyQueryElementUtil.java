package com.easy.query.plugin.core.util;


import cn.hutool.core.collection.CollectionUtil;
import com.easy.query.plugin.core.config.ProjectSettings;
import com.easy.query.plugin.core.entity.AnnoAttrCompareResult;
import com.easy.query.plugin.core.entity.InspectionResult;
import com.google.common.collect.Lists;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * EasyQuery 相关元素工具类
 *
 * @author link2fun
 */
public class EasyQueryElementUtil {


    /**
     * 检查DTO上的 @Column 注解是否需要更新
     *
     * @param projectSettings 项目级别的设置
     * @param dtoField        DTO上的字段
     * @param entityField     实体上的字段
     * @return InspectionResult 检查结果
     */
    public static InspectionResult inspectionColumnAnnotation(ProjectSettings projectSettings, PsiField dtoField, PsiField entityField) {

        if (Objects.isNull(dtoField) || Objects.isNull(entityField)) {
            // 任意一个字段为空, 应该是没法检查的, 直接当做没有问题
            return InspectionResult.noProblem();
        }

        // 项目设置, 是否保留DTO上的@Column注解 value 值
        Boolean featureKeepDtoColumnAnnotation = Optional.ofNullable(projectSettings).map(ProjectSettings::getState).map(ProjectSettings.State::getFeatureKeepDtoColumnAnnotation).orElse(true);

        // 获取DTO上的 @Column 注解
        PsiAnnotation dtoAnnoColumn = dtoField.getAnnotation("com.easy.query.core.annotation.Column");
        PsiAnnotation entityAnnoColumn = entityField.getAnnotation("com.easy.query.core.annotation.Column");


        if (entityAnnoColumn == null && dtoAnnoColumn == null) {
            // 实体上和 DTO上都没有 @Column 注解, 应该也是无需判断的
            return InspectionResult.noProblem();
        }
//        String entityColumnName = PsiUtil.getPsiAnnotationValue(entityAnnoColumn, "value", "");
//        String dtoColumnName = PsiUtil.getPsiAnnotationValue(dtoAnnoColumn, "value", "");
//        if (StrUtil.isBlank(entityColumnName) && StrUtil.isBlank(dtoColumnName)) {
//            // 实体上和 DTO上的 @Column 注解 的 value属性, 应该也是无需判断的
//            return InspectionResult.noProblem();
//        }

        AnnoAttrCompareResult attrCompareResult = compareColumnAnnoAttr(entityAnnoColumn, dtoAnnoColumn, featureKeepDtoColumnAnnotation);

        List<String> errInfoList = attrCompareResult.getProblemMsgList();
        if (CollectionUtil.isEmpty(errInfoList)) {
            // 属性一致, 不需要更新
            return InspectionResult.noProblem();
        }


        if (entityAnnoColumn == null) {
            // 实体上没有有, 但是DTO上有, 那么DTO上的应该移除掉

            LocalQuickFix removeDtoAnnoColumn = new LocalQuickFix() {
                @Override
                public @IntentionFamilyName @NotNull String getFamilyName() {
                    return "★★★移除DTO上的@Column注解";
                }

                @Override
                public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
                    problemDescriptor.getPsiElement().delete();
                }
            };


            return InspectionResult.newResult().addProblem(dtoAnnoColumn, "实体上没有@Column注解,DTO上的@Column应移除", ProblemHighlightType.ERROR,
                Lists.newArrayList(removeDtoAnnoColumn)
            );
        }


        Map<String, PsiNameValuePair> newAttrMap = attrCompareResult.getFixedAttrMap();


        // 属性不一致

// 实体上有@Column 注解, 那么应该精简一下, 看看是否有必要更新
        PsiAnnotation dtoAnnoColumnNew = (PsiAnnotation) entityAnnoColumn.copy();
        PsiAnnotationParameterList parameterList = dtoAnnoColumnNew.getParameterList();
        PsiNameValuePair[] attributes = parameterList.getAttributes();
        int length = attributes.length;
        for (int i = length - 1; i >= 0; i--) {
            PsiNameValuePair attribute = attributes[i];
            if (!newAttrMap.containsKey(attribute.getAttributeName())) {
                attribute.delete();
            }
        }


        // 现在需要进行更新了

        SmartPsiElementPointer<PsiElement> newAnnoColumnPointer = SmartPointerManager.createPointer(dtoAnnoColumnNew);


        if (Objects.isNull(dtoAnnoColumn)) {
            // 这里分两种情况, 第一种, 是 DTO 上的注解不存在, 这时候是需要新增注解

            LocalQuickFix addColumnAnnoToDTO = new LocalQuickFix() {
                @Override
                public @IntentionFamilyName @NotNull String getFamilyName() {
                    return "★★★添加缺失的@Column注解";
                }

                @Override
                public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {


                    if (newAnnoColumnPointer.getElement() != null) {
                        // 缺少注解的时候异常信息绑定的是字段上
                        PsiField dtoFieldToFix = (PsiField) problemDescriptor.getPsiElement();
                        dtoFieldToFix.getModifierList().addBefore(newAnnoColumnPointer.getElement(), dtoFieldToFix.getModifierList().getFirstChild());

                    }


                }
            };
            return InspectionResult.newResult()
                    .addProblem(dtoField, "需要在DTO上添加 @Column 注解", ProblemHighlightType.ERROR,
                            Lists.newArrayList(addColumnAnnoToDTO)
                    );
        }

        // 现在是直接需要更新的


        // 另一种是, DTO上的注解存在, 这时候需要更新

        LocalQuickFix updateDtoAnnoColumn = new LocalQuickFix() {
            @Override
            public @IntentionFamilyName @NotNull String getFamilyName() {
                return "★★★更新DTO上的 @Column 注解";
            }

            @Override
            public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
                if (newAnnoColumnPointer.getElement() != null) {
                    if (CollectionUtil.isEmpty(((PsiAnnotation) newAnnoColumnPointer.getElement()).getAttributes())) {
                        // 如果没有属性, 那么直接删除
                        problemDescriptor.getPsiElement().delete();
                    } else {
                        problemDescriptor.getPsiElement().replace(newAnnoColumnPointer.getElement());
                    }
                }
            }
        };


        return InspectionResult.newResult()
                .addProblem(dtoAnnoColumn, "@Column 注解需要更新: " + StrUtil.join("\n", errInfoList), ProblemHighlightType.ERROR,
                        Lists.newArrayList(updateDtoAnnoColumn)
                );
    }

    /**
     * 比较 @Column 注解的属性
     *
     * @param entityAnnoColumn               实体上的Column注解
     * @param dtoAnnoColumn                  DTO上的 @Column 注解
     * @param featureKeepDtoColumnAnnotation 项目配置 是否保留DTO上的@Column注解 value 值
     * @return 比较结果
     */
    public static AnnoAttrCompareResult compareColumnAnnoAttr(PsiAnnotation entityAnnoColumn, PsiAnnotation dtoAnnoColumn, Boolean featureKeepDtoColumnAnnotation) {
        Map<String, PsiNameValuePair> entityAnnoColumnAttrMap = PsiJavaAnnotationUtil.attrToMap(entityAnnoColumn);
        Map<String, PsiNameValuePair> dtoAnnoColumnAttrMap = PsiJavaAnnotationUtil.attrToMap(dtoAnnoColumn);

        // 允许实体独有的属性
        List<String> entityOnlyKeysPermit = Lists.newArrayList("primaryKey", "generatedKey", "primaryKeyGenerator");
        // 允许DTO独有的属性
        List<String> dtoOnlyKeysPermit = Lists.newArrayList("conversion");
        List<String> dtoRemoveKeys = Lists.newArrayList();
        if (!featureKeepDtoColumnAnnotation) {
            // 不保留 value
            entityOnlyKeysPermit.add("value");
            dtoRemoveKeys.add("value");
        }

        ;
        return AnnoAttrCompareResult.newCompare(entityAnnoColumnAttrMap, dtoAnnoColumnAttrMap)
                .withEntityOnlyKeysPermit(entityOnlyKeysPermit)
                .withDtoOnlyKeysPermit(dtoOnlyKeysPermit)
                .withDtoRemoveKeys(dtoRemoveKeys)
                .compare();
    }


}
