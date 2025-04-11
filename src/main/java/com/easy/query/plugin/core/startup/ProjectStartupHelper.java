package com.easy.query.plugin.core.startup;

import com.easy.query.plugin.action.RunEasyQueryInspectionAction;
import com.easy.query.plugin.config.EasyQueryConfigManager;
import com.easy.query.plugin.core.EasyQueryDocumentChangeHandler;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.google.common.collect.Lists;
import com.intellij.ide.SaveAndSyncHandler;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaSourceRootProperties;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;

import java.io.File;
import java.util.List;

import static com.intellij.ide.projectView.actions.MarkRootActionBase.findContentEntry;

/**
 * 项目启动帮助类
 * 提供共享逻辑，供不同版本的启动活动使用
 */
public class ProjectStartupHelper {
    private static final Logger log = Logger.getInstance(ProjectStartupHelper.class);

    private static final List<String> GENERATED_SOURCES_PATH_LIST = Lists.newArrayList("target/generated-sources/annotations",
            "target/generated-sources/kapt/compile",
            "build/generated/ksp/main/java",
            "build/generated/sources/annotationProcessor/java/main");

    /**
     * 初始化项目
     *
     * @param project 当前项目
     */
    public static void initializeProject(@NotNull Project project) {
        // 在非EDT线程中执行，避免UI卡顿
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            // 等待索引完成后再执行初始化操作
            com.intellij.openapi.project.DumbService.getInstance(project).runWhenSmart(() -> {
                log.info("索引已完成，开始初始化EasyQuery插件：" + project.getName());
                
                // 1. 预加载配置，避免在需要时出现同步问题
                try {
                    EasyQueryConfigManager.getInstance().getConfig(project);
                } catch (Exception e) {
                    log.warn("Failed to preload configuration for project: " + project.getName(), e);
                }

                // 2. 注册文件编辑器事件监听
                // 必须在EDT线程中执行UI相关操作
                ApplicationManager.getApplication().invokeLater(() -> {
                    registerFileEditorListener(project);
                });

                // 3. 标记生成的源代码根目录 - 在EDT线程中安全执行（在这里调用总是不生效，可能是太早了，现在放到Action里面）
        //        updateGeneratedSourceRoot(project);

                // 4. 在项目启动时运行EasyQuery检查（已在后台线程中）
                runEasyQueryInspection(project);
            });
        });
    }

    /**
     * 更新生成的源代码根目录
     *
     * @param project 当前项目
     */
    public static void updateGeneratedSourceRoot(@NotNull Project project) {
        WriteAction.run(() -> {
            Module[] modules = ReadAction.compute(() -> ModuleManager.getInstance(project).getModules());
            for (Module module : modules) {
                if (!module.isDisposed()) {
                    markGeneratedSourcesRoot(module, GENERATED_SOURCES_PATH_LIST);
                }
            }
        });
    }

    /**
     * 注册文件编辑器事件监听
     *
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

    /**
     * 标记生成的源代码根目录
     *
     * @param module           当前模块
     * @param relativePathList 相对路径列表
     */
    public static void markGeneratedSourcesRoot(@NotNull Module module, List<String> relativePathList) {
        if (module.isDisposed()) {
            return;
        }

        // 使用ReadAction读取模块路径信息
        VirtualFile moduleVirtualFile = ProjectUtil.guessModuleDir(module);
        if (moduleVirtualFile == null) {
            return;
        }

        String modulePath = moduleVirtualFile.getPath();

        // 获取模块目录
        File moduleDir = new File(modulePath);

        boolean needCommit = false;

        for (String GENERATED_SOURCES_PATH : relativePathList) {
            // 构建生成的源代码路径
            File generatedSourcesDir = new File(moduleDir, GENERATED_SOURCES_PATH);
            if (!generatedSourcesDir.exists()) {
                continue;
            }

            // 获取虚拟文件
            VirtualFile generatedSourcesVFile = LocalFileSystem.getInstance().findFileByIoFile(generatedSourcesDir);
            if (generatedSourcesVFile == null) {
                continue;
            }

            // 检查是否已经是源根目录 - 在ReadAction中执行读操作
            boolean alreadySourceRoot = ReadAction.compute(() -> {
                final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
                for (ContentEntry rootContentEntry : moduleRootManager.getContentEntries()) {
                    for (SourceFolder sourceFolder : rootContentEntry.getSourceFolders()) {
                        VirtualFile sourceFolderFile = sourceFolder.getFile();
                        if (sourceFolderFile != null && sourceFolderFile.equals(generatedSourcesVFile)) {
                            return true;
                        }
                    }
                }
                return false;
            });

            // 如果已经是源根目录，则不需要再次标记
            if (alreadySourceRoot) {
                continue;
            }

            // 在写操作中标记为生成的源根目录 - 确保在EDT线程中执行写操作
            ModifiableRootModel model = ModuleRootManager.getInstance(module).getModifiableModel();
            ContentEntry entry = findContentEntry(model, generatedSourcesVFile);
            if (entry != null) {
                SourceFolder[] sourceFolders = entry.getSourceFolders();
                for (SourceFolder sourceFolder : sourceFolders) {
                    if (Comparing.equal(sourceFolder.getFile(), generatedSourcesVFile)) {
                        break;
                    }
                }

                // 添加气泡提醒 目录{}已标记为生成的源根目录
                NotificationUtils.notifySuccess("目录【" + generatedSourcesVFile.getPath() + "】已标记为生成的源根目录", "EasyQuery", module.getProject());

                needCommit = true;
                JavaSourceRootProperties properties = JpsJavaExtensionService.getInstance().createSourceRootProperties("", true);
                entry.addSourceFolder(generatedSourcesVFile, JavaSourceRootType.SOURCE, properties);

                ApplicationManager.getApplication().runWriteAction(model::commit);
            }
        }
        if (needCommit) {
            SaveAndSyncHandler.getInstance().scheduleProjectSave(module.getProject());
        }
    }

    private static void runEasyQueryInspection(Project project) {
        try {
            // 确保在非EDT线程执行
            if (ApplicationManager.getApplication().isDispatchThread()) {
                log.info("在EDT线程中调用runEasyQueryInspection，转移到后台线程执行");
                ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    runEasyQueryInspection(project);
                });
                return;
            }
            
            // 索引已经准备好了（由于外层的DumbService.runWhenSmart），直接执行
            log.info("启动时运行EasyQuery检查：" + project.getName());
            // 使用RunEasyQueryInspectionAction中的方法运行检查
            RunEasyQueryInspectionAction inspectionAction = new RunEasyQueryInspectionAction();
            inspectionAction.runInspectionForProject(project);
        } catch (Exception e) {
            log.warn("运行EasyQuery检查失败", e);
        }
    }
}
