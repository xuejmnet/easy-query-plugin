package com.easy.query.plugin.core.util;

import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ReflectUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;

import javax.swing.*;
import java.lang.reflect.Method;

/**
 * create time 2025/4/14 09:22
 * 文件说明
 *
 * @author xuejiaming
 */
public class MyRegisterToolWindowTask {
    public static final String TOOL_WINDOW_ID = "EasyQuery Issues";

    public static Object closable(String wid, Icon icon, ToolWindowAnchor position) {
        Class<Object> objectClass = ClassUtil.loadClass("com.intellij.openapi.wm.RegisterToolWindowTask");
        Method closable = ReflectUtil.getMethod(objectClass, "closable", String.class, Icon.class, ToolWindowAnchor.class);
        if (closable != null) {
            return ReflectUtil.invoke(null, closable,
                wid,
                icon, // 使用默认图标
                position // 直接设置锚点
            );
        }
        return null;
    }

    public static ToolWindow registerToolWindow(ToolWindowManager toolWindowManager, Object task) {
        Class<Object> objectClass = ClassUtil.loadClass("com.intellij.openapi.wm.RegisterToolWindowTask");
        Method registerToolWindowMethod = ReflectUtil.getMethod(ToolWindowManager.class, "registerToolWindow", objectClass);
        if (registerToolWindowMethod != null) {
            ToolWindow toolWindow = ReflectUtil.invoke(toolWindowManager, registerToolWindowMethod, task);
            return toolWindow;
        }
        return null;

    }
}
