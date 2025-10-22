package com.easy.query.plugin.core.config;

/**
 * create time 2025/10/22 22:23
 * 文件说明
 *
 * @author xuejiaming
 */
public class NamedEasyQueryConfig {
    private final String name;
    private final EasyQueryConfig easyQueryConfig;
    private final boolean supportModify;

    public NamedEasyQueryConfig(String name, EasyQueryConfig easyQueryConfig,boolean supportModify){
        this.name = name;
        this.easyQueryConfig = easyQueryConfig;
        this.supportModify = supportModify;
    }

    public String getName() {
        return name;
    }

    public EasyQueryConfig getEasyQueryConfig() {
        return easyQueryConfig;
    }

    public boolean isSupportModify() {
        return supportModify;
    }
}
