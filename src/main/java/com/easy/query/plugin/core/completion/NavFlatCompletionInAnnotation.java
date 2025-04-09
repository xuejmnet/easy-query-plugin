package com.easy.query.plugin.core.completion;

import com.intellij.codeInsight.completion.CompletionConfidence;
import com.intellij.codeInsight.completion.SkipAutopopupInStrings;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 导航映射自动补全
 *
 * @author link2fun,xuejiaming
 */
public class NavFlatCompletionInAnnotation extends CompletionConfidence {
    @Override
    public @NotNull ThreeState shouldSkipAutopopup(@NotNull PsiElement contextElement, @NotNull PsiFile psiFile, int offset) {
        boolean inStringLiteral = SkipAutopopupInStrings.isInStringLiteral(contextElement);
        if (inStringLiteral) {//判断在字符串里面后续判断在注解@NavigateFlat中
            boolean easyQueryAnnotation = isEasyQueryAnnotation(contextElement);
            if(easyQueryAnnotation){
                return ThreeState.NO;
            }
        }
        return super.shouldSkipAutopopup(contextElement, psiFile, offset);
    }
    private static final Set<String> EASY_QUERY_ANNONTATIONS=new HashSet<>(Arrays.asList(
            "com.easy.query.core.annotation.NavigateFlat",
            "com.easy.query.core.annotation.NavigateJoin"
    ));
    private boolean isEasyQueryAnnotation(PsiElement contextElement){
        PsiElement parent = contextElement.getParent();
        if(parent!=null){
            parent = parent.getParent();
            if(parent!=null){
                parent = parent.getParent();
                if(parent!=null){
                    PsiElement prevSibling = parent.getPrevSibling();
                    if(prevSibling!=null){
                        String text = prevSibling.getText();
                        if("NavigateFlat".equals(text) || "NavigateJoin".equals(text)){
                            if(prevSibling.getContext() instanceof PsiAnnotation){
                                String qualifiedName = ((PsiAnnotation) prevSibling.getContext()).getQualifiedName();
                                return EASY_QUERY_ANNONTATIONS.contains(qualifiedName);
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
}
