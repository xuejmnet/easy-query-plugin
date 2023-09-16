package com.easy.query.plugin.action;

import com.easy.query.plugin.core.util.ProjectUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class AutoCompileAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getProject();
        ProjectUtils.setCurrentProject(project);
        PsiJavaFileUtil.createAptFile();
    }
}
