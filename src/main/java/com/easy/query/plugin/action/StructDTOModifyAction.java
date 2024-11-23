package com.easy.query.plugin.action;

import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

/**
 * create time 2024/11/23 14:52
 * 文件说明
 *
 * @author xuejiaming
 */
public class StructDTOModifyAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getProject();
        Messages.showErrorDialog(project, "功能开发中", "提示");
//        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
//        PsiJavaFileUtil.createAptFile(project);
    }
}
