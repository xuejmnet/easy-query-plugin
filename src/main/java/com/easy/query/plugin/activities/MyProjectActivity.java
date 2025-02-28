package com.easy.query.plugin.activities;

import com.easy.query.plugin.core.startup.ProjectStartupHelper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 新版本的项目启动活动
 * 使用新的ProjectActivity接口
 * 
 * @author link2fun
 */
public class MyProjectActivity implements ProjectActivity {


    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 使用共享的初始化逻辑
        ProjectStartupHelper.initializeProject(project);
        return Unit.INSTANCE;
    }
}
