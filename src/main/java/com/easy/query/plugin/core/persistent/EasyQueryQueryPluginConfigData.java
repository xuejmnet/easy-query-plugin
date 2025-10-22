package com.easy.query.plugin.core.persistent;

import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Objects;

/**
 * create time 2023/11/30 11:19
 * 文件说明
 *
 * @author xuejiaming
 */
@Service
@State(
    name = "PluginEQSettings",
    storages = {
        @Storage("pluginEQSettings.xml")
    }
)
public final class EasyQueryQueryPluginConfigData implements PersistentStateComponent<EasyQueryQueryPluginConfigData.State> {


    @Override
    public @Nullable EasyQueryQueryPluginConfigData.State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.myState = state;
    }


    public static EasyQueryQueryPluginConfigData getInstance() {
        ComponentManager componentManager = ApplicationManager.getApplication();
        return componentManager.getService(EasyQueryQueryPluginConfigData.class);
    }


    /**
     * 获取当前项目mybatis flex配置
     *
     * @return {@code EasyQueryConfig}
     */
//    public static EasyQueryConfig getCurrentProjectEasyQueryConfig(Project project) {
//        EasyQueryQueryPluginConfigData instance = getInstance();
//        State state = instance.getState();
//        Map<String, EasyQueryConfig> eqConfigMap = JSONObject.parseObject(state.easyQueryConfig, new TypeReference<Map<String, EasyQueryConfig>>() {
//        });
//        return eqConfigMap.getOrDefault(project.getName(), new EasyQueryConfig());
//    }


    private static final String QUICK_TIP_SETTING_KEY = "QUICK_TIP_SETTING_KEY";
    private static final String STRUCT_DTO_IGNORE_COLUMNS_KEY = "STRUCT_DTO_IGNORE_COLUMNS_KEY";


    public static boolean isHiddenConfigKey(String configKey) {
        return Objects.equals(QUICK_TIP_SETTING_KEY, configKey)
            || Objects.equals(STRUCT_DTO_IGNORE_COLUMNS_KEY, configKey);
    }

    public static LinkedHashMap<String, EasyQueryConfig> getProjectSinceMap() {
        EasyQueryQueryPluginConfigData instance = getInstance();
        State state = instance.getState();
        String configSince = state.configSince;
        return JSONObject.parseObject(configSince, new TypeReference<LinkedHashMap<String, EasyQueryConfig>>() {
        });
    }

//    /**
//     * 设置easy-query配置
//     *
//     * @param config 配置
//     */
//    public static void setCurrentEasyQueryConfig(EasyQueryConfig config,Project project) {
//        Map<String, EasyQueryConfig> configMap = getProjectEasyQueryConfig();
//        configMap.put(project.getName(), config);
//        EasyQueryQueryPluginConfigData instance = getInstance();
//        State state = instance.getState();
//        state.easyQueryConfig = JSONObject.toJSONString(configMap);
//        instance.loadState(state);
//    }

//    public static Map<String, EasyQueryConfig> getProjectEasyQueryConfig() {
//        EasyQueryQueryPluginConfigData instance = getInstance();
//        State state = instance.getState();
//        return JSONObject.parseObject(state.easyQueryConfig, new TypeReference<Map<String, EasyQueryConfig>>() {
//        });
//    }

    public static void saveConfigSince(String key, EasyQueryConfig config) {
        EasyQueryQueryPluginConfigData instance = getInstance();
        State state = instance.getState();
        LinkedHashMap<String, EasyQueryConfig> projectSinceMap = getProjectSinceMap();
        projectSinceMap.put(key, config);
        state.configSince = JSONObject.toJSONString(projectSinceMap);
        instance.loadState(state);
    }

    public static void delConfigSince(String key) {
        EasyQueryQueryPluginConfigData instance = getInstance();
        State state = instance.getState();
        LinkedHashMap<String, EasyQueryConfig> projectSinceMap = getProjectSinceMap();
        projectSinceMap.remove(key);
        state.configSince = JSONObject.toJSONString(projectSinceMap);
        instance.loadState(state);
    }

    public static void export(String targetPath) {
        LinkedHashMap<String, EasyQueryConfig> projectSinceMap = getProjectSinceMap();
        FileUtil.writeString(JSONObject.toJSONString(projectSinceMap), new File(targetPath + File.separator + "EasyQueryData.json"), "UTF-8");
        Messages.showDialog("导出成功，请到选择的目录查看", "提示", new String[]{"确定"}, -1, Messages.getInformationIcon());
    }

    public static void importConfig(String path) {
        File file = new File(path);
        if (!file.exists()) {
            Messages.showDialog("文件不存在", "提示", new String[]{"确定"}, -1, Messages.getInformationIcon());
            return;
        }
        String content = FileUtil.readString(file, StandardCharsets.UTF_8);
        LinkedHashMap<String, EasyQueryConfig> config = JSONObject.parseObject(content, new TypeReference<LinkedHashMap<String, EasyQueryConfig>>() {
        });

        setCurrentConfig(config);

        Messages.showDialog("导入成功", "提示", new String[]{"确定"}, -1, Messages.getInformationIcon());
    }

    /**
     * 设置mybatis flex配置
     *
     * @param config 配置
     */
    public static void setCurrentConfig(LinkedHashMap<String, EasyQueryConfig> config) {
        EasyQueryQueryPluginConfigData instance = getInstance();
        State state = instance.getState();
        state.configSince = JSONObject.toJSONString(config);
        instance.loadState(state);
    }

    public static class State {
//        /**
//         * 当前项目配置（项目隔离）
//         */
//        public String easyQueryConfig = "{}";
        /**
         * 生成配置（项目隔离）
         */
        public String configSince = "{}";
        /**
         * 列类型和字段类型映射（通用）
         */
        public String columnFieldMap = "{}";

        public String typeMappings = "{}";

    }

    private State myState = new State();


}
