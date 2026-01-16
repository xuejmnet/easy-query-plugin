package com.easy.query.plugin.core.util;


import cn.hutool.core.collection.CollectionUtil;
import com.easy.query.plugin.config.EasyQueryProjectSettingKey;
import com.easy.query.plugin.core.entity.AnnoAttrCompareResult;
import com.easy.query.plugin.core.entity.InspectionResult;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.LocalQuickFixOnPsiElement;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * EasyQuery 相关元素工具类
 *
 * @author link2fun
 */
public class EasyQueryElementUtil {

    /**
     * 检查DTO上的 @Column 注解是否需要更新
     *
     * @param project     项目
     * @param dtoField    DTO上的字段
     * @param entityField 实体上的字段
     * @return InspectionResult 检查结果
     */
    public static InspectionResult inspectionColumnAnnotation(Project project, PsiField dtoField, PsiField entityField) {

        if (Objects.isNull(dtoField) || Objects.isNull(entityField)) {
            // 任意一个字段为空, 应该是没法检查的, 直接当做没有问题
            return InspectionResult.noProblem();
        }
        // 项目设置, 是否保留DTO上的@Column注解 value 值
        Boolean featureKeepDtoColumnAnnotation = EasyQueryConfigUtil.getProjectSettingBool(project, EasyQueryProjectSettingKey.DTO_KEEP_ANNO_COLUMN, true);

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

        PsiAnnotation dtoAnnoColumnNew =
            PsiJavaAnnotationUtil.createAnnotation(
                project,
                dtoField,
                "Column",
                newAttrMap
            );
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
        dtoRemoveKeys.add("sqlExpression"); // sql表达式 在实体上就能工作， 没必要拷贝到DTO上
        dtoRemoveKeys.add("sqlConversion"); // sql表达式 在实体上就能工作， 没必要拷贝到DTO上

        List<String> ignoreKeys = Lists.newArrayList();
        ignoreKeys.add("nullable"); // sql表达式 在实体上就能工作， 没必要拷贝到DTO上
        ignoreKeys.add("comment"); // sql表达式 在实体上就能工作， 没必要拷贝到DTO上
        ignoreKeys.add("dbDefault"); // sql表达式 在实体上就能工作， 没必要拷贝到DTO上
        ignoreKeys.add("dbType"); // sql表达式 在实体上就能工作， 没必要拷贝到DTO上
        ignoreKeys.add("renameFrom"); // sql表达式 在实体上就能工作， 没必要拷贝到DTO上
        ignoreKeys.add("exist"); // sql表达式 在实体上就能工作， 没必要拷贝到DTO上
        ignoreKeys.add("autoSelect"); // sql表达式 在实体上就能工作， 没必要拷贝到DTO上

        return AnnoAttrCompareResult.newCompare(entityAnnoColumnAttrMap, dtoAnnoColumnAttrMap)
                .withEntityOnlyKeysPermit(entityOnlyKeysPermit)
                .withDtoOnlyKeysPermit(dtoOnlyKeysPermit)
                .withDtoRemoveKeys(dtoRemoveKeys)
                .withIgnoredKeys(ignoreKeys)
                .compare();
    }


    /**
     * 字段上是否有 com.easy.query.core.annotation.Navigate 注解
     *
     * @param field PsiField
     */
    public static boolean hasNavigateAnnotation(PsiField field) {
        if (field == null) {
            return false;
        }
        return field.getAnnotation("com.easy.query.core.annotation.Navigate") != null;
    }


    /**
     * 检查DTO上的 @Navigate 注解<br/>
     * 1. Navigate 注解的 type  必须保持一致
     *
     * @param dtoField    DTO上的字段
     * @param entityField 实体上的字段
     * @return InspectionResult 检查结果
     */
    public static InspectionResult inspectionNavigateAnnotation(Project project, PsiField dtoField, PsiField entityField) {

        // 1. 如果


        PsiAnnotation dtoNavigateAnno = Optional.ofNullable(dtoField).map(field -> field.getAnnotation("com.easy.query.core.annotation.Navigate")).orElse(null);

        PsiAnnotation entityNavigateAnno = Optional.ofNullable(entityField).map(field -> field.getAnnotation("com.easy.query.core.annotation.Navigate")).orElse(null);

        if (Objects.isNull(dtoNavigateAnno) && Objects.isNull(entityNavigateAnno)) {
            // 两个字段都没有 Navigate 注解, 没有问题
            return InspectionResult.noProblem();
        }

        // 1. 如果实体上有 Navigate 注解, 那么 DTO 上必须有 Navigate 注解
        if (Objects.nonNull(entityNavigateAnno)) {
            // 实体上有这个注解, 那么 DTO 上必须有这个注解
            //1.1. 如果 DTO 上没有这个字段， 那就没问题了
            if (Objects.isNull(dtoField)) {
                return InspectionResult.noProblem();
            }
            // 1.2. 现在是DTO上有这个字段， 需要看看是否存在注解
            if (Objects.isNull(dtoNavigateAnno)) {
                SmartPsiElementPointer<PsiField> dtoFieldPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(dtoField);
                PsiAnnotation navigateAnno = PsiJavaAnnotationUtil.copyAnnotation(entityNavigateAnno, "value");
                // DTO 上没有这个注解, 需要添加
                SmartPsiElementPointer<PsiAnnotation> navigateAnnoPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(navigateAnno);
                LocalQuickFixOnPsiElement addNavigateAnnoToDTO = new LocalQuickFixOnPsiElement(dtoField) {
                    @Override
                    public @IntentionFamilyName @NotNull String getFamilyName() {
                        return "★★★添加缺失的@Navigate注解";
                    }

                    @Override
                    public void invoke(@NotNull Project project, @NotNull PsiFile file, @NotNull PsiElement startElement, @NotNull PsiElement endElement) {

                        if (dtoFieldPointer.getElement() == null || navigateAnnoPointer.getElement() == null) {
                            return;
                        }
                        PsiField field = dtoFieldPointer.getElement();

                        field.getModifierList().addBefore(navigateAnnoPointer.getElement(), field.getModifierList().getFirstChild());
                    }

                    @Override
                    public @NotNull String getText() {
                        return getFamilyName();
                    }
                };
                return InspectionResult.newResult().addProblem(dtoField, "需要在DTO上添加 @Navigate 注解", ProblemHighlightType.ERROR,
                        Lists.newArrayList(addNavigateAnnoToDTO)
                );
            }

            // 3. 现在是DTO上也有这个注解， 需要看下类型是否一样
            Map<String, PsiNameValuePair> entityAnnoAttrMap = PsiJavaAnnotationUtil.attrToMap(entityNavigateAnno);
            Map<String, PsiNameValuePair> dtoAnnoAttrMap = PsiJavaAnnotationUtil.attrToMap(dtoNavigateAnno);

            PsiNameValuePair supportNonEntity = dtoAnnoAttrMap.get("supportNonEntity");
            if(supportNonEntity!=null){
                String literalValue = supportNonEntity.getLiteralValue();
                if("true".equals(literalValue)){
                    return InspectionResult.noProblem();
                }
            }

            // 合并两个Map的key并排除value
            Set<String> dtoRemoveKeys = Sets.newHashSet();
            dtoRemoveKeys.addAll(entityAnnoAttrMap.keySet());
            dtoRemoveKeys.addAll(dtoAnnoAttrMap.keySet());
            dtoRemoveKeys.remove("value");
            dtoRemoveKeys.remove("orderByProps"); // DTO 上可以保留 orderByProps
            dtoRemoveKeys.remove("partitionOrder"); // DTO 上可以保留 partitionOrder


            AnnoAttrCompareResult compareResult = AnnoAttrCompareResult.newCompare(entityAnnoAttrMap, dtoAnnoAttrMap)
                    .withDtoRemoveKeys(Lists.newArrayList(dtoRemoveKeys))
                    .withIgnoredKeys(Lists.newArrayList("orderByProps","limit","offset","partitionOrder")) // 忽略 orderByProps 检查
                    .compare();
            Map<String, PsiNameValuePair> fixedAttrMap = compareResult.getFixedAttrMap();
            List<String> problemMsgList = compareResult.getProblemMsgList();
            if (CollectionUtil.isNotEmpty(problemMsgList)) {
                SmartPsiElementPointer<PsiField> dtoFieldPointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(dtoField);
                // 属性不一致
                LocalQuickFix updateNavigateAnno = new LocalQuickFix() {
                    @Override
                    public @IntentionFamilyName @NotNull String getFamilyName() {
                        return "★★★更新DTO上的 @Navigate 注解";
                    }

                    @Override
                    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
                        PsiField dtoField = dtoFieldPointer.getElement();
                        if (Objects.isNull(dtoField)) {
                            return;
                        }
                        PsiAnnotation newNavigateAnno = PsiJavaAnnotationUtil.createAnnotation(project, dtoField, "Navigate", fixedAttrMap);
                        Objects.requireNonNull(dtoField.getAnnotation("com.easy.query.core.annotation.Navigate")).replace(newNavigateAnno);
                    }
                };
                return InspectionResult.newResult().addProblem(dtoNavigateAnno, "@Navigate 注解需要更新: " + StrUtil.join("\n", problemMsgList), ProblemHighlightType.ERROR,
                        Lists.newArrayList(updateNavigateAnno)
                );
            }


        }


        return InspectionResult.noProblem();

    }


    /**
     * 判断实体类是否继承自 AbstractProxyEntity
     * @param psiClass 实体类
     * @return true: 继承了 AbstractProxyEntity, false: 没有继承
     */
    public static boolean isExtendAbstractProxyEntity(PsiClass psiClass) {
        if (psiClass == null) {
            return false;
        }
        PsiClassType[] superTypes = psiClass.getSuperTypes();
        for (PsiClassType superType : superTypes) {
            PsiClass resolvedClass = superType.resolve();
            if (resolvedClass != null && "com.easy.query.core.proxy.AbstractProxyEntity".equals(resolvedClass.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查看方法调用中是否包含 com.easy.query.core.proxy.AbstractProxyEntity#expression 的调用
     * @param methodCallExpression 方法调用表达式
     * @return true: 包含, false: 不包含
     */
    public static boolean hasAbstractExpressionMethodCall(PsiMethodCallExpression methodCallExpression) {
        if (methodCallExpression == null) {
            return false;
        }
        return PsiTreeUtil.findChildrenOfType(methodCallExpression, PsiMethodCallExpression.class).stream()
                .anyMatch(exp -> {
                    PsiMethod expMethod = exp.resolveMethod();
                    if (expMethod == null) {
                        return false;
                    }

                    PsiClass expMethodClass = expMethod.getContainingClass();
                    if (expMethodClass == null) {
                        return false;
                    }

                    String expMethodClassName = expMethodClass.getQualifiedName();
                    String expMethodName = expMethod.getName();

                    return "com.easy.query.core.proxy.AbstractProxyEntity".equals(expMethodClassName) && "expression".equals(expMethodName);
                });
    }


    /**
     * 拿到直接树形直接关联的 T 类型元素
     * 也就是 从顶级一直往下找 找到  T 类型元素 就放到结果中， 不再寻找  T 类型元素 的子节点
     * @param psiElement
     * @param expectClass
     * @reutrn List
     */
    public static <T> List<T> getDirectChildOfType(PsiElement psiElement, Class<T> expectClass) {
        List<T> result = Lists.newArrayList();
        if (psiElement == null) {
            return result;
        }
        
        // 使用队列进行广度优先搜索
        Queue<PsiElement> queue = new LinkedList<>();
        queue.offer(psiElement);
        
        while (!queue.isEmpty()) {
            PsiElement current = queue.poll();
            PsiElement[] children = current.getChildren();
            
            for (PsiElement child : children) {
                if (expectClass.isInstance(child)) {
                    result.add((T) child);
                } else {
                    queue.offer(child);
                }
            }
        }
        
        return result;
        }

}
