//package com.easy.query.plugin.core.contributor;
//
//import cn.hutool.core.util.StrUtil;
//import com.easy.query.plugin.core.icons.Icons;
//import com.easy.query.plugin.core.util.VirtualFileUtils;
//import com.intellij.codeInsight.completion.CompletionContributor;
//import com.intellij.codeInsight.completion.CompletionParameters;
//import com.intellij.codeInsight.completion.CompletionResultSet;
//import com.intellij.codeInsight.lookup.LookupElement;
//import com.intellij.codeInsight.lookup.LookupElementBuilder;
//import com.intellij.openapi.command.WriteCommandAction;
//import com.intellij.openapi.editor.Document;
//import com.intellij.openapi.editor.Editor;
//import com.intellij.openapi.editor.LogicalPosition;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.util.TextRange;
//import com.intellij.openapi.vfs.VirtualFile;
//import com.intellij.psi.JavaPsiFacade;
//import com.intellij.psi.PsiClass;
//import com.intellij.psi.PsiElement;
//import com.intellij.psi.PsiFile;
//import com.intellij.psi.PsiInvalidElementAccessException;
//import com.intellij.psi.PsiJavaFile;
//import com.intellij.psi.PsiManager;
//import com.intellij.psi.PsiWhiteSpace;
//import com.intellij.psi.search.GlobalSearchScope;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.kotlin.psi.KtFile;
//
//import java.util.List;
//import java.util.Map;
//
///**
// * create time 2024/1/29 21:37
// * 文件说明
// *
// * @author xuejiaming
// */
//public class EasyQueryAnonymousTypeCompletionContributor extends CompletionContributor {
//    @Override
//    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
//        try {
//            Editor editor = parameters.getEditor();
//            Project project = parameters.getPosition().getProject();
//            Document document = editor.getDocument();
//            PsiFile psiFile = VirtualFileUtils.getPsiFile(project,document);
//            int offset = editor.getCaretModel().getOffset();
//            String text = parameters.getPosition().getText();
//            String inputText = StrUtil.subBefore(text, "IntellijIdeaRulezzz", true);
//            if(StrUtil.isBlank(inputText)){
//                return;
//            }
//            System.out.println("输入内容："+inputText);
//            System.out.println("输入内容index："+offset);
//            // 添加代码提示
//            addCodeTip(result,project,document, inputText,offset);
//        } catch (Exception e) {
//            System.out.println(e);
//        }
//    }
//    private void addCodeTip(@NotNull CompletionResultSet result, Project project, Document document,String text,int start) {
//        // 获取忽略大小写的结果集
//        CompletionResultSet completionResultSet = result.caseInsensitive();
//        LookupElementBuilder anonymousElement = LookupElementBuilder.create(text)
//                .withTypeText("EasyQuery", true)
//                .withInsertHandler((context, item) -> {
//                    WriteCommandAction.runWriteCommandAction(project, () -> {
//                        document.insertString(start-text.length(),"MapProxy(\""+text+"\")");
//                    });
//
//                })
//                .withIcon(Icons.EQ);
//        completionResultSet.addElement(anonymousElement);
//    }
//}
