package com.easy.query.plugin.core.entity;

import com.intellij.database.util.JdbcUtil;

/**
 * create time 2023/12/12 16:54
 * 文件说明
 *
 * @author xuejiaming
 */
public class ColumnMetadata {
    private final String name;
    private final String jdbcTypeStr;
    private final int jdbcType;
    private final String jdbcTypeName;
    private final boolean notNull;
    private final String comment;
    private final boolean primary;
    private final boolean autoIncrement;
    private final int size;

    public ColumnMetadata(String name,String jdbcTypeStr,int jdbcType,boolean notNull,String comment,boolean primary,boolean autoIncrement,int size){

        this.name = name;
        this.jdbcTypeStr = jdbcTypeStr;
        this.jdbcType = jdbcType;
        this.jdbcTypeName = JdbcUtil.getJdbcTypeName(jdbcType);
        this.notNull = notNull;
        this.comment = comment;
        this.primary = primary;
        this.autoIncrement = autoIncrement;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public String getJdbcTypeStr() {
        return jdbcTypeStr;
    }

    public int getJdbcType() {
        return jdbcType;
    }

    public String getJdbcTypeName() {
        return jdbcTypeName;
    }

    public boolean isNotNull() {
        return notNull;
    }

    public String getComment() {
        return comment;
    }

    public boolean isPrimary() {
        return primary;
    }

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public int getSize() {
        return size;
    }
}
