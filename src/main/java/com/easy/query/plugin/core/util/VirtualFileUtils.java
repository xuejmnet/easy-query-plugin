package com.easy.query.plugin.core.util;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class VirtualFileUtils {
    private static Map<String,Map<String, PsiDirectory>> PSI_DIRECTORY_MAP = new ConcurrentHashMap<>();
    private static FileDocumentManager fileDocumentManager = FileDocumentManager.getInstance();

    public static PsiFile getPsiFile(Project project, VirtualFile virtualFile) {
        PsiManager psiManager = PsiManager.getInstance(project);
        return psiManager.findFile(virtualFile);
    }

    public static PsiFile getPsiFile(Project project,Document document) {

        return PsiDocumentManager.getInstance(project).getPsiFile(document);
    }

    public static VirtualFile getVirtualFile(Document document) {
        return fileDocumentManager.getFile(document);

    }

    /**
     * 反式到java文件
     *
     * @param path 路径
     * @return {@code VirtualFile}
     */
    public static VirtualFile transToJavaFile(String path) {
        return LocalFileSystem.getInstance().findFileByPath(path);
    }

    /**
     * 根据路径获取 psi目录
     *
     * @param project 项目
     * @param path    路径
     * @return {@code PsiDirectory}
     */
    public static PsiDirectory psiDirectory(Project project, String path) {
        PsiManager psiManager = PsiManager.getInstance(project);
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
        assert file != null;
        return psiManager.findDirectory(file);
    }


    /**
     * 得到psi目录
     *
     * @param project 项目
     * @param path    路径
     * @return {@code PsiDirectory}
     */
    public static PsiDirectory getPsiDirectory(Project project, String path) {
        PsiManager psiManager = PsiManager.getInstance(project);
        VirtualFile virtualFile = transToJavaFile(path);
        if (ObjectUtil.isNull(virtualFile)) {
            return null;
        }
        return psiManager.findDirectory(virtualFile);
    }

    /**
     * 得到psi目录
     *
     * @param project     项目
     * @param virtualFile 虚拟文件
     * @return {@code PsiDirectory}
     */
    public static PsiDirectory getPsiDirectory(Project project, VirtualFile virtualFile) {
        PsiManager psiManager = PsiManager.getInstance(project);
        return psiManager.findDirectory(virtualFile);
    }


    /**
     * 得到psi目录
     *
     * @param module 模块
     * @param key
     * @return {@code PsiDirectory}
     */
    public static PsiDirectory getPsiDirectory(Project project,Module module, String packageName, String key) {

        Set javaResourceRootTypes = StrUtil.isEmpty(key) ? JavaModuleSourceRootTypes.RESOURCES : JavaModuleSourceRootTypes.SOURCES;
        Map<String, PsiDirectory> psiDirectoryMap = PSI_DIRECTORY_MAP.computeIfAbsent(project.getName(), o -> new ConcurrentHashMap<>());
        PsiDirectory psiDirectory = psiDirectoryMap.get(packageName);
        if (ObjectUtil.isNull(psiDirectory)) {
            AtomicReference<PsiDirectory> subPsiDirectory = new AtomicReference<>();
            WriteCommandAction.runWriteCommandAction(module.getProject(), () -> {
                subPsiDirectory.set(createSubDirectory(module, javaResourceRootTypes, packageName));
            });
            psiDirectory = subPsiDirectory.get();
            psiDirectoryMap.put(packageName, psiDirectory);
        }
        return psiDirectory;
    }

    public static PsiDirectory createSubDirectory(Module module, Set javaResourceRootTypes, String packageName) {
        PsiDirectory targetDirectory = MyModuleUtil.getModuleDirectory(module, javaResourceRootTypes);
        if (targetDirectory != null) {
            String[] directories = packageName.split("\\.");
            for (String directoryName : directories) {
                PsiDirectory subdirectory = targetDirectory.findSubdirectory(directoryName);
                if (subdirectory == null) {
                    subdirectory = targetDirectory.createSubdirectory(directoryName);
                }
                targetDirectory = subdirectory;
            }
        }
        return targetDirectory;
    }

    public static PsiDirectory createSubDirectory(Module module, String packageName) {
        PsiDirectory targetDirectory = getPsiDirectory(module.getProject(), packageName);
        if (ObjectUtil.isNotNull(targetDirectory)) {
            return targetDirectory;
        }
        String path = MyModuleUtil.getPath(module);
        targetDirectory = getPsiDirectory(module.getProject(), path);
        if (targetDirectory != null) {
            String[] directories = StrUtil.subAfter(packageName, path, false).split("/");
            for (String directoryName : directories) {
                AtomicReference<PsiDirectory> subdirectory = new AtomicReference<>(targetDirectory.findSubdirectory(directoryName));
                if (subdirectory.get() == null) {
                    PsiDirectory finalTargetDirectory = targetDirectory;
                    WriteCommandAction.runWriteCommandAction(module.getProject(), () -> {
                        subdirectory.set(finalTargetDirectory.createSubdirectory(directoryName));
                    });
                }
                targetDirectory = subdirectory.get();
            }
        }
        return targetDirectory;
    }

    /**
     * 清空psi目录
     */
    public static void clearPsiDirectoryMap() {
        PSI_DIRECTORY_MAP.clear();
    }
}
