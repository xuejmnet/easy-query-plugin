package com.easy.query.plugin.core.completion;

import com.intellij.codeInsight.completion.*;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

public class DtoFieldAutoCompletion extends CompletionContributor {

    public DtoFieldAutoCompletion() {
        super();
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, @NotNull ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement position = completionParameters.getPosition();

                // 占位, 后续实现键入时提示实体字段
            }
        });
    }
}
