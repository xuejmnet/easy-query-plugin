package com.easy.query.plugin.components;

import com.easy.query.plugin.action.RunEasyQueryInspectionAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.startup.StartupActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 项目启动时运行EasyQuery检查
 * @author link2fun
 */
public class EasyQueryInspectionStartupActivity implements StartupActivity, ProjectActivity {
    private static final Logger log = Logger.getInstance(EasyQueryInspectionStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        // 在项目启动时运行EasyQuery检查
        log.info("启动时运行EasyQuery检查：" + project.getName());
        runInspection(project);
    }

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 在项目启动时运行EasyQuery检查
        log.info("启动时运行EasyQuery检查：" + project.getName());
        runInspection(project);
        return Unit.INSTANCE;
    }

    private void runInspection(Project project) {
        try {
            // 在后台线程中执行检查，避免EDT线程阻塞
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    // 使用RunEasyQueryInspectionAction中的方法运行检查
                    RunEasyQueryInspectionAction inspectionAction = new RunEasyQueryInspectionAction();
                    inspectionAction.runInspectionForProject(project);
                } catch (Exception e) {
                    log.warn("后台线程中运行EasyQuery检查失败", e);
                }
            });
        } catch (Exception e) {
            log.warn("启动时运行EasyQuery检查失败", e);
        }
    }
} 