package com.easy.query.plugin.core.util;

import com.intellij.ide.util.PackageChooserDialog;
import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiPackage;

/**
 * create time 2023/11/30 15:30
 * 文件说明
 *
 * @author xuejiaming
 */
public class PackageUtil {
    /**
     * 选择包路径
     *
     * @param module 模块
     * @return {@code String}
     */
    public static String selectPackage(Module module, String... packagePath) {
        PackageChooserDialog chooser = new PackageChooserDialog("Select Package", module);
        if (packagePath.length > 0) {
            chooser.selectPackage(packagePath[0]);
        }
        // 显示对话框并等待用户选择
        chooser.show();
        PsiPackage selectedPackage = chooser.getSelectedPackage();
        if (ObjectUtil.isNull(selectedPackage)) {
            return packagePath[0];
        }
        return selectedPackage.getQualifiedName();
    }
}
