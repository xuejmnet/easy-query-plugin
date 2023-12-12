package com.easy.query.plugin.core.entity;

import org.apache.commons.collections.CollectionUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * create time 2023/11/28 22:40
 * 文件说明
 *
 * @author xuejiaming
 */
public class TableInfo {
    /**
     * 表名
     */
    private String name;

    /**
     * 表注释
     */
    private String comment;

    /**
     * 列信息集合
     */
    private List<ColumnInfo> columnList;

    /**
     * 导入的类集合
     */
    private Set<String> importClassList;
    private String superClass;

    public Set<String> getImportClassList() {
        return importClassList;
    }

    public void addImportClassItem(String importClassItem) {
        if (CollectionUtils.isEmpty(importClassList)) {
            importClassList = new HashSet<>();
        }
        this.importClassList.add(importClassItem);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<ColumnInfo> getColumnList() {
        return columnList;
    }

    public void setColumnList(List<ColumnInfo> columnList) {
        this.columnList = columnList;
    }

    public void setImportClassList(Set<String> importClassList) {
        this.importClassList = importClassList;
    }

    public String getSuperClass() {
        return superClass;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    @Override
    public String toString() {
        return "TableInfo{" +
                "name='" + name + '\'' +
                ", comment='" + comment + '\'' +
                ", columnList=" + columnList +
                ", importClassList=" + importClassList +
                ", superClass='" + superClass + '\'' +
                '}';
    }
}

