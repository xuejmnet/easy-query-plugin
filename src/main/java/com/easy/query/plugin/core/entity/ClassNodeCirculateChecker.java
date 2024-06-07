package com.easy.query.plugin.core.entity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
    private Map<ClassNodePropPath,ClassNodePropPath> classNodePropPaths = new HashMap<>();
    public ClassNodeCirculateChecker(String rootClass){

        this.rootClass = rootClass;
    }

    public boolean pathRepeat(ClassNodePropPath classNodePropPath) {
        if(Objects.equals(rootClass,classNodePropPath.getTo())){
            return true;
        }
        ClassNodePropPath oldPath = classNodePropPaths.get(classNodePropPath);
        if(oldPath==null){
            classNodePropPaths.put(classNodePropPath,classNodePropPath);
            return false;
        }
        if(oldPath.getDeep()<classNodePropPath.getDeep()){
            return true;
        }
        oldPath.setDeep(classNodePropPath.getDeep());
        return  false;
    }
}
