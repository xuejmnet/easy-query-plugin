package com.easy.query.plugin.action;

import com.easy.query.plugin.config.EasyQueryConfigManager;
import com.easy.query.plugin.core.EasyQueryDocumentChangeHandler;
import com.easy.query.plugin.core.startup.ProjectStartupHelper;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


public class CompileAllAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        // 使用后台任务执行编译
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "EasyQuery 全量编译", true) {
            private int totalFiles = 0;
            private int processedFiles = 0;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);
                
                // 0. 首先等待索引准备好，避免 createAptFile 内部触发 runWhenSmart 回调
                if (DumbService.getInstance(project).isDumb()) {
                    indicator.setText("正在等待索引准备...");
                    DumbService.getInstance(project).waitForSmartMode();
                }

                // 1. 获取所有需要编译的类 (需要在 ReadAction 中执行)
                indicator.setText("正在扫描需要编译的实体类...");
                Collection<PsiClass> annotationPsiProxyClass = ReadAction.compute(() -> 
                    PsiJavaFileUtil.getAnnotationPsiClass(project, "com.easy.query.core.annotation.EntityProxy"));
                Collection<PsiClass> annotationPsiFileProxyClass = ReadAction.compute(() -> 
                    PsiJavaFileUtil.getAnnotationPsiClass(project, "com.easy.query.core.annotation.EntityFileProxy"));
                
                List<PsiClass> annotationPsiClass = new ArrayList<>();
                annotationPsiClass.addAll(annotationPsiProxyClass);
                annotationPsiClass.addAll(annotationPsiFileProxyClass);
                
                totalFiles = annotationPsiClass.size();
                
                if (totalFiles == 0) {
                    return;
                }

                // 2. 分批处理文件，每批处理一部分，更新进度
                List<VirtualFile> virtualFiles = new ArrayList<>();
                int batchSize = 10;
                int batchCount = 0;
                
                for (PsiClass psiClass : annotationPsiClass) {
                    if (indicator.isCanceled()) {
                        break;
                    }
                    
                    processedFiles++;
                    indicator.setFraction((double) processedFiles / totalFiles);
                    
                    // 在 ReadAction 中获取 VirtualFile
                    final PsiClass currentPsiClass = psiClass;
                    final int currentProcessed = processedFiles;
                    VirtualFile virtualFile = ReadAction.compute(() -> {
                        String className = currentPsiClass.getName();
                        // 主标题显示总体进度：已处理/总数
                        indicator.setText(String.format("EasyQuery 全量编译 (%d/%d)", 
                            currentProcessed, totalFiles));
                        // 副标题显示当前处理的类名
                        indicator.setText2("正在处理: " + className);
                        return currentPsiClass.getContainingFile().getVirtualFile();
                    });
                    
                    if (virtualFile != null) {
                        virtualFile.putUserData(EasyQueryDocumentChangeHandler.CHANGE, true);
                        virtualFiles.add(virtualFile);
                        batchCount++;
                    }

                    // 每收集 batchSize 个文件就处理一次
                    if (batchCount >= batchSize) {
                        processBatch(virtualFiles, project);
                        virtualFiles.clear();
                        batchCount = 0;
                    }
                }

                // 处理剩余的文件
                if (!virtualFiles.isEmpty() && !indicator.isCanceled()) {
                    processBatch(virtualFiles, project);
                }

                // 3. 等待所有后台任务完成（createAptFile 内部使用了 DumbService.runWhenSmart）
                if (!indicator.isCanceled()) {
                    indicator.setText(String.format("EasyQuery 全量编译 (%d/%d) - 等待处理完成...", 
                        processedFiles, totalFiles));
                    indicator.setText2("");
                    // 确保所有后台任务完成
                    DumbService.getInstance(project).waitForSmartMode();
                }
                
                // 4. 更新 generated sources root
                if (!indicator.isCanceled()) {
                    indicator.setText(String.format("EasyQuery 全量编译 (%d/%d) - 正在更新源代码根目录...", 
                        processedFiles, totalFiles));
                    ProjectStartupHelper.updateGeneratedSourceRoot(project);
                }

                // 5. 清除缓存
                if (!indicator.isCanceled()) {
                    indicator.setText(String.format("EasyQuery 全量编译 (%d/%d) - 正在清除缓存...", 
                        processedFiles, totalFiles));
                    EasyQueryConfigManager.invalidateProjectCache(project);
                }
            }

            private void processBatch(List<VirtualFile> virtualFiles, Project project) {
                if (virtualFiles.isEmpty()) {
                    return;
                }
                // createAptFile 内部需要 Read Access
                ReadAction.run(() -> {
                    EasyQueryDocumentChangeHandler.createAptFile(
                        new ArrayList<>(virtualFiles), project, true);
                });
            }

            @Override
            public void onSuccess() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) {
                        return;
                    }
                    NotificationUtils.notifySuccess(
                        String.format("编译完成，共处理 %d 个实体类", processedFiles), 
                        "EasyQuery", 
                        project);
                });
            }

            @Override
            public void onCancel() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) {
                        return;
                    }
                    NotificationUtils.notifySuccess(
                        String.format("编译已取消，已处理 %d/%d 个实体类", processedFiles, totalFiles), 
                        "EasyQuery", 
                        project);
                });
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {

            }
        });
    }
}
