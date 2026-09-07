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
     * 按绝对路径解析 psi 目录，缺失时逐级创建（须在 EDT 调用，内部为写命令）。
     * 供写入阶段使用：生成阶段只记录目标路径字符串，不再在后台线程触达目录写入。
     *
     * @return 目标目录；路径无法定位到任何已存在祖先时返回 null
     */
    public static PsiDirectory ensurePsiDirectory(Project project, String path) {
        PsiDirectory target = getPsiDirectory(project, path);
        if (ObjectUtil.isNotNull(target)) {
            return target;
        }
        // 自底向上找最深已存在祖先，记录缺失的目录名后逐级创建
        java.util.LinkedList<String> missing = new java.util.LinkedList<>();
        String cursor = path;
        while (StrUtil.isNotEmpty(cursor)) {
            PsiDirectory dir = getPsiDirectory(project, cursor);
            if (dir != null) {
                break;
            }
            int idx = Math.max(cursor.lastIndexOf('/'), cursor.lastIndexOf('\\'));
            if (idx <= 0) {
                missing.clear();
                break;
            }
            missing.addFirst(cursor.substring(idx + 1));
            cursor = cursor.substring(0, idx);
        }
        PsiDirectory parent = getPsiDirectory(project, cursor);
        if (parent == null) {
            return null;
        }
        for (String name : missing) {
            PsiDirectory sub = parent.findSubdirectory(name);
            if (sub == null) {
                PsiDirectory parentDir = parent;
                AtomicReference<PsiDirectory> created = new AtomicReference<>();
                WriteCommandAction.runWriteCommandAction(project, () -> created.set(parentDir.createSubdirectory(name)));
                sub = created.get();
            }
            if (sub == null) {
                return null;
            }
            parent = sub;
        }
        return parent;
    }

    /**
     * 清空psi目录
     */
    public static void clearPsiDirectoryMap() {
        PSI_DIRECTORY_MAP.clear();
    }
}
