package com.easy.query.plugin.core.entity;

import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * create time 2024/3/6 14:36
 * 文件说明
 *
 * @author xuejiaming
 */
public class StructDTOApp implements PropAppendable {
    @Getter
    private final String entityName;
    @Getter
    private final String ownerEntityName;
    @Getter
    private final String packageName;
    @Getter
    private final int sort;
    private final Map<String, StructDTOProp> props;
    @Getter
    private final Set<String> imports;

    public StructDTOApp(String entityName, String ownerEntityName, String packageName, int sort) {
        this.entityName = entityName;
        this.ownerEntityName = ownerEntityName;
        this.packageName = packageName;
        this.sort = sort;
        this.props = new LinkedHashMap<>();
        this.imports = new LinkedHashSet<>();
    }

    @Override
    public void addProp(StructDTOProp prop) {
        this.props.putIfAbsent(prop.getPropName(), prop);
    }

    @Override
    public String getSelfEntityType() {
        return null;
    }

    @Override
    public String getPropName() {
        return null;
    }

    @Override
    public List<StructDTOProp> getProps() {
        return props.values().stream().sorted(Comparator.comparingInt(StructDTOProp::getSort)).collect(Collectors.toList());
    }

    @Override
    public int getPathCount() {
        return 2;
    }
}
