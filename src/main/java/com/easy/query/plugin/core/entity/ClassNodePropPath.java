package com.easy.query.plugin.core.entity;

import java.util.Objects;

/**
 * create time 2024/2/26 21:43
 * 文件说明
 *
 * @author xuejiaming
 */
public class ClassNodePropPath {
    private final String ownerPropertyName;
    private final String from;
    private final String to;
    private final String property;
    private int deep;

    public ClassNodePropPath(String ownerPropertyName, String from, String to, String property, int deep) {
        this.ownerPropertyName = ownerPropertyName == null ? "" : ownerPropertyName;

        this.from = from;
        this.to = to;
        this.property = property;
        this.deep = deep;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        ClassNodePropPath that = (ClassNodePropPath) o;
        return deep == that.deep && Objects.equals(ownerPropertyName, that.ownerPropertyName) && Objects.equals(from, that.from) && Objects.equals(to, that.to) && Objects.equals(property, that.property);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerPropertyName, from, to, property, deep);
    }

    public String getTo() {
        return to;
    }

    public String getFrom() {
        return from;
    }

    public int getDeep() {
        return deep;
    }

    public void setDeep(int deep) {
        this.deep = deep;
    }
}
