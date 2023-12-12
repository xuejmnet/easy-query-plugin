package com.easy.query.plugin.core.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * create time 2023/12/12 16:54
 * 文件说明
 *
 * @author xuejiaming
 */
public class TableMetadata {
    /**
     * 表名
     */
    private final String name;

    /**
     * 表注释
     */
    private final String comment;
    /**
     * 列
     */
    private final List<ColumnMetadata> columns;

    public TableMetadata(String name,String comment){

        this.name = name;
        this.comment = comment;
        this.columns = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getComment() {
        return comment;
    }

    public List<ColumnMetadata> getColumns() {
        return columns;
    }
}
