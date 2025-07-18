package com.easy.query.plugin.core.inspection;


import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.config.ProjectSettings;
import com.easy.query.plugin.core.entity.InspectionResult;
import com.easy.query.plugin.core.util.*;
import com.google.common.collect.Lists;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DTO 字段检测<br/>
 * 1. DTO 字段上的 Column 注解应该和 实体上的 Column 注解的属性保持一致
 * 2. DTO 上的字段名应和实体类中的字段名保持一致
 * 3. DTO 上的字段类型应和实体类中的字段类型保持一致
 *
 * @author link2fun
 */

public class EasyQueryFieldMissMatchInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final String INSPECTION_PREFIX = "[EQ插件检查-DTO字段] ";

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getDisplayName() {
        return "EasyQuery DTO 字段检测";
    }

    @Override
    public boolean runForWholeFile() {
        return true;
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {

            @Override
            public void visitAnnotationParameterList(@NotNull PsiAnnotationParameterList list) {
                super.visitAnnotationParameterList(list);
            }

            @Override
            public void visitClass(@NotNull PsiClass currentClass) {
                // 这里只处理DTO类

                if (currentClass.getAnnotation("com.easy.query.core.annotation.Table") != null) {
                    // 如果是实体类, 则不处理
                    return;
                }

                PsiClass linkClass = PsiJavaClassUtil.getLinkPsiClass(currentClass);

                if (Objects.isNull(linkClass)) {
                    // 如果最终还是没找到的话, 那就直接跳过
                    return;
                }

                // 有 linkClass, 看看 linkClass 上是否有 Table 注解, 如果没有, 则不处理
                if (linkClass.getAnnotation("com.easy.query.core.annotation.Table") == null) {
                    return;
                }

                // 说明当前是链接实体的DTO类

                // 从当前字段中提取字段

                PsiField[] dtoFields = currentClass.getFields();
                PsiField[] entityFields = linkClass.getAllFields();

                // entityFields 转 map
                Map<String, PsiField> entityFieldMap = Arrays.stream(entityFields).collect(Collectors.toMap(PsiField::getName, Function.identity(), (a, b) -> a));


                Project project = currentClass.getProject();

                for (PsiField dtoField : dtoFields) {
                    boolean ignoreField = PsiJavaFieldUtil.ignoreField(dtoField);
                    if (ignoreField) {
                        continue;
                    }
                    //region 字段名称不匹配
                    if (!entityFieldMap.containsKey(dtoField.getName())) {

                        // 判断是否应该保留字段, 忽略警告
                        if (PsiJavaFieldUtil.keepField(dtoField, false)) {
                            continue;
                        }

                        // 尝试生成 quickFix

                        // 1. 抑制警告， 在字段上添加 @SuppressWarnings("EasyQueryFieldMissMatch")
                        @NotNull LocalQuickFix quickFixMethod1 = createQuickFixForSuppressWarningField("EasyQueryFieldMissMatch");

                        // 2. 注释字段， 注释当前 field
                        @NotNull LocalQuickFix quickFixMethod2 = createQuickFixForCommentField();

                        List<LocalQuickFix> localQuickFixes = Lists.newArrayList();
                        localQuickFixes.add(quickFixMethod1);
                        localQuickFixes.add(quickFixMethod2);

                        // 3. 可能是其他相近的字段，目前只提示忽略大小写的情况
                        for (PsiField entityField : entityFields) {
                            if (StrUtil.similar(dtoField.getName().toLowerCase(), entityField.getName().toLowerCase()) > 0.8) {
                                // 忽略大小写相同， 可能是这个字段
                                @NotNull LocalQuickFix quickFixMethod3 = new LocalQuickFix() {
                                    @Override
                                    public @IntentionFamilyName @NotNull String getFamilyName() {
                                        return "推测可能是 " + entityField.getName() + " 字段, 进行更新";
                                    }

                                    @Override
                                    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
                                        PsiField dtoField = PsiJavaFieldUtil.copyField(entityField);
                                        // 拷贝过来的字段， 类型需要保持和之前DTO的一致， 不然转换的DTO转换的类型会不匹配
                                        dtoField.getTypeElement().replace(((PsiField) problemDescriptor.getPsiElement()).getTypeElement());
                                        problemDescriptor.getPsiElement().replace(dtoField);
                                    }
                                };
                                localQuickFixes.add(quickFixMethod3);
                            }
                        }


                        // 这个字段不在实体类中, 需要警告

                        holder.registerProblem(dtoField, INSPECTION_PREFIX + "当前字段在实体类 " + linkClass.getQualifiedName() + " 中不存在", ProblemHighlightType.WARNING, localQuickFixes.toArray(new LocalQuickFix[0]));
                        continue;
                    }
                    //endregion


                    // 现在是有这个字段, 需要比对类型
                    PsiField entityField = entityFieldMap.get(dtoField.getName());

                    //region 字段名称匹配, 比较类型
                    if (dtoField.getType() instanceof PsiClassReferenceType && entityField.getType() instanceof PsiClassReferenceType) {
                        PsiJavaCodeReferenceElement dtoFieldTypeRef = ((PsiClassReferenceType) dtoField.getType()).getReference();
                        String dtoTypeRefName = dtoFieldTypeRef.getQualifiedName();
                        PsiJavaCodeReferenceElement entityFieldTypeRef = ((PsiClassReferenceType) entityField.getType()).getReference();
                        String entityTypeRefName = entityFieldTypeRef.getQualifiedName();
                        if (!StrUtil.equals(dtoTypeRefName, entityTypeRefName)) {
                            if (StrUtil.startWithAny(dtoTypeRefName, "java.")) { // 常见包下面的视作基础类型
                                // 类型不一致
                                holder.registerProblem(dtoField, INSPECTION_PREFIX + "当前字段类型和实体类中不一致,应为 " + entityTypeRefName + " 或其生成的DTO需添加{@link"+entityTypeRefName+"}", ProblemHighlightType.ERROR);
                                continue;

                            } else {
                                PsiClass psiClass = PsiJavaFileUtil.getPsiClass(project, dtoTypeRefName);
                                // 复杂类型的, 需要进一步比对
                                PsiClass linkPsiClass = PsiJavaClassUtil.getLinkPsiClass(psiClass);
                                if (Objects.isNull(linkPsiClass)) {
                                    // 没有找到对应的类, 无法比对， 视为不一致
                                    holder.registerProblem(dtoField, INSPECTION_PREFIX + "当前字段类型和实体类中不一致,应为 " + entityTypeRefName + " 或其生成的DTO需添加{@link"+entityTypeRefName+"}", ProblemHighlightType.ERROR);
                                    continue;
                                }
                                String linkPsiClassName = linkPsiClass.getQualifiedName();
                                if (!StrUtil.equals(linkPsiClassName, entityTypeRefName)) {
                                    // 类型不一致
                                    holder.registerProblem(dtoField, INSPECTION_PREFIX + "当前字段类型和实体类中不一致,应为 " + entityTypeRefName + " 或其生成的DTO需添加{@link"+entityTypeRefName+"}", ProblemHighlightType.ERROR);
                                    continue;
                                }
                            }
                        }

                        // 外围的一致，尝试匹配泛型
                        if (dtoFieldTypeRef.getTypeParameterCount() != entityFieldTypeRef.getTypeParameterCount()) {
                            // 泛型的个数不一致,肯定不对
                            holder.registerProblem(dtoField, INSPECTION_PREFIX + "当前字段泛型数量和实体类中不一致,应为 " + entityFieldTypeRef.getText(), ProblemHighlightType.ERROR);
                            continue;
                        }
                        // 现在进行内部泛型匹配
                        PsiType[] dtoFieldTypeParams = dtoFieldTypeRef.getTypeParameters();
                        for (int paraIdx = 0; paraIdx < dtoFieldTypeParams.length; paraIdx++) {
                            PsiType dtoFieldTypeParam = dtoFieldTypeParams[paraIdx];
                            PsiType entityFieldTypeParam = entityFieldTypeRef.getTypeParameters()[paraIdx];
                            if (dtoFieldTypeParam.equals(entityFieldTypeParam)) {
                                // 一致
                                continue;
                            }
                            // 不一致的情况
                            if (dtoFieldTypeParam instanceof PsiClassReferenceType && entityFieldTypeParam instanceof PsiClassReferenceType) {
                                // 都是类引用
                                PsiJavaCodeReferenceElement dtoFieldTypeParamRef = ((PsiClassReferenceType) dtoFieldTypeParam).getReference();
                                // 不相同的情况下， 尝试获取DTO泛型上面的 Link
                                String paramRefTypeName = ((PsiJavaCodeReferenceElement) dtoFieldTypeParamRef).getQualifiedName();
                                PsiClass linkPsiClass = PsiJavaClassUtil.getLinkPsiClass(PsiJavaFileUtil.getPsiClass(project, ((PsiJavaCodeReferenceElement) dtoFieldTypeParamRef).getQualifiedName()));
                                PsiJavaCodeReferenceElement entityFieldTypeParamRef = ((PsiClassReferenceType) entityFieldTypeParam).getReference();
                                if (Objects.isNull(linkPsiClass)) {
                                    // 没有找到对应的类, 无法比对， 视为不一致
                                    holder.registerProblem(dtoField, INSPECTION_PREFIX + "当前字段泛型和实体类中不一致,应为 " + entityFieldTypeParamRef.getQualifiedName() + " 或其生成的DTO需添加{@link"+entityTypeRefName+"}", ProblemHighlightType.ERROR);
                                    continue;
                                }
                                String linkPsiClassName = linkPsiClass.getQualifiedName();

                                if (!StrUtil.equals(linkPsiClassName, entityFieldTypeParamRef.getQualifiedName())) {
                                    // 类型不一致
                                    holder.registerProblem(dtoField, INSPECTION_PREFIX + "当前字段泛型和实体类中不一致,应为 " + entityFieldTypeParamRef.getQualifiedName(), ProblemHighlightType.ERROR);
                                }
                            } else {
                                // 其他情况, 暂不处理
                            }
                        }


                    }
                    //endregion


                    InspectionResult annoColumnInspectionResult = EasyQueryElementUtil.inspectionColumnAnnotation(project, dtoField, entityField);
                    if (annoColumnInspectionResult.hasProblem()) {
                        for (InspectionResult.Problem problem : annoColumnInspectionResult.getProblemList()) {
                            // 补充quickFix
                            ArrayList<LocalQuickFix> quickFixes = Lists.newArrayList(problem.getFixes());
                            quickFixes.add(createQuickFixForSuppressWarningField("EasyQueryFieldMissMatch"));
                            quickFixes.add(createQuickFixForCommentField());

                            // 添加前缀到问题描述
                            String description = INSPECTION_PREFIX + problem.getDescriptionTemplate();
                            holder.registerProblem(problem.getPsiElement(), description, problem.getHighlightType(), quickFixes.toArray(new LocalQuickFix[0]));
                        }
                    }

                    InspectionResult annoNavigationInspectionResult = EasyQueryElementUtil.inspectionNavigateAnnotation(holder.getProject(), dtoField, entityField);
                    if (annoNavigationInspectionResult.hasProblem()) {
                        for (InspectionResult.Problem problem : annoNavigationInspectionResult.getProblemList()) {
                            // 补充quickFix
                            ArrayList<LocalQuickFix> quickFixes = Lists.newArrayList(problem.getFixes());
                            quickFixes.add(createQuickFixForSuppressWarningField("EasyQueryFieldMissMatch"));
                            quickFixes.add(createQuickFixForCommentField());

                            // 添加前缀到问题描述
                            String description = INSPECTION_PREFIX + problem.getDescriptionTemplate();
                            holder.registerProblem(problem.getPsiElement(), description, problem.getHighlightType(), quickFixes.toArray(new LocalQuickFix[0]));
                        }
                    }

                }

            }
        };
    }



    /** 创建字段抑制QuickFix */
    public static LocalQuickFix createQuickFixForSuppressWarningField(String warningName) {
        return new LocalQuickFix() {
            @Override
            public @IntentionFamilyName @NotNull String getFamilyName() {
                return "★抑制警告， 在字段上添加 @SuppressWarnings(\"" + warningName + "\")";
            }

            @Override
            public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                PsiAnnotation annotation = elementFactory.createAnnotationFromText("@SuppressWarnings(\"" + warningName + "\")", problemDescriptor.getPsiElement());
                PsiField psiField = problemDescriptor.getPsiElement() instanceof PsiField ? (PsiField) problemDescriptor.getPsiElement() : PsiTreeUtil.getParentOfType(problemDescriptor.getPsiElement(), PsiField.class);
                problemDescriptor.getPsiElement().addAfter(annotation, psiField.getDocComment());
            }
        };
    }

    /** 创建字段注释QuickFix */
    public static LocalQuickFix createQuickFixForCommentField() {
        return new LocalQuickFix() {
            @Override
            public @IntentionFamilyName @NotNull String getFamilyName() {
                return "★注释字段， 注释当前 field";
            }

            @Override
            public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {
                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                PsiField psiField = problemDescriptor.getPsiElement() instanceof PsiField ? (PsiField) problemDescriptor.getPsiElement() : PsiTreeUtil.getParentOfType(problemDescriptor.getPsiElement(), PsiField.class);
                List<String> lines = StrUtil.split(psiField.getText(), "\n");
                // 生成注释
                lines.stream().map(line -> "// " + line).map(comment -> elementFactory.createCommentFromText(comment, null)).collect(Collectors.toList())
                        .forEach(comment -> psiField.addBefore(comment, psiField));
                psiField.delete();
            }
        };
    }
}
