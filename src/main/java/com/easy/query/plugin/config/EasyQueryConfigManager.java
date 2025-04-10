package com.easy.query.plugin.config;

import cn.hutool.setting.Setting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * EasyQuery 配置管理器
 * 负责读取、解析、缓存配置文件，并监听文件变化
 * @author link2fun
 */
public class EasyQueryConfigManager {

    private static final Logger log = LoggerFactory.getLogger(EasyQueryConfigManager.class);

    /** 配置文件名 */
    private static final String CONFIG_FILE_NAME = "easy-query.setting";
    /** 默认配置模板路径 */
    private static final String DEFAULT_CONFIG_TEMPLATE = "/templates/default-easyqueryconfig.template";
    /** easy-query 列注解类名 */
    private static final String EASY_QUERY_COLUMN_CLASS = "com.easy.query.core.annotation.Column";
    
    /** 配置文件缓存 */
    private final Map<Project, Setting> configCache = new ConcurrentHashMap<>();
    /** 项目是否使用 easy-query 的检查结果缓存 */
    private static final Map<Project, Boolean> projectUsingEasyQuery = new ConcurrentHashMap<>();
    
    /** 单例实例 */
    private static EasyQueryConfigManager instance;
    
    /** 私有构造函数，防止外部实例化 */
    private EasyQueryConfigManager() {
        // 初始化配置文件监听器
        setupFileListener();
    }
    
    public static synchronized EasyQueryConfigManager getInstance() {
        if (instance == null) {
            instance = new EasyQueryConfigManager();
        }
        return instance;
    }
    
    /** 获取项目配置 */
    public Setting getConfig(Project project) {
        return configCache.computeIfAbsent(project, this::loadConfig);
    }

    /** 加载项目配置 */
    public Setting loadConfig(Project project) {
        try {
            String projectBasePath = project.getBasePath();
            if (projectBasePath == null) {
                log.warn("项目基础路径为空");
                return Setting.create();
            }
            
            Path configFilePath = Paths.get(projectBasePath, CONFIG_FILE_NAME);
            if (!Files.exists(configFilePath)) {
                // 检查项目是否使用 easy-query
                if (EasyQueryConfigManager.isProjectUsingEasyQuery(project)) {
                    log.info("检测到项目使用 easy-query，创建默认配置: " + configFilePath);
                    createDefaultConfig(configFilePath);
                } else {
                    log.info("项目未使用 easy-query，跳过配置文件创建");
                    return Setting.create();
                }
            }
            
            // 如果配置文件存在或已创建，继续加载
            if (Files.exists(configFilePath)) {
                Setting setting = new Setting(configFilePath.toFile().getAbsolutePath());
                log.info("配置加载成功: " + configFilePath);
                return setting;
            }
            
            return Setting.create();
        } catch (IOException e) {
            log.error("读取配置文件时出错", e);
            return Setting.create();
        }
    }
    
    /**
     * 检查项目是否使用了 easy-query
     * @param project 当前项目
     * @return 是否使用了 easy-query
     */
    public static boolean isProjectUsingEasyQuery(Project project) {
        return projectUsingEasyQuery.computeIfAbsent(project, p -> {
            try {
                // 通过查找关键类来判断是否使用了 easy-query
                PsiClass columnClass = com.intellij.openapi.application.ReadAction.compute(() -> JavaPsiFacade.getInstance(project)
                        .findClass(EASY_QUERY_COLUMN_CLASS, GlobalSearchScope.allScope(project)));
                boolean result = columnClass != null;
                log.info("项目 {} 使用 easy-query: {}", project.getName(), result);
                return result;
            } catch (Exception e) {
                log.warn("检查项目依赖时出错", e);
                return false;
            }
        });
    }
    
    /**
     * 清除指定项目的 EasyQuery 使用状态缓存。
     * 当检测到项目依赖发生变化时（例如通过 ProjectDependencyChangeListener），应调用此方法。
     * @param project 需要清除缓存的项目
     */
    public static void invalidateProjectCache(Project project) {
        if (project != null) {
            projectUsingEasyQuery.remove(project);
            log.info("EasyQuery usage cache invalidated for project: {}", project.getName());
        }
    }
    
    /**
     * 创建默认配置文件
     * @param configFilePath 配置文件路径
     * @throws IOException 创建文件失败时抛出异常
     */
    private void createDefaultConfig(Path configFilePath) throws IOException {
        String defaultContent = generateDefaultConfig();
        
        // 确保父目录存在
        File parentDir = configFilePath.getParent().toFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // 写入默认配置
        try (FileWriter writer = new FileWriter(configFilePath.toFile())) {
            writer.write(defaultContent);
        }
        
        // 使用异步刷新文件系统，避免在读锁下进行同步刷新导致的死锁
        ApplicationManager.getApplication().invokeLater(() -> {
            LocalFileSystem.getInstance().refreshIoFiles(
                java.util.Collections.singletonList(configFilePath.toFile()),
                true, 
                false, 
                null
            );
        });
    }
    
    /**
     * 从模板文件加载默认配置内容
     * @return 默认配置字符串
     */
    private String generateDefaultConfig() {
        try (InputStream inputStream = getClass().getResourceAsStream(DEFAULT_CONFIG_TEMPLATE)) {
            if (inputStream == null) {
                log.warn("无法找到默认配置模板文件，使用硬编码的默认值");
                return getFallbackDefaultConfig();
            }
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            log.warn("读取默认配置模板失败，使用硬编码的默认值", e);
            return getFallbackDefaultConfig();
        }
    }
    
    /**
     * 获取备用的默认配置（当无法加载模板文件时使用）
     * @return 备用的默认配置内容
     */
    private String getFallbackDefaultConfig() {
        return "# EasyQuery 默认配置文件\n"
                + "# 在此处添加您的自定义配置\n\n"
                + "# 示例配置项\n"
                + "# property1=value1\n"
                + "# property2=value2\n";
    }

    
    private void setupFileListener() {
        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
            @Override
            public void contentsChanged(VirtualFileEvent event) {
                checkConfigFileChange(event.getFile());
            }
            
            @Override
            public void fileCreated(VirtualFileEvent event) {
                checkConfigFileChange(event.getFile());
            }
            
            private void checkConfigFileChange(VirtualFile file) {
                if (CONFIG_FILE_NAME.equals(file.getName())) {
                    String filePath = file.getPath();
                    log.info("检测到配置文件变化: {}", filePath);
                    
                    for (Project project : configCache.keySet()) {
                        String projectPath = project.getBasePath();
                        if (projectPath != null && filePath.startsWith(projectPath)) {
                            Setting setting = loadConfig(project);
                            configCache.put(project, setting);
                            log.info("已更新项目配置: {}", projectPath);
                            break;
                        }
                    }
                }
            }
        });
    }
}
