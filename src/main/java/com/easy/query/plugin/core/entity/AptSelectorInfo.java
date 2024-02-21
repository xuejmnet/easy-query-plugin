package com.easy.query.plugin.core.entity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * create time 2023/12/6 10:00
 * 文件说明
 *
 * @author xuejiaming
 */
public class AptSelectorInfo {
    private final String name;
    private final Map<String,AptSelectPropertyInfo> propertieMap;

    public AptSelectorInfo(String name){

        this.name = name;
        this.propertieMap=new LinkedHashMap<>();
    }

    public String getName() {
        return name;
    }

    public List<AptSelectPropertyInfo> getProperties() {
        return new ArrayList<>(propertieMap.values());
    }
    public void addProperties(AptSelectPropertyInfo aptSelectPropertyInfo){
        this.propertieMap.putIfAbsent(aptSelectPropertyInfo.getPropertyName(),aptSelectPropertyInfo);
    }
}
