package com.easy.query.plugin.config;

import cn.hutool.setting.Setting;
import com.alibaba.fastjson2.JSON;
import com.easy.query.plugin.action.PreviewConsoleLogSQLAction;
import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.config.NamedEasyQueryConfig;
import com.easy.query.plugin.core.util.EasyQueryConfigUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * create time 2025/10/22 21:33
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyQueryPluginSetting {
    private static final Logger LOG = Logger.getInstance(EasyQueryPluginSetting.class);
    private final Setting setting;

    public EasyQueryPluginSetting(Setting setting) {
        this.setting = setting;
    }

    /**
     * 获取数据库格式化类型
     *
     * @return
     */
    public @Nullable String getSQLFormatOrNull() {
        return setting.getStr(EasyQueryProjectSettingKey.SQL_FORMAT_TYPE);
    }

    /**
     * 获取数据库格式化类型 或默认值
     *
     * @param defaultValue
     * @return
     */
    public @NotNull String getSQLFormatOrDefault(@NotNull String defaultValue) {
        return setting.getStr(EasyQueryProjectSettingKey.SQL_FORMAT_TYPE, defaultValue);
    }

    public @Nullable NamedEasyQueryConfig getTableGenerateConfig() {
        String generateValue = setting.getStr(EasyQueryProjectSettingKey.SQL_GENERATE, null);
        if (StrUtil.isNotBlank(generateValue)) {
            try {
                byte[] decode = Base64.getDecoder().decode(generateValue.getBytes(StandardCharsets.UTF_8));
                String configJson = new String(decode, StandardCharsets.UTF_8);
                EasyQueryConfig easyQueryConfig = JSON.parseObject(configJson, EasyQueryConfig.class);
                return new NamedEasyQueryConfig("【使用当前项目配置】", easyQueryConfig, false);
            } catch (Exception e) {
                Messages.showWarningDialog("找不到名称为：【使用当前项目配置】的配置", "提示");
            }
        }
        return null;
    }

    public void saveTableGenerateConfig(@NotNull EasyQueryConfig configData, @NotNull Project project) {
        String jsonString = JSON.toJSONString(configData);
        if (jsonString == null) {
            Messages.showWarningDialog("序列化失败无法序列化当前配置信息", "提示");
            return;
        }
        byte[] encode = Base64.getEncoder().encode(jsonString.getBytes(StandardCharsets.UTF_8));
        String value = new String(encode, StandardCharsets.UTF_8);
        saveConfig(EasyQueryProjectSettingKey.SQL_GENERATE, value, project);
    }

    private void saveConfig(@NotNull String key, @NotNull String value, @NotNull Project project) {
        try {
            setting.put(key, value);
            setting.store();
            NotificationUtils.notifySuccess("配置保存成功", project);
        } catch (Exception ex) {
            LOG.error(ex);
            Messages.showWarningDialog("配置保存失败", "提示");
        }
    }

    public @NotNull String getLambdaTip() {
        return setting.getStr(EasyQueryProjectSettingKey.LAMBDA_TIP, "");
    }

    public void saveLambdaTip(String lambdaTip, Project project) {
        saveConfig(EasyQueryProjectSettingKey.SQL_GENERATE, lambdaTip == null ? "" : lambdaTip, project);
    }

    public @NotNull String getDTOColumnsIgnore() {
        String str = setting.getStr(EasyQueryProjectSettingKey.DTO_IGNORE_COLUMNS, null);
        if (str == null) {
            return "";
        }
        try {
            byte[] decode = Base64.getDecoder().decode(str.getBytes(StandardCharsets.UTF_8));
            return new String(decode, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }

    public void saveDTOColumnsIgnore(String value, Project project) {
        String json = value == null ? "" : value;
        byte[] encode = Base64.getEncoder().encode(json.getBytes(StandardCharsets.UTF_8));
        String base64 = new String(encode, StandardCharsets.UTF_8);
        saveConfig(EasyQueryProjectSettingKey.DTO_IGNORE_COLUMNS, base64, project);
    }
}
