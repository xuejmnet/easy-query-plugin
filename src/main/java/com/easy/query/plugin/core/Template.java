package com.easy.query.plugin.core;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.intellij.ide.fileTemplates.impl.UrlUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
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
//    public static @NotNull MybatisFlexConfig getMybatisFlexConfig() {
//        MybatisFlexConfig config = MybatisFlexPluginConfigData.getCurrentProjectMybatisFlexConfig();
//        if (StrUtil.isEmpty(config.getControllerTemplate())) {
//            config.setControllerTemplate(getTemplateContent(MybatisFlexConstant.CONTROLLER_TEMPLATE));
//            config.setModelTemplate(getTemplateContent(MybatisFlexConstant.MODEL_TEMPLATE));
//            config.setInterfaceTempalate(getTemplateContent(MybatisFlexConstant.INTERFACE_TEMPLATE));
//            config.setImplTemplate(getTemplateContent(MybatisFlexConstant.IMPL_TEMPLATE));
//            config.setMapperTemplate(getTemplateContent(MybatisFlexConstant.MAPPER_TEMPLATE));
//            config.setXmlTemplate(getTemplateContent(MybatisFlexConstant.XML_TEMPLATE));
//        }
//
//        if (ObjectUtil.isNull(config.getControllerSuffix())) {
//            config.setControllerSuffix(MybatisFlexConstant.CONTROLLER);
//        }
//        if (ObjectUtil.isNull(config.getInterfaceSuffix())) {
//            config.setInterfaceSuffix(MybatisFlexConstant.SERVICE);
//        }
//        if (ObjectUtil.isNull(config.getImplSuffix())) {
//            config.setImplSuffix(MybatisFlexConstant.SERVICE_IMPL);
//        }
//        if (ObjectUtil.isNull(config.getModelSuffix())) {
//            config.setModelSuffix(MybatisFlexConstant.ENTITY);
//        }
//        if (ObjectUtil.isNull(config.getMapperSuffix())) {
//            config.setMapperSuffix(MybatisFlexConstant.MAPPER);
//        }
//
//        if(ObjectUtil.isNull(config.getContrPath())){
//            config.setContrPath(MybatisFlexConstant.CONTROLLER.toLowerCase());
//        }
//        if(ObjectUtil.isNull(config.getDomainPath())){
//            config.setDomainPath(MybatisFlexConstant.DOMAIN.toLowerCase());
//        }
//        if(ObjectUtil.isNull(config.getImplPath())){
//            config.setImplPath(MybatisFlexConstant.IMPL.toLowerCase());
//        }
//        if(ObjectUtil.isNull(config.getServicePath())){
//            config.setServicePath(MybatisFlexConstant.SERVICE.toLowerCase());
//        }
//        if(ObjectUtil.isNull(config.getMapperPath())){
//            config.setMapperPath(MybatisFlexConstant.MAPPER.toLowerCase());
//        }
//        if(ObjectUtil.isNull(config.getXmlPath())){
//            config.setXmlPath(MybatisFlexConstant.MAPPERS.toLowerCase());
//        }
//        return config;
//    }

    /**
     * 得到模板内容
     *
     * @param templateName 模板名称
     * @return {@code String}
     */
    @NotNull
    public static String getTemplateContent(String templateName) {
        URL resource = Template.class.getResource(StrUtil.format("/templates/{}.vm", templateName));
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
