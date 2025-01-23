package com.easy.query.plugin.core.util;

import com.intellij.psi.JavaDocTokenType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;

import java.util.stream.Collectors;

public class PsiCommentUtil {


    public static String getCommentDataStr(PsiDocComment docComment) {
        if (docComment == null || ArrayUtil.isEmpty(docComment.getChildren())) {
            return StrUtil.EMPTY;
        }
        return PsiTreeUtil.findChildrenOfType(docComment, PsiDocToken.class).stream().filter(ele -> ele.getTokenType() == JavaDocTokenType.DOC_COMMENT_DATA)
                .map(PsiElement::getText)
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trimToEmpty)
                .collect(Collectors.joining("; "));
    }
}
