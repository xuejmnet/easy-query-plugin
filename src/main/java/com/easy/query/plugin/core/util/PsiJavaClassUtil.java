package com.easy.query.plugin.core.util;

import cn.hutool.core.util.ReUtil;
import com.google.common.collect.Lists;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiJavaFileImpl;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiInlineDocTag;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * PsiJavaClass 工具类
 *
 * @author link2fun
 */
@Slf4j
public class PsiJavaClassUtil {

    /**
     * 获取 @link 注解的类
     *
     * @param currentClass 当前类
     * @return {@code PsiClass}
     */
    public static PsiClass getLinkPsiClass(PsiClass currentClass) {
        String linkPsiClassQualifiedName = getLinkPsiClassQualifiedName(currentClass);
        if (StrUtil.isBlank(linkPsiClassQualifiedName)) {
            return null; // 如果没有找到主实体类的完全限定名，返回 null
        }
        return PsiJavaFileUtil.getPsiClass(currentClass.getProject(), linkPsiClassQualifiedName);
    }

    /**
     * 获取 @link 注解的类名称(全限定名)
     *
     * @param currentClass 当前类
     * @return {@code String}
     */
    public static String getLinkPsiClassQualifiedName(PsiClass currentClass) {
        if (currentClass == null) {
            return "";
        }
        PsiDocComment docComment = currentClass.getDocComment();
        if (Objects.isNull(docComment)) {
            return ""; // 当前类上没有文档注释, 无法校验
        }

        // 有文档注释, 尝试提取
        PsiElement linkDocEle = Arrays.stream(docComment.getDescriptionElements())
                .filter(ele -> ele instanceof PsiInlineDocTag)
                .findFirst()
                .orElse(null);
        if (Objects.isNull(linkDocEle)) {
            return ""; // 没有找到 @link, 无法校验
        }

        // 有 @link 尝试提取
        String mainEntityClassFromLink = ReUtil.getGroup1("\\{@link *(\\S+) *\\}", linkDocEle.getText());
        String mainEntityClassQualifiedName = "";

        // 这个 mainEntityClassFromLink 就是我们要找的主实体类, 但是可能不包含包名
        if (!cn.hutool.core.util.StrUtil.contains(mainEntityClassFromLink, ".")) {
            // 没有包含 . 说明没有包名, 需要从上下文中提取, 优先从 import 中提取
            List<String> importList = getImportLinesQualifiedName(currentClass);
            for (String importStr : importList) {
                if (importStr.endsWith("." + mainEntityClassFromLink)) {
                    mainEntityClassQualifiedName = importStr;
                    break;
                } else if (importStr.endsWith(".*")) {
                    String portableClassName = importStr.substring(0, importStr.length() - 1) + mainEntityClassFromLink;
                    if (PsiJavaFileUtil.getPsiClass(currentClass.getProject(), portableClassName) != null) {
                        mainEntityClassQualifiedName = portableClassName;
                        break;
                    } else {
                    }
                }
            }
        } else {
            mainEntityClassQualifiedName = mainEntityClassFromLink;
        }

        return mainEntityClassQualifiedName; // 返回主实体类的完全限定名
    }


    /**
     * 从 PsiClass 中提取 import 行信息, 未经解析
     * eg: import xx.xx;
     *
     * @param psiClass {@code PsiClass}
     */
    public List<String> getImportLines(final PsiClass psiClass) {
        PsiImportList importList = ((PsiJavaFileImpl) psiClass.getContainingFile()).getImportList();
        if (Objects.isNull(importList)) {
            return Lists.newArrayList();
        }
        return Arrays.stream(importList.getAllImportStatements()).map(PsiElement::getText).collect(Collectors.toList());
    }


    /**
     * 从 PsiClass 中提取 import 的全限定名
     * eg: import com.easyquery -> com.easyquery
     */
    public static List<String> getImportLinesQualifiedName(final PsiClass psiClass) {
        PsiImportList importList = ((PsiJavaFileImpl) psiClass.getContainingFile()).getImportList();
        if (Objects.isNull(importList)) {
            return Lists.newArrayList();
        }

        return Arrays.stream(importList.getAllImportStatements())
                .map(statement -> {
                    return Arrays.stream(statement.getChildren())
                            // 过滤掉关键字 import
                            .filter(child -> {
                                if (child instanceof PsiKeyword) {
                                    // 不要 keyword 这里面一般是 import
                                    return false;
                                } else if (child instanceof PsiJavaToken) {
                                    // 过滤掉一些特殊字符
                                    if (StrUtil.equalsAny(child.getText(), ";", " ")) {
                                        return false;
                                    }
                                }
                                // 剩下的都要
                                return true;
                            })
                            .map(PsiElement::getText) // 提取元素文本
                            .collect(Collectors.joining(""));
                })
                .map(StrUtil::trimToEmpty) // 拼接后去掉首尾空格
                .filter(StrUtil::isNotBlank) // 过滤掉空行
                .collect(Collectors.toList());
    }


    public static boolean hasAnnoTable(PsiClass psiClass) {
        if (psiClass == null) {
            return false;
        }
        return psiClass.getAnnotation("com.easy.query.core.annotation.Table") != null;
    }


    /**
     * 元素父级需要是 class 而不是method
     * @param element
     */
    public static boolean isElementRelatedToClass(PsiElement element) {
        if (element == null) {
            return false;
        }
        PsiElement parent = element.getParent();
        while (!(parent instanceof PsiClass)) {
            if (parent instanceof PsiMethod) {
                return false;
            }
            parent = parent.getParent();
        }

        return true;
    }




}
