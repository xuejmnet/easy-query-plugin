package com.easy.query.plugin.core;

import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.constant.EasyQueryConstant;
import com.easy.query.plugin.core.persistent.EasyQueryQueryPluginConfigData;
import com.easy.query.plugin.core.util.ObjectUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.ide.fileTemplates.impl.UrlUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class Template {
    private static Set<String> set = new HashSet<>();

//    /**
//     * 得到控制器vm代码
//     *
//     * @return {@code String}
//     */
//    public static String getVmCode(String template) {
//        String code = getConfigData(template.split("\\.")[0]);
//        if (StrUtil.isBlank(code)) {
//            code = getTemplateContent(template);
//        }
//        return code;
//    }
//
    public static @NotNull EasyQueryConfig getEasyQueryConfig(Project project,String sinceName) {
        LinkedHashMap<String, EasyQueryConfig> projectSinceMap = EasyQueryQueryPluginConfigData.getProjectSinceMap();
        EasyQueryConfig config = projectSinceMap.get(sinceName);
        if(config==null){
            config=new EasyQueryConfig();
            config.setModelTemplate(getTemplateContent(EasyQueryConstant.MODEL_TEMPLATE));
            config.setModelSuffix(EasyQueryConstant.ENTITY);
            config.setModelPackage(EasyQueryConstant.DOMAIN.toLowerCase());
        }

        return config;
    }

    /**
     * 得到模板内容
     *
     * @param templateName 模板名称
     * @return {@code String}
     */
    @NotNull
    public static String getTemplateContent(String templateName) {
        URL resource = Template.class.getResource("/templates/"+templateName+".vm");
        String templateContent = null;
        try {
            templateContent = StringUtil.convertLineSeparators(UrlUtil.loadText(resource));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return templateContent;
    }

//
//    public static String getConfigData(String property) {
//        MybatisFlexConfig config = getMybatisFlexConfig();
//        Object fieldValue = ReflectUtil.getFieldValue(config, property);
//        return ObjectUtil.defaultIfNull(fieldValue, "").toString();
//    }
//
//    public static String getSuffix(String property) {
//        return getConfigData(property);
//    }
//
//    public static boolean getCheckBoxConfig(String property) {
//        MybatisFlexConfig config = getMybatisFlexConfig();
//        Object fieldValue = ReflectUtil.getFieldValue(config, property);
//        return (boolean) ObjectUtil.defaultIfNull(fieldValue, false);
//    }
//    public static Boolean getCheckBoxConfig(String property,boolean defaultValue) {
//        MybatisFlexConfig config = getMybatisFlexConfig();
//        Object fieldValue = ReflectUtil.getFieldValue(config, property);
//        return (boolean) ObjectUtil.defaultIfNull(fieldValue, defaultValue);
//    }
//
//    public static String getTablePrefix() {
//        return getConfigData(MybatisFlexConstant.TABLE_PREFIX);
//    }
//
//    public static String getSince() {
//        return getConfigData(MybatisFlexConstant.SINCE);
//    }
//
//    public static String getAuthor() {
//        return getConfigData(MybatisFlexConstant.AUTHOR);
//    }
//
//    public static void clear() {
//        set.clear();
//    }
}
