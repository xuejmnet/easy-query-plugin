package com.easy.query.plugin.core.util;

import cn.hutool.core.util.ReUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiInlineDocTag;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;

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
            Set<String> importSet = PsiJavaFileUtil
                    .getQualifiedNameImportSet((PsiJavaFile) currentClass.getContainingFile());
            for (String importStr : importSet) {
                if (importStr.endsWith("." + mainEntityClassFromLink)) {
                    mainEntityClassQualifiedName = importStr;
                    break;
                } else if (importStr.endsWith(".*")) {
                    String portableClass = importStr.substring(0, importStr.length() - 1) + mainEntityClassFromLink;
                    if (PsiJavaFileUtil.getPsiClass(currentClass.getProject(), portableClass) != null) {
                        mainEntityClassQualifiedName = portableClass;
                        break;
                    }
                }
            }
        } else {
            mainEntityClassQualifiedName = mainEntityClassFromLink;
        }

        return mainEntityClassQualifiedName; // 返回主实体类的完全限定名
    }
}
