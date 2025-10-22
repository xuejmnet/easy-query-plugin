package com.easy.query.plugin.config;

import cn.hutool.setting.Setting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * create time 2025/10/22 21:33
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyQueryPluginSetting {
    private final Setting setting;

    public EasyQueryPluginSetting(Setting setting) {
        this.setting = setting;
    }

    /**
     * 获取数据库格式化类型
     * @return
     */
    public @Nullable String getSQLFormatOrNull() {
        return setting.getStr(EasyQueryProjectSettingKey.SQL_FORMAT_TYPE);
    }

    /**
     * 获取数据库格式化类型 或默认值
     * @param defaultValue
     * @return
     */
    public @NotNull String getSQLFormatOrDefault(@NotNull String defaultValue) {
        return setting.getStr(EasyQueryProjectSettingKey.SQL_FORMAT_TYPE, defaultValue);
    }
}
