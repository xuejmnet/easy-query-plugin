package com.easy.query.plugin.core.entity;

import java.util.List;

/**
 * create time 2024/3/7 08:21
 * 文件说明
 *
 * @author xuejiaming
 */
public interface PropAppendable {
    void addProp(StructDTOProp prop);
    List<StructDTOProp> getProps();
    int getPathCount();
    String getSelfEntityType();
    String getPropName();
}
