package com.easy.query.plugin.core.inspection;


import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.util.PsiJavaClassUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import com.intellij.psi.javadoc.PsiDocComment;
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
            public void visitAnnotation(@NotNull PsiAnnotation annotation) {
                if (!"com.easy.query.core.annotation.Column".equals(annotation.getQualifiedName())) {
                    return;
                }

                if (annotation.getContext() == null) {
                    // 缺少上下文无法处理
                    return;
                }

                PsiClass currentClass;
                PsiElement parentClass = annotation.getParent();
                while (!(parentClass instanceof PsiClass)) {
                    parentClass = parentClass.getParent();
                }
                currentClass = (PsiClass) parentClass;

                // 从当前类中找到 link
                PsiDocComment docComment = currentClass.getDocComment();
                if (Objects.isNull(docComment)) {
                    // 当前类上没有文档注释, 没法校验,直接跳过
                    return;
                }

                // 看看当前类上是否有 Table 注解, 如果有的话, 说明自身就是实体, 不用校验
                PsiAnnotation entityTable = currentClass.getAnnotation("com.easy.query.core.annotation.Table");
                if (Objects.nonNull(entityTable)) {
                    return;
                }


                PsiClass linkClass = PsiJavaClassUtil.getLinkPsiClass(currentClass);
                if (Objects.isNull(linkClass)) {
                    // 如果最终还是没找到的话, 那就直接跳过
                    return;
                }

                // 找到了, 需要去对应类中找到字段
                // 从 annotation 中提取出字段

                PsiField currentField = (PsiField) annotation.getParent().getParent();

                // 从 linkClass 中找到对应字段
                for (PsiField field : linkClass.getAllFields()) {
                    if (!field.getName().equals(currentField.getName())) {
                        continue;
                    }

                    // 字段相同, 获取 @Column 注解
                    PsiAnnotation linkFieldColumn = field.getAnnotation("com.easy.query.core.annotation.Column");
                    if (Objects.isNull(linkFieldColumn)) {
                        // 没有 Column 注解, 说明不需要特殊处理
                        return;
                    }

                    // 有 Column 注解, 则两遍的注解需要保持一致
                    PsiAnnotationParameterList currentFieldParameterList = annotation.getParameterList();
                    PsiAnnotationParameterList linkFieldParameterList = linkFieldColumn.getParameterList();
                    // 两遍的注解内容转为 Map
                    Map<String, PsiNameValuePair> currentFieldParaMap = Arrays.stream(currentFieldParameterList.getAttributes()).collect(Collectors.toMap(PsiNameValuePair::getAttributeName, Function.identity()));
                    Map<String, PsiNameValuePair> linkFieldParaMap = Arrays.stream(linkFieldParameterList.getAttributes()).collect(Collectors.toMap(PsiNameValuePair::getAttributeName, Function.identity()));

                    // 比较两个 Map
                    // 先合并 key
                    Set<String> allKeys = Sets.newHashSet(currentFieldParaMap.keySet());
                    allKeys.addAll(linkFieldParaMap.keySet());

                    List<String> errList = Lists.newArrayList();

                    for (String key : allKeys) {
                        PsiNameValuePair currentFieldPara = currentFieldParaMap.get(key);
                        PsiNameValuePair linkFieldPara = linkFieldParaMap.get(key);
                        if (StrUtil.equalsAny(key, "primaryKey", "generatedKey", "primaryKeyGenerator")) {
                            continue;
                        }
                        if (Objects.isNull(currentFieldPara)) {
                            // 缺少了这个参数
                            errList.add("需要设置 " + linkFieldPara.getText());
                            continue;
                        }
                        if (Objects.isNull(linkFieldPara)) {
                            // 缺少了这个参数
                            errList.add("需要移除 " + currentFieldPara.getText());
                            continue;
                        }
                        if (!currentFieldPara.getText().equals(linkFieldPara.getText())) {
                            // 参数不一致
                            errList.add("需要将 " + currentFieldPara.getText() + "修改为" + linkFieldPara.getText());
                        }
                    }

                    if (!errList.isEmpty()) {
                        @NotNull LocalQuickFix quickFix = new LocalQuickFix() {
                            @Override
                            public @IntentionFamilyName @NotNull String getFamilyName() {
                                return "使用实体注解更新DTO注解";
                            }

                            @Override
                            public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor problemDescriptor) {

                                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                                List<JvmAnnotationAttribute> columnAttrList = linkFieldColumn.getAttributes().stream()
                                    // 排除掉一些不要的属性,剩下的均传递到新的注解上
                                    .filter(attr -> !StrUtil.equalsAny(attr.getAttributeName(), "primaryKey", "generatedKey", "primaryKeyGenerator"))
                                    // 过滤下值为空的
                                    .filter(attr -> Objects.nonNull(attr.getAttributeValue()))
                                    .collect(Collectors.toList());
                                // 过滤后的属性值拼接起来
                                String attrText = columnAttrList.stream().map(attr -> ((PsiNameValuePairImpl) attr).getText()).sorted().collect(Collectors.joining(", "));
                                // 再拼成 @Column 注解文本
                                String replacement = StrUtil.isBlank(attrText) ? "" : "@Column(" + attrText + ")";
                                PsiAnnotation newAnno = elementFactory.createAnnotationFromText(replacement, problemDescriptor.getPsiElement());
                                problemDescriptor.getPsiElement().replace(newAnno);
                            }
                        };
                        holder.registerProblem(annotation, String.join(" \n ", errList), ProblemHighlightType.ERROR, quickFix);
                    }

                }


            }

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


                for (PsiField dtoField : dtoFields) {
                    if (!entityFieldMap.containsKey(dtoField.getName())) {

                        //如果是navigateFlat或者NavigateJoin那么应该忽略
                        PsiAnnotation navigateFlat = dtoField.getAnnotation("com.easy.query.core.annotation.NavigateFlat");
                        if (navigateFlat != null) {
                            continue;
                        }
                        PsiAnnotation navigateJoin = dtoField.getAnnotation("com.easy.query.core.annotation.NavigateJoin");
                        if (navigateJoin != null) {
                            continue;
                        }

                        // 这个字段不在实体类中, 需要警告
                        holder.registerProblem(dtoField, "当前字段在实体类 " + linkClass.getQualifiedName() + " 中不存在", ProblemHighlightType.WARNING);
                        continue;
                    }
                    // 现在是有这个字段, 需要比对类型
                    PsiField entityField = entityFieldMap.get(dtoField.getName());

                    if (dtoField.getType() instanceof PsiClassReferenceType && entityField.getType() instanceof PsiClassReferenceType) {
                        PsiJavaCodeReferenceElement dtoFieldTypeRef = ((PsiClassReferenceType) dtoField.getType()).getReference();
                        String dtoTypeRefName = dtoFieldTypeRef.getQualifiedName();
                        PsiJavaCodeReferenceElement entityFieldTypeRef = ((PsiClassReferenceType) entityField.getType()).getReference();
                        String entityTypeRefName = entityFieldTypeRef.getQualifiedName();
                        if (!StrUtil.equals(dtoTypeRefName, entityTypeRefName)) {
                            if (StrUtil.startWithAny(dtoTypeRefName, "java.")) { // 常见包下面的视作基础类型
                                // 类型不一致
                                holder.registerProblem(dtoField, "当前字段类型和实体类中不一致,应为 " + entityTypeRefName + " 或其生成的DTO", ProblemHighlightType.ERROR);
                                continue;

                            } else {
                                // 复杂类型的, 需要进一步比对
                                PsiClass linkPsiClass = PsiJavaClassUtil.getLinkPsiClass(PsiJavaFileUtil.getPsiClass(currentClass.getProject(), dtoTypeRefName));
                                if (Objects.isNull(linkPsiClass)) {
                                    // 没有找到对应的类, 无法比对， 视为不一致
                                    holder.registerProblem(dtoField, "当前字段类型和实体类中不一致,应为 " + entityTypeRefName + " 或其生成的DTO", ProblemHighlightType.ERROR);
                                    continue;
                                }
                                String linkPsiClassName = linkPsiClass.getQualifiedName();
                                if (!StrUtil.equals(linkPsiClassName, entityTypeRefName)) {
                                    // 类型不一致
                                    holder.registerProblem(dtoField, "当前字段类型和实体类中不一致,应为 " + entityTypeRefName + " 或其生成的DTO", ProblemHighlightType.ERROR);
                                    continue;
                                }
                            }
                        }

                        // 外围的一致，尝试匹配泛型
                        if (dtoFieldTypeRef.getTypeParameterCount() != entityFieldTypeRef.getTypeParameterCount()) {
                            // 泛型的个数不一致,肯定不对
                            holder.registerProblem(dtoField, "当前字段泛型数量和实体类中不一致,应为 " + entityFieldTypeRef.getText(), ProblemHighlightType.ERROR);
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
                                PsiClass linkPsiClass = PsiJavaClassUtil.getLinkPsiClass(PsiJavaFileUtil.getPsiClass(currentClass.getProject(), ((PsiJavaCodeReferenceElement) dtoFieldTypeParamRef).getQualifiedName()));
                                PsiJavaCodeReferenceElement entityFieldTypeParamRef = ((PsiClassReferenceType) entityFieldTypeParam).getReference();
                                if (Objects.isNull(linkPsiClass)) {
                                    // 没有找到对应的类, 无法比对， 视为不一致
                                    holder.registerProblem(dtoField, "当前字段泛型和实体类中不一致,应为 " + entityFieldTypeParamRef.getQualifiedName() + " 或其生成的DTO", ProblemHighlightType.ERROR);
                                    continue;
                                }
                                String linkPsiClassName = linkPsiClass.getQualifiedName();

                                if (!StrUtil.equals(linkPsiClassName, entityFieldTypeParamRef.getQualifiedName())) {
                                    // 类型不一致
                                    holder.registerProblem(dtoField, "当前字段泛型和实体类中不一致,应为 " + entityFieldTypeParamRef.getQualifiedName(), ProblemHighlightType.ERROR);
                                }
                            } else {
                                // 其他情况, 暂不处理
                            }
                        }


                    }


                }

            }
        };
    }
}
