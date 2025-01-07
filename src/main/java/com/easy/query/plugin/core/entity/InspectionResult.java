package com.easy.query.plugin.core.entity;

import cn.hutool.core.collection.CollectionUtil;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * Inspection 信息
 */
@Data
public class InspectionResult {


    private List<Problem> problemList;


    /**
     * 是否有问题需要抛出
     */
    public Boolean hasProblem() {
        return CollectionUtil.isNotEmpty(problemList);
    }


    /**
     * 添加一个问题和解决方案
     * @param psiElement 有问题的元素
     * @param descriptionTemplate 描述模板
     * @param highlightType 高亮类型
     * @param fixes 解决方案
     * @return this
     */
    public InspectionResult addProblem(PsiElement psiElement, String descriptionTemplate, ProblemHighlightType highlightType, List<LocalQuickFix> fixes) {
        Problem problem = new Problem(psiElement, descriptionTemplate, highlightType, fixes);
        if (problemList == null) {
            problemList = CollectionUtil.newArrayList();
        }
        problemList.add(problem);
        return this;
    }




    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Problem {


        private PsiElement psiElement;
        private String descriptionTemplate;
        private ProblemHighlightType highlightType;
        private List<LocalQuickFix> fixes;

    }


    // ====== 静态构建 ======


    /**
     * 没有问题 返回一个空的 InspectionResult
     */
    public static InspectionResult noProblem() {
        InspectionResult inspectionResult = new InspectionResult();
        inspectionResult.setProblemList(Collections.emptyList());
        return inspectionResult;
    }


    /**
     * 有问题 返回一个 InspectionResult
     */
    public static InspectionResult newResult() {
        InspectionResult inspectionResult = new InspectionResult();
        inspectionResult.setProblemList(CollectionUtil.newArrayList());
        return inspectionResult;
    }


}
