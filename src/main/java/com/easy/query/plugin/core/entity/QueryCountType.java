package com.easy.query.plugin.core.entity;

/**
 * create time 2024/1/31 16:59
 * 文件说明
 *
 * @author xuejiaming
 */
public class QueryCountType {
    private final String shortName;
    private final int index;
    private final boolean group;

    public QueryCountType(String shortName,int index) {
        this(shortName,index, false);
    }

    public QueryCountType(String shortName,int index, boolean group) {

        this.shortName = shortName;
        this.index = index;
        this.group = group;
    }

    public String getShortName() {
        return shortName;
    }

    public int getIndex() {
        return index;
    }

    public boolean isGroup() {
        return group;
    }

}
