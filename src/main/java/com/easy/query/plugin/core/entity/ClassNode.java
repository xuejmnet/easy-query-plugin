package com.easy.query.plugin.core.entity;

import com.easy.query.plugin.core.enums.BeanPropTypeEnum;
import com.easy.query.plugin.core.util.StrUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * create time 2024/3/6 08:54
 * 文件说明
 *
 * @author xuejiaming
 */
public class ClassNode {
    private final String name;
    private final String owner;
    private final int sort;
    private String propText;
    private final boolean primary;
    /**
     * 是否是对象是就是导航属性
     */
    private final boolean entity;
    private final String selfEntityType;
    private final String ownerPropertyName;
    private final String ownerFullName;
    private final String selfFullEntityType;
    private final BeanPropTypeEnum beanPropType;
    private final List<ClassNode> children;
    private final Set<String> requireProps;
    /**
     * 无值表示主键
     */
    private String selfNavigateId;
    private String targetNavigateId;
    private String dtoName;
    private String comment;
    private String relationType;
    private String conversion;
    private String columnValue;

    public ClassNode(String name, String owner, int sort, boolean primary, boolean entity, String selfEntityType, String ownerPropertyName, String ownerFullName, String selfFullEntityType, BeanPropTypeEnum beanPropType) {

        this.name = name;
        this.owner = owner;
        this.sort = sort;
        this.primary = primary;
        this.entity = entity;
        this.selfEntityType = selfEntityType;
        this.ownerPropertyName = ownerPropertyName;
        this.ownerFullName = ownerFullName;
        this.selfFullEntityType = selfFullEntityType;
        this.beanPropType = beanPropType;
        this.children = new ArrayList<>();
        this.requireProps = new HashSet<>();
    }

    public void addChild(ClassNode child) {
        this.children.add(child);
    }

    public List<ClassNode> getChildren() {
        return children;
    }

    public String getName() {
        return name;
    }

    public String getPropertyNameGetMethodName() {
        if(beanPropType==BeanPropTypeEnum.IS){
            return "is" + StrUtil.toUpperCaseFirstOne(name);
        }
        return "get" + StrUtil.toUpperCaseFirstOne(name);
    }

    public String getOwner() {
        return owner;
    }

    public String getPropText() {
        return propText;
    }

    public void setPropText(String propText) {
        this.propText = propText;
    }

    public int getSort() {
        return sort;
    }

    public Set<String> getRequireProps() {
        return requireProps;
    }

    public boolean isPrimary() {
        return primary;
    }

    public String getSelfNavigateId() {
        return selfNavigateId;
    }

    public void setSelfNavigateId(String selfNavigateId) {
        this.selfNavigateId = selfNavigateId;
    }

    public String getTargetNavigateId() {
        return targetNavigateId;
    }

    public void setTargetNavigateId(String targetNavigateId) {
        this.targetNavigateId = targetNavigateId;
    }

    public boolean isEntity() {
        return entity;
    }
    public String getSelfEntityType() {
        return selfEntityType;
    }

    public String getDtoName() {
        return dtoName;
    }

    public void setDtoName(String dtoName) {
        this.dtoName = dtoName;
    }

    @Override
    public String toString() {
        if(cn.hutool.core.util.StrUtil.isNotBlank(comment)){
            return getName()+" 备注:"+comment;
        }
        return getName();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        if(comment==null){
            this.comment="";
        }else{
            this.comment = comment;
        }
    }

    public String getOwnerPropertyName() {
        return ownerPropertyName;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public String getConversion() {
        return conversion;
    }

    public void setConversion(String conversion) {
        this.conversion = conversion;
    }

    public String getOwnerFullName() {
        return ownerFullName;
    }

    public String getColumnValue() {
        return columnValue;
    }

    public void setColumnValue(String columnValue) {
        this.columnValue = columnValue;
    }

    public String getSelfFullEntityType() {
        return selfFullEntityType;
    }
}
