package com.easy.query.plugin.core.entity;

import com.intellij.psi.PsiFile;

/**
 * create time 2023/12/10 18:30
 * 文件说明
 *
 * @author xuejiaming
 */
public class GenerateFileEntry {
    private final PsiFile psiFile;
    private boolean allCompileFrom;
    private final String strategy;

    public GenerateFileEntry(PsiFile psiFile, boolean allCompileFrom, String strategy) {

        this.psiFile = psiFile;
        this.allCompileFrom = allCompileFrom;
        this.strategy = strategy;
    }

    public PsiFile getPsiFile() {
        return psiFile;
    }

    public boolean isOverrideWrite() {
        //如果不是EntityFileProxy就直接返回
        if (strategy == null) {
            return true;
        }

        if ("com.easy.query.core.enums.FileGenerateEnum.GENERATE_CURRENT_COMPILE_OVERRIDE".equals(strategy)) {
            return !allCompileFrom;
        }
        return "com.easy.query.core.enums.FileGenerateEnum.GENERATE_COMPILE_ALWAYS_OVERRIDE".equals(strategy);
    }
}
