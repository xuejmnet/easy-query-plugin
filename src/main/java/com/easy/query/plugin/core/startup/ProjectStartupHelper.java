package com.easy.query.plugin.core.startup;

import com.easy.query.plugin.config.EasyQueryConfigManager;
import com.easy.query.plugin.core.EasyQueryDocumentChangeHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

/**
 * 项目启动帮助类
 * 提供共享逻辑，供不同版本的启动活动使用
 *
 * @author link2fun
 */
public class ProjectStartupHelper {
    private static final Logger log = Logger.getInstance(ProjectStartupHelper.class);

    /**
     * 初始化项目
     * @param project 当前项目
     */
    public static void initializeProject(@NotNull Project project) {
        // 1. 预加载配置，避免在需要时出现同步问题
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                EasyQueryConfigManager.getInstance().getConfig(project);
            } catch (Exception e) {
                log.warn("Failed to preload configuration for project: " + project.getName(), e);
            }
        });
        
        // 2. 注册文件编辑器事件监听
        registerFileEditorListener(project);
        
        // 3. 其他初始化逻辑
        // ...
    }
    
    /**
     * 注册文件编辑器事件监听
     * @param project 当前项目
     */
    public static void registerFileEditorListener(@NotNull Project project) {
        MessageBus messageBus = project.getMessageBus();
        messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void selectionChanged(@NotNull FileEditorManagerEvent event) {
                try {
                    if (event.getOldEditor() != null && event.getOldEditor().getFile() != null) {
                        EasyQueryDocumentChangeHandler.createAptFile(
                                java.util.Collections.singletonList(event.getOldEditor().getFile()),
                                project,
                                false
                        );
                    }
                } catch (Exception ex) {
                    log.warn("selectionChanged try apt error", ex);
                }
            }
        });
    }
}
