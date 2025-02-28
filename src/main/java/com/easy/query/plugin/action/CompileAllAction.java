package com.easy.query.plugin.action;

import com.easy.query.plugin.core.startup.ProjectStartupHelper;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

public class CompileAllAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        PsiJavaFileUtil.createAptFile(project);

        // 更新 generated sources root ，生成的代码需要标记一下
        ProjectStartupHelper.updateGeneratedSourceRoot(project);

        // 添加气泡提醒
        NotificationUtils.notifySuccess("编译完成", "EasyQuery", project);
    }
}
