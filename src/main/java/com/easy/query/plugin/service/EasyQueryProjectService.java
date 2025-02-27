package com.easy.query.plugin.service;

import com.easy.query.plugin.config.EasyQueryConfigManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

/**
 * EasyQuery 项目服务
 * 在项目打开时初始化并加载配置
 */
@Service
public final class EasyQueryProjectService {
    private static final Logger LOG = Logger.getInstance(EasyQueryProjectService.class);
    private final Project project;
    
    public EasyQueryProjectService(Project project) {
        this.project = project;
        LOG.info("EasyQuery 项目服务初始化: " + project.getName());
        // 初始化时加载项目配置
        EasyQueryConfigManager.getInstance().getConfig(project);
    }
}
