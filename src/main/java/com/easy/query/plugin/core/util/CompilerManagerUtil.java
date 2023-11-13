package com.easy.query.plugin.core.util;

import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
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
    public static void compile(VirtualFile[] files, CompileStatusNotification notification) {
        CompilerManager compilerManager = getCompilerManager();
        compilerManager.compile(files, notification);
    }

    /**
     * 重新编译
     */
    public static void rebuild() {
        CompilerManager compilerManager = getCompilerManager();
        compilerManager.rebuild(null);
    }

    private static CompilerManager getCompilerManager() {
        CompilerManager compilerManager = CompilerManager.getInstance(ProjectUtils.getCurrentProject());
        return compilerManager;
    }

    /**
     * 单独编译某个模块
     *
     * @param module 模块
     */
    public static void make(Module module) {
        CompilerManager compilerManager = CompilerManager.getInstance(ProjectUtils.getCurrentProject());
        compilerManager.make(module, null);
    }
}
