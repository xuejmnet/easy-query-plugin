package com.easy.query.plugin.core.util;

import javax.swing.*;
import java.awt.*;

public class DialogUtil {

    /**
     * dialog 居中显示
     *
     * @param dialog
     */
    public static void centerShow(JDialog dialog) {
        // 获取屏幕的宽度和高度
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int screenWidth = screenSize.width;
        int screenHeight = screenSize.height;

        // 计算对话框的位置
        int dialogWidth = dialog.getWidth();
        int dialogHeight = dialog.getHeight();

        int dialogX = (screenWidth - dialogWidth) / 2;
        int dialogY = (screenHeight - dialogHeight) / 2;
        // 设置对话框的位置
        dialog.setLocation(dialogX, dialogY);
    }
}
