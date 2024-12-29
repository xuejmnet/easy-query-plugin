package com.easy.query.plugin.core.provider.doctag;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.javadoc.JavadocTagInfo;
import com.intellij.psi.javadoc.PsiDocTagValue;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;


/**
 * @easy-query-dto EasyQueryDTODocTagInfo
 * 提供自定义的javadoc标签
 */
public class EasyQueryDTODocTagInfo implements JavadocTagInfo {

    @Override
    public String getName() {
        return "easy-query-dto";
    }

    @Override
    public boolean isInline() {
        return false;
    }

    @Override
    public boolean isValidInContext(PsiElement psiElement) {
        return true;
    }

    @Override
    public @Nullable @Nls String checkTagValue(PsiDocTagValue psiDocTagValue) {
        return null;
    }

    @Override
    public @Nullable PsiReference getReference(PsiDocTagValue psiDocTagValue) {
        return null;
    }
}
