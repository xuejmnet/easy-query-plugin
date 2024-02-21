package com.easy.query.plugin.core.entity;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * create time 2023/11/8 11:33
 * 文件说明
 *
 * @author xuejiaming
 */
public class AptValueObjectInfo {
    private final String entityName;
    private final Map<String,AptPropertyInfo> propertieMap;
    private final List<AptValueObjectInfo> children;

    public AptValueObjectInfo(String entityName){

        this.entityName = entityName;
        this.propertieMap = new LinkedHashMap<>();
        this.children = new ArrayList<>();
    }

    public String getEntityName() {
        return entityName;
    }

    public List<AptPropertyInfo> getProperties() {
        return new ArrayList<>(propertieMap.values());
    }
    public void addProperties(AptPropertyInfo aptPropertyInfo){
        propertieMap.putIfAbsent(aptPropertyInfo.getPropertyName(),aptPropertyInfo);
    }

    public List<AptValueObjectInfo> getChildren() {
        return children;
    }
}
