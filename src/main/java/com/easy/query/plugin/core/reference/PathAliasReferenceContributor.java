package com.easy.query.plugin.core.reference;

import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * 引用贡献者，用于注册处理 pathAlias 属性值的引用提供者。
 */
public class PathAliasReferenceContributor extends PsiReferenceContributor {

    /**
     * 注册引用提供者。
     *
     * @param registrar 引用注册器
     */
    @Override
    public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiLiteralExpression.class),
            new PsiReferenceProvider() {
                @NotNull
                @Override
                public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
                    if (element instanceof PsiLiteralExpression) {
                        PsiLiteralExpression literalExpression = (PsiLiteralExpression) element;
                        String value = literalExpression.getValue() instanceof String ? (String) literalExpression.getValue() : null;
                        if (value != null && isNavigateFlatPathAlias(element)) {
                            // 将 pathAlias 以英文句号分割成多个段落
                            String[] pathSegments = value.split("\\.");
                            PsiReference[] references = new PsiReference[pathSegments.length];
                            // 为每个段落创建独立的引用
                            for (int i = 0; i < pathSegments.length; i++) {
                                references[i] = new PathAliasNavigationHandler(literalExpression, pathSegments);
                            }
                            return references;
                        }
                    }
                    return PsiReference.EMPTY_ARRAY;
                }

                /**
                 * 检查元素是否是 NavigateFlat 注解的 pathAlias 属性
                 *
                 * @param element 待检查的元素
                 * @return 如果是 NavigateFlat 注解的 pathAlias 属性则返回 true，否则返回 false
                 */
                private boolean isNavigateFlatPathAlias(PsiElement element) {
                    PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
                    if (annotation == null) {
                        return false;
                    }
                    String qualifiedName = annotation.getQualifiedName();
                    if(element.getParent() instanceof PsiNameValuePair){
                        return "com.easy.query.core.annotation.NavigateFlat".equals(qualifiedName) &&
                            "pathAlias".equals(((PsiNameValuePair) element.getParent()).getName());
                    }
                    return false;
                }
            }
        );
    }
}