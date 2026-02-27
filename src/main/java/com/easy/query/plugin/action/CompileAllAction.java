package com.easy.query.plugin.action;

import com.easy.query.plugin.config.EasyQueryConfigManager;
import com.easy.query.plugin.core.EasyQueryDocumentChangeHandler;
import com.easy.query.plugin.core.startup.ProjectStartupHelper;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
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

                // 1. 获取所有需要编译的类
                indicator.setText("正在扫描需要编译的实体类...");
                Collection<PsiClass> annotationPsiProxyClass = PsiJavaFileUtil.getAnnotationPsiClass(project, 
                    "com.easy.query.core.annotation.EntityProxy");
                Collection<PsiClass> annotationPsiFileProxyClass = PsiJavaFileUtil.getAnnotationPsiClass(project, 
                    "com.easy.query.core.annotation.EntityFileProxy");
                
                List<PsiClass> annotationPsiClass = new ArrayList<>();
                annotationPsiClass.addAll(annotationPsiProxyClass);
                annotationPsiClass.addAll(annotationPsiFileProxyClass);
                
                totalFiles = annotationPsiClass.size();
                
                if (totalFiles == 0) {
                    return;
                }

                indicator.setText(String.format("找到 %d 个需要编译的实体类", totalFiles));

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
                    indicator.setText2(String.format("正在处理: %s (%d/%d)", 
                        psiClass.getName(), processedFiles, totalFiles));

                    VirtualFile virtualFile = psiClass.getContainingFile()
                            .getVirtualFile();
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

                // 3. 更新 generated sources root
                if (!indicator.isCanceled()) {
                    indicator.setText("正在更新生成的源代码根目录...");
                    ProjectStartupHelper.updateGeneratedSourceRoot(project);
                }

                // 4. 清除缓存
                if (!indicator.isCanceled()) {
                    indicator.setText("正在清除缓存...");
                    EasyQueryConfigManager.invalidateProjectCache(project);
                }
            }

            private void processBatch(List<VirtualFile> virtualFiles, Project project) {
                if (virtualFiles.isEmpty()) {
                    return;
                }
                EasyQueryDocumentChangeHandler.createAptFile(
                    new ArrayList<>(virtualFiles), project, true);
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
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) {
                        return;
                    }
                    NotificationUtils.notifyError(
                        "编译过程中发生错误: " + error.getMessage(), 
                        "EasyQuery", 
                        project);
                });
            }
        });
    }
}
