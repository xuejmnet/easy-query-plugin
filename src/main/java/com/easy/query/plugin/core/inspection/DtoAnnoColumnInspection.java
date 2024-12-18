package com.easy.query.plugin.core.inspection;


import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiInlineDocTag;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DTO Column 注解检测<br/>
 * DTO Column 注解应该和 实体上的 Column 注解的属性保持一致
 * @author link2fun
 */

public class DtoAnnoColumnInspection extends AbstractBaseJavaLocalInspectionTool {


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

                if (annotation.getContext() == null || annotation.getContext().getProject() == null) {
                    // 缺少上下文无法处理
                    return;
                }

                PsiClass currentClass = null;
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


                // 有文档注释, 尝试提取
                PsiElement linkDocEle = Arrays.stream(docComment.getDescriptionElements()).filter(ele -> ele instanceof PsiInlineDocTag).findFirst().orElse(null);
                if (Objects.isNull(linkDocEle)) {
                    // 没有找到 @link, 无法校验, 直接跳过
                    return;
                }

                // 有 @link 尝试提取
                String mainEntityClassFromLink = ReUtil.getGroup1("\\{@link *(\\S+) *\\}", linkDocEle.getText());
                String mainEntityClassQualifiedName = "";

                // 这个 mainEntityClassFromLink 就是我们要找的主实体类, 但是可能不包含包名
                if (!StrUtil.contains(mainEntityClassFromLink, ".")) {
                    // 没有包含 . 说明没有包名, 需要从上下文中提取, 优先从 import 中提取
                    Set<String> importSet = PsiJavaFileUtil.getQualifiedNameImportSet((PsiJavaFile) annotation.getParent().getParent().getParent().getContainingFile());
                    for (String importStr : importSet) {
                        if (importStr.endsWith("." + mainEntityClassFromLink)) {
                            mainEntityClassQualifiedName = importStr;
                            break;
                        } else if (importStr.endsWith(".*")) {
                            String portableClass = importStr.substring(0, importStr.length() - 1) + mainEntityClassFromLink;
                            if (PsiJavaFileUtil.getPsiClass(annotation.getContext().getProject(), portableClass) != null) {
                                mainEntityClassQualifiedName = portableClass;
                                break;
                            }
                        }
                    }
                } else {
                    mainEntityClassQualifiedName = mainEntityClassFromLink;
                }

                // 如果最终还是没找到的话, 那就直接跳过
                if (StrUtil.isBlank(mainEntityClassQualifiedName)) {
                    return;
                }


                // 找到了, 需要去对应类中找到字段
                PsiClass linkClass = PsiJavaFileUtil.getPsiClass(annotation.getContext().getProject(), mainEntityClassQualifiedName);


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
                        if (StrUtil.equalsAny(key, "primaryKey", "generatedKey","primaryKeyGenerator")) {
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
                        holder.registerProblem(annotation, String.join(" \n ", errList), quickFix);
                    }

                }


            }

            @Override
            public void visitAnnotationParameterList(@NotNull PsiAnnotationParameterList list) {
                super.visitAnnotationParameterList(list);
            }
        };
    }
}
