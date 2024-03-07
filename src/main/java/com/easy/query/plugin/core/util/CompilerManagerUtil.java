package com.easy.query.plugin.core.util;

import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

/**
 * create time 2023/11/13 13:07
 * 文件说明
 *
 * @author xuejiaming
 */
public class CompilerManagerUtil {

    /**
     * 编译
     *
     * @param files        文件
     * @param notification 通知
     */
    public static void compile(Project project,VirtualFile[] files, CompileStatusNotification notification) {
        CompilerManager compilerManager = getCompilerManager(project);
        compilerManager.compile(files, notification);
    }

    /**
     * 重新编译
     */
    public static void rebuild(Project project) {
        CompilerManager compilerManager = getCompilerManager(project);
        compilerManager.rebuild(null);
    }

    private static CompilerManager getCompilerManager(Project project) {
        CompilerManager compilerManager = CompilerManager.getInstance(project);
        return compilerManager;
    }

    /**
     * 单独编译某个模块
     *
     * @param module 模块
     */
    public static void make(Project project, Module module) {
        CompilerManager compilerManager = CompilerManager.getInstance(project);
        compilerManager.make(module, null);
    }
}
