package com.easy.query.plugin.core.entity;

/**
 * create time 2024/1/31 16:59
 * 文件说明
 *
 * @author xuejiaming
 */
public class QueryType {
    private final String shortName;
    private final boolean group;

    public QueryType(String shortName) {
        this(shortName, false);
    }

    public QueryType(String shortName, boolean group) {

        this.shortName = shortName;
        this.group = group;
    }

    public String getShortName() {
        return shortName;
    }

    public boolean isGroup() {
        return group;
    }
}
