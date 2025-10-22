package com.easy.query.plugin.core.util;

import cn.hutool.setting.Setting;
import com.easy.query.plugin.config.EasyQueryConfigManager;
import com.easy.query.plugin.config.EasyQueryPluginSetting;
import com.intellij.openapi.project.Project;

/**
 * EasyQuery 配置工具类
 * 
 * @author link2fun
 */
public class EasyQueryConfigUtil {

    /**
     * 获取项目插件配置
     * @param project 项目
     * @return 返回一个可操作的插件配置
     */
    public static EasyQueryPluginSetting getPluginSetting(Project project){
        return new EasyQueryPluginSetting(EasyQueryConfigManager.getInstance().getConfig(project));
    }
    /**
     * 获取布尔类型的项目配置
     * 
     * @param project 项目
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 布尔配置值
     */
    public static Boolean getProjectSettingBool(Project project, String key, Boolean defaultValue) {
        Setting config = EasyQueryConfigManager.getInstance().getConfig(project);
        return config.getBool(key, defaultValue);
    }
    
    /**
     * 获取字符串类型的项目配置
     * 
     * @param project 项目
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 字符串配置值
     */
    public static String getProjectSettingStr(Project project, String key, String defaultValue) {
        Setting config = EasyQueryConfigManager.getInstance().getConfig(project);
        return config.getStr(key, defaultValue);
    }
    
    /**
     * 获取整数类型的项目配置
     * 
     * @param project 项目
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 整数配置值
     */
    public static Integer getProjectSettingInt(Project project, String key, Integer defaultValue) {
        Setting config = EasyQueryConfigManager.getInstance().getConfig(project);
        return config.getInt(key, defaultValue);
    }
    
    /**
     * 获取配置对象
     * 
     * @param project 项目
     * @return 配置对象
     */
    public static Setting getProjectConfig(Project project) {
        return EasyQueryConfigManager.getInstance().getConfig(project);
    }
}
