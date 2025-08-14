package com.easy.query.plugin.core.reference.pathalias;

import com.easy.query.plugin.core.ResultWithError;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import static com.easy.query.plugin.core.reference.ReferenceSegementUtils.findSegmentTargetElement;

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
                ResultWithError<PsiElement> resultWithError = findSegmentTargetElement(element.getProject(), element, pathSegments);
                if (resultWithError.result == null) {
                    holder.newAnnotation(HighlightSeverity.ERROR, "找不到对应的引用: " + String.join(".", pathSegments)+ " "+resultWithError.error)
                            .range(element.getTextRange())
                            .create();
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
        boolean isNav = "com.easy.query.core.annotation.NavigateFlat".equals(qualifiedName)
            ||
            "com.easy.query.core.annotation.NavigateJoin".equals(qualifiedName);
        if(isNav){
            if(element.getParent() instanceof PsiNameValuePair){
                return "pathAlias".equals(((PsiNameValuePair) element.getParent()).getName());
            }
        }
//
//
//         isNav = "com.easy.query.core.annotation.EasyWhereCondition".equals(qualifiedName);
//        if(isNav){
//            if(element.getParent() instanceof PsiNameValuePair){
//                return "propName".equals(((PsiNameValuePair) element.getParent()).getName())
//                    ||
//                    "propNames".equals(((PsiNameValuePair) element.getParent()).getName());
//            }
//        }
        return false;
    }

}