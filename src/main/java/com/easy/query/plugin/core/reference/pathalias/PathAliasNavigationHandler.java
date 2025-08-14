package com.easy.query.plugin.core.reference.pathalias;

import com.easy.query.plugin.core.ResultWithError;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiReferenceBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.easy.query.plugin.core.reference.ReferenceSegementUtils.findSegmentTargetElement;

/**
 * 自定义导航处理器，用于处理 pathAlias 属性值的跳转。
 */
public class PathAliasNavigationHandler extends PsiReferenceBase<PsiLiteralExpression> {

    private final String[] pathSegments;

    /**
     * 构造函数
     *
     * @param element      包含 pathAlias 字符串的 PsiLiteralExpression
     * @param pathSegments pathAlias 的分割值数组
     */
    public PathAliasNavigationHandler(@NotNull PsiLiteralExpression element, String[] pathSegments) {
        super(element);
        this.pathSegments = pathSegments;
    }

    /**
     * 解析 pathAlias 并返回目标 PsiElement。
     *
     * @return 目标 PsiElement，如果找不到则返回当前元素
     */
    @Nullable
    @Override
    public PsiElement resolve() {
        Project project = getElement().getProject();
        // 依次解析每个段落，找到最终的目标元素
        PsiElement targetElement = getElement();
//        for (int i = 0; i < pathSegments.length; i++) {
//            String[] subPathSegments = new String[i + 1];
//            System.arraycopy(pathSegments, 0, subPathSegments, 0, i + 1);
        ResultWithError<PsiElement> resultWithError = findSegmentTargetElement(project, targetElement, pathSegments);

//            if (targetElement == null) {
//                break;
//            }
//        }
//        for (int i = 0; i < pathSegments.length; i++) {
//            String[] subPathSegments = new String[i + 1];
//            System.arraycopy(pathSegments, 0, subPathSegments, 0, i + 1);
//            targetElement = findSegmentTargetElement(project, targetElement, subPathSegments);
//            if (targetElement == null) {
//                break;
//            }
//        }
        return resultWithError.result != null ? resultWithError.result : getElement();
    }

    /**
     * 提供补全选项（如果需要）。
     *
     * @return 补全选项
     */
    @NotNull
    @Override
    public Object[] getVariants() {
        return new Object[0];
    }
}