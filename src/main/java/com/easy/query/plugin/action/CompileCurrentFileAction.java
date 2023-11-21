package com.easy.query.plugin.action;

import com.easy.query.plugin.core.util.ProjectUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public class CompileCurrentFileAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getProject();
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        ProjectUtils.setCurrentProject(project);
        PsiJavaFileUtil.createAptCurrentFile(virtualFile,project);
    }
}
