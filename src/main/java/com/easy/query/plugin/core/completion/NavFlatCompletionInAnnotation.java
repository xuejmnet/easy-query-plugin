package com.easy.query.plugin.core.completion;

import com.easy.query.plugin.core.util.PsiUtil;
import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.codeInsight.completion.SkipAutopopupInStrings;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;

/**
 * 导航映射自动补全
 *
 * @author link2fun, xuejiaming
 */
public class NavFlatCompletionInAnnotation extends CompletionConfidence {
    @Override
    public @NotNull ThreeState shouldSkipAutopopup(@NotNull PsiElement contextElement, @NotNull PsiFile psiFile, int offset) {
        boolean inStringLiteral = SkipAutopopupInStrings.isInStringLiteral(contextElement);
        if (inStringLiteral) {//判断在字符串里面后续判断在注解@NavigateFlat中
            ;
            boolean easyQueryAnnotation = PsiUtil.isEasyQueryNavigateFlatJoinAnnotation(contextElement);
            if (easyQueryAnnotation) {
                return ThreeState.NO;
            }
        }
        return super.shouldSkipAutopopup(contextElement, psiFile, offset);
    }
}
