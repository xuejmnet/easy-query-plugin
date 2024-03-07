package com.easy.query.plugin.core.entity;

/**
 * create time 2024/3/7 08:31
 * 文件说明
 *
 * @author xuejiaming
 */
public class TreeClassNode {
    private final int pathCount;
    private final ClassNode classNode;

    public TreeClassNode(int pathCount, ClassNode classNode){

        this.pathCount = pathCount;
        this.classNode = classNode;
    }

    public int getPathCount() {
        return pathCount;
    }

    public ClassNode getClassNode() {
        return classNode;
    }
}
