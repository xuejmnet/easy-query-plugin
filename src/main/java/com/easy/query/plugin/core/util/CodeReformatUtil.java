package com.easy.query.plugin.core.util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.codeStyle.CodeStyleManager;

/**
 * create time 2023/12/1 08:53
 * 文件说明
 *
 * @author xuejiaming
 */
public class CodeReformatUtil {
    /**
     * 重新格式化
     *
     * @param psiElement psi元素
     * @return {@code PsiElement}
     */
    public static PsiElement reformat(PsiElement psiElement) {
        Project project = psiElement.getProject();
        return CodeStyleManager.getInstance(project).reformat(psiElement);
    }
}
