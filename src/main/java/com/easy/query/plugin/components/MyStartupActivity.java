package com.easy.query.plugin.components;

import com.easy.query.plugin.core.EasyQueryDocumentChangeHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;

/**
 * create time 2024/5/2 22:46
 * 文件说明
 *
 * @author xuejiaming
 */
public class MyStartupActivity implements StartupActivity,FileEditorManagerListener, ProjectActivity {
    private static final Logger log = Logger.getInstance(MyStartupActivity.class);
    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        FileEditor oldEditor = event.getOldEditor();
        try {
            if (oldEditor != null) {
                VirtualFile file = oldEditor.getFile();
                if (file != null) {
                    Project project = event.getManager().getProject();
                    EasyQueryDocumentChangeHandler.createAptFile(Collections.singletonList(file), project, false);
                }
            }
        } catch (Exception ex) {
            log.warn("selectionChanged try apt error", ex);
        }
    }

//    @Override
//    public void projectOpened() {
//    }

    @Override
    public void runActivity(@NotNull Project project) {
        MessageBus messageBus = project.getMessageBus();
        messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);

    }

    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        return null;
    }

//    @Override
//    public void projectClosed() {
//        MessageBus messageBus = project.getMessageBus();
//        messageBus.connect().(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
//    }
}
