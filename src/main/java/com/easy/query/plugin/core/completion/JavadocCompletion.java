package com.easy.query.plugin.core.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

/**
 * create time 2025/1/28 11:26
 * 文件说明
 *
 * @author xuejiaming
 */
public class JavadocCompletion extends CompletionContributor {

    public JavadocCompletion() {
        extend(
            CompletionType.BASIC,
//            // 匹配 Javadoc 注释中的文本
//            PsiJavaPatterns.psiElement().inside(PsiDocComment.class),
            // 匹配 Javadoc 注释中以 "@" 开头的文本（例如 "@link"）
            createJavadocTagPattern(),

            new CompletionProvider<CompletionParameters>() {
                @Override
                protected void addCompletions(
                    @NotNull CompletionParameters parameters,
                    @NotNull ProcessingContext context,
                    @NotNull CompletionResultSet result
                ) {
                    // 添加 "link" 的补全建议
                    result.addElement(
                        LookupElementBuilder.create("link")
                            .withInsertHandler((ctx, item) -> {
                                // 获取当前输入的文本范围（包括前面的 "@"）
                                int startOffset = ctx.getStartOffset() - 1; // 包含 "@"
                                int endOffset = ctx.getTailOffset();

                                // 替换 "@link" 为 "{@link }"
                                ctx.getDocument().replaceString(
                                    startOffset,
                                    endOffset,
                                    "{@link }"
                                );

                                // 光标定位到 "}" 前
                                ctx.getEditor().getCaretModel().moveToOffset(startOffset + 7);
                            })
                            .withBoldness(true)
                            .withPresentableText("link → {@link }")
                    );
                }
            }
        );
    }

    // 匹配 Javadoc 注释中以 "@" 开头的文本
    private static PsiElementPattern.Capture<PsiElement> createJavadocTagPattern() {
        return PlatformPatterns.psiElement()
            .inside(PsiDocComment.class);
//            .and(PlatformPatterns.psiElement().afterLeaf("@")) // 仅在 "@" 后触发
//            .withText(PlatformPatterns.string().startsWith("link")); // 匹配以 "link" 开头的输入
    }
}