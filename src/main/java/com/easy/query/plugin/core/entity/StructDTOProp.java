package com.easy.query.plugin.core.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * create time 2024/3/6 14:38
 * 文件说明
 *
 * @author xuejiaming
 */
public class StructDTOProp implements PropAppendable {

    private final String propName;
    /** 属性文本, 生成的时候, 实际输出的是这里面的内容 */
    @Setter
    @Getter
    private String propText;

    @Getter
    private final String owner;
    @Getter
    private final boolean entity;
    private final String selfEntityType;

    @Getter
    private final int sort;
    private final int pathCount;

    @Getter
    private final String ownerFullName;

    @Getter
    private final String selfFullEntityType;

    @Setter
    @Getter
    private String dtoName;

    @Setter
    @Getter
    private ClassNode classNode;


    private final Map<String, StructDTOProp> props;

    public StructDTOProp(String propName, String propText, String owner, boolean entity, String selfEntityType, int sort, int pathCount, String ownerFullName, String selfFullEntityType) {

        this.propName = propName;
        this.propText = propText;
        this.owner = owner;
        this.entity = entity;
        this.selfEntityType = selfEntityType;
        this.sort = sort;
        this.pathCount = pathCount;
        this.ownerFullName = ownerFullName;
        this.selfFullEntityType = selfFullEntityType;
        this.props = new LinkedHashMap<>();
    }

    @Override
    public void addProp(StructDTOProp prop) {
        this.props.putIfAbsent(prop.getPropName(), prop);
    }

    @Override
    public List<StructDTOProp> getProps() {
        return props.values().stream().sorted(Comparator.comparingInt(StructDTOProp::getSort)).collect(Collectors.toList());
    }

    @Override
    public String getPropName() {
        return propName;
    }

    @Override
    public String getSelfEntityType() {
        return selfEntityType;
    }

    @Override
    public int getPathCount() {
        return pathCount;
    }

}
