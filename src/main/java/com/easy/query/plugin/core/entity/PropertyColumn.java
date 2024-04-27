package com.easy.query.plugin.core.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * create time 2023/12/25 12:01
 * 文件说明
 *
 * @author xuejiaming
 */

public interface PropertyColumn {
    String getSqlColumnName();

    String getPropertyType();

    String getPropertyTypeClass(boolean includeProperty);

    String getImport();

    String getSQLColumnMethod();

    String getNavigateProxyName();

    void setNavigateProxyName(String navigateProxyName);

    boolean isAnyType();
}