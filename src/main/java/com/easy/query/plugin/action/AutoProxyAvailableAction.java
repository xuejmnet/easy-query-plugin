//package com.easy.query.plugin.action;
//
//import com.intellij.codeInsight.generation.actions.BaseGenerateAction;
//import com.intellij.openapi.editor.Editor;
//import com.intellij.openapi.project.Project;
//import com.intellij.psi.PsiFile;
//import org.jetbrains.annotations.NotNull;
//
///**
// * create time 2024/5/15 11:09
// * 文件说明
// *
// * @author xuejiaming
// */
//public class AutoProxyAvailableAction extends BaseGenerateAction {
//    protected AutoProxyAvailableAction() {
//        super(new AutoProxyAvailableHandler());
//    }
//    @Override
//    protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
//        return file.isWritable() && super.isValidForFile(project, editor, file);
//    }
//}
