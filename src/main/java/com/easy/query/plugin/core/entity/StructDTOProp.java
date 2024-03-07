package com.easy.query.plugin.core.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * create time 2024/3/6 14:38
 * 文件说明
 *
 * @author xuejiaming
 */
public class StructDTOProp implements PropAppendable{
    private final String propName;
    private  String propText;
    private final String owner;
    private final boolean entity;
    private final String selfEntityType;
    private final int sort;
    private final int pathCount;
    private  String dtoName;


    private final List<StructDTOProp> props;
    public StructDTOProp(String propName, String propText,String owner,boolean entity,String selfEntityType,int sort,int pathCount){

        this.propName = propName;
        this.propText = propText;
        this.owner = owner;
        this.entity = entity;
        this.selfEntityType = selfEntityType;
        this.sort = sort;
        this.pathCount = pathCount;
        this.props = new ArrayList<>();
    }
    @Override
    public void addProp(StructDTOProp prop){
        this.props.add(prop);
    }

    @Override
    public List<StructDTOProp> getProps() {
        return props;
    }

    @Override
    public String getPropName() {
        return propName;
    }

    public String getPropText() {
        return propText;
    }

    public int getSort() {
        return sort;
    }

    public String getOwner() {
        return owner;
    }

    public boolean isEntity() {
        return entity;
    }

    @Override
    public String getSelfEntityType() {
        return selfEntityType;
    }

    public String getDtoName() {
        return dtoName;
    }

    public void setPropText(String propText) {
        this.propText = propText;
    }

    public void setDtoName(String dtoName) {
        this.dtoName = dtoName;
    }

    @Override
    public int getPathCount() {
        return pathCount;
    }
}
