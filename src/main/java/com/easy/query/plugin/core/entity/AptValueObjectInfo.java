package com.easy.query.plugin.core.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * create time 2023/11/8 11:33
 * 文件说明
 *
 * @author xuejiaming
 */
public class AptValueObjectInfo {
    private final String entityName;
    private final List<AptPropertyInfo> properties;
    private  List<AptValueObjectInfo> children;

    public AptValueObjectInfo(String entityName){

        this.entityName = entityName;
        this.properties = new ArrayList<>();
        this.children = new ArrayList<>();
    }

    public String getEntityName() {
        return entityName;
    }

    public List<AptPropertyInfo> getProperties() {
        return properties;
    }

    public List<AptValueObjectInfo> getChildren() {
        return children;
    }
    public boolean isParent(String mainEntityName){
        return Objects.equals(mainEntityName,entityName);
    }
}
