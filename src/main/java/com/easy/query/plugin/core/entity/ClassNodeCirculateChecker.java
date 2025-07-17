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

    /** 允许的路径重复次数 */
    private static final Integer REPEAT_LIMIT = 1;
    /** root 元素重复次数 */
    private Integer rootClassPathRepeatCount = 0;

    private final String rootClass;
    private final int maxDeep;
    private final Map<ClassNodePropPath, ClassNodePropPath> classNodePropPaths = new HashMap<>();

    public ClassNodeCirculateChecker(String rootClass, int maxDeep) {

        this.rootClass = rootClass;
        this.maxDeep = maxDeep;
    }

    public boolean pathRepeat(ClassNodePropPath classNodePropPath) {
        boolean repeat0 = pathRepeat0(classNodePropPath);
        if(!repeat0){
            if (maxDeep >= 0 && classNodePropPath.getDeep() >= maxDeep) {
                return true;
            }
        }
        return repeat0;
    }
    public boolean pathRepeat0(ClassNodePropPath classNodePropPath) {
        if (Objects.equals(rootClass, classNodePropPath.getTo())) {

            if (rootClassPathRepeatCount >= REPEAT_LIMIT) {
                // 超过次数限定判断为重复
                return true;
            }
            // 没有超过次数， 判定为允许
            rootClassPathRepeatCount++;
            return false;
        }
        ClassNodePropPath oldPath = classNodePropPaths.get(classNodePropPath);
        if (oldPath == null) {
            classNodePropPaths.put(classNodePropPath, classNodePropPath);
            return false;
        }
        if (oldPath.getDeep() < classNodePropPath.getDeep()) {
            return true;
        }
        oldPath.setDeep(classNodePropPath.getDeep());
        return false;
    }
}
