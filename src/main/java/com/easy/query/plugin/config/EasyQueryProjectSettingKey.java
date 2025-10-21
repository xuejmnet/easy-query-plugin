package com.easy.query.plugin.config;

/**
 * 项目配置KEY 常量
 */
public interface EasyQueryProjectSettingKey {

    /** DTO是否保留 @Column 注解, 当字段映射为属性优先的时候可以不用保留 Column注解 */
    String DTO_KEEP_ANNO_COLUMN = "dto.keepAnnoColumn";
    /**
     * 程序启动时是否扫描检查
     */
    String STARTUP_RUN_INSPECTION = "startup.runInspection";
    /**
     * 数据库格式化类型
     */
    String SQL_FORMAT_TYPE = "sql.format";
    /**
     * 数据库生成实体配置
     */
    String SQL_GENERATE = "sql.generate";
}
