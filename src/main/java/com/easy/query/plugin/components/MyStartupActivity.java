package com.easy.query.plugin.components;

import com.easy.query.plugin.core.EasyQueryDocumentChangeHandler;
import com.easy.query.plugin.core.startup.ProjectStartupHelper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFile;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
 * 旧版本的启动活动，用于兼容早期版本的 IDE
 * 
 * @author xuejiaming
 */
public class MyStartupActivity implements StartupActivity, ProjectActivity {
    private static final Logger log = Logger.getInstance(MyStartupActivity.class);

    @Override
    public void runActivity(@NotNull Project project) {
        // 使用共享的初始化逻辑
        ProjectStartupHelper.initializeProject(project);
    }

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        // 使用共享的初始化逻辑
        ProjectStartupHelper.initializeProject(project);
        return Unit.INSTANCE;
    }
}
