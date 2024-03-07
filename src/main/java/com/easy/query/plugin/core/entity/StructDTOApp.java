package com.easy.query.plugin.core.entity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * create time 2024/3/6 14:36
 * 文件说明
 *
 * @author xuejiaming
 */
public class StructDTOApp implements PropAppendable{
    private final String entityName;
    private final String ownerEntityName;
    private final String packageName;
    private final int sort;
    private final List<StructDTOProp> props;
    private final Set<String> imports;

    public StructDTOApp(String entityName,String ownerEntityName,String packageName,int sort){
        this.entityName = entityName;
        this.ownerEntityName = ownerEntityName;
        this.packageName = packageName;
        this.sort = sort;
        this.props = new ArrayList<>();
        this.imports = new LinkedHashSet<>();
    }

    @Override
    public void addProp(StructDTOProp prop){
        this.props.add(prop);
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
        return props;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getOwnerEntityName() {
        return ownerEntityName;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getSort() {
        return sort;
    }

    public Set<String> getImports() {
        return imports;
    }

    @Override
    public int getPathCount() {
        return 2;
    }
}
