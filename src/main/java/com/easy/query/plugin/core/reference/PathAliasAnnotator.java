package com.easy.query.plugin.core.reference;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 自定义注释器，用于在找不到引用时添加错误注释。
 */
public class PathAliasAnnotator implements Annotator {

    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (element instanceof PsiLiteralExpression) {
            PsiLiteralExpression literalExpression = (PsiLiteralExpression) element;
            String value = literalExpression.getValue() instanceof String ? (String) literalExpression.getValue() : null;
            if (value != null && isNavigateFlatPathAlias(element)) {
                // 将 pathAlias 以英文句号分割成多个段落
                String[] pathSegments = value.split("\\.");
                for (String segment : pathSegments) {
                    PsiElement targetElement = findSegmentTargetElement(element.getProject(), element, segment);
                    if (targetElement == null) {
                        holder.newAnnotation(HighlightSeverity.ERROR, "找不到对应的引用: " + segment)
                              .range(element.getTextRange())
                              .create();
                    }
                }
            }
        }
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
        return "com.easy.query.core.annotation.NavigateFlat".equals(qualifiedName) &&
               "pathAlias".equals(((PsiNameValuePair) element.getParent()).getName());
    }

    /**
     * 查找单个段落的目标元素的逻辑。
     *
     * @param project        当前项目
     * @param currentElement 当前元素
     * @param segment        当前段落
     * @return 目标 PsiElement
     */
    @Nullable
    private PsiElement findSegmentTargetElement(Project project, PsiElement currentElement, String segment) {
        // 在此实现查找目标元素的逻辑
        // 此处仅作示例，返回 null
        return null;
    }
}