package com.easy.query.plugin.core.entity;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * create time 2024/2/26 21:43
 * 文件说明
 *
 * @author xuejiaming
 */
public class ClassNodeCirculateChecker {
    private final String rootClass;
    private Set<ClassNodePropPath> classNodePropPaths = new HashSet<>();
    public ClassNodeCirculateChecker(String rootClass){

        this.rootClass = rootClass;
    }

    public boolean pathRepeat(ClassNodePropPath classNodePropPath) {
        return Objects.equals(rootClass,classNodePropPath.getTo()) || !classNodePropPaths.add(classNodePropPath);
    }
}
