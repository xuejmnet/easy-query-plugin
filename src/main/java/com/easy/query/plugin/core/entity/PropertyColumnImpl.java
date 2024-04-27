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

public class PropertyColumnImpl implements PropertyColumn {
    private final String sqlColumnName;
    private final String propertyType;
    private String navigateProxyName;

    private static Map<String, String> IMPORT_MAPPING = new HashMap<>();

    static {
        IMPORT_MAPPING.put("SQLNumberColumn", "com.easy.query.core.proxy.columns.SQLNumberColumn");
        IMPORT_MAPPING.put("SQLBooleanColumn", "com.easy.query.core.proxy.columns.SQLBooleanColumn");
        IMPORT_MAPPING.put("SQLStringColumn", "com.easy.query.core.proxy.columns.SQLStringColumn");
        IMPORT_MAPPING.put("SQLDateTimeColumn", "com.easy.query.core.proxy.columns.SQLDateTimeColumn");
        IMPORT_MAPPING.put("SQLAnyColumn", "com.easy.query.core.proxy.columns.SQLAnyColumn");
    }

    public PropertyColumnImpl(String sqlColumnName, String propertyType) {

        this.sqlColumnName = sqlColumnName;
        this.propertyType = propertyType;
    }

    @Override
    public String getSqlColumnName() {
        return sqlColumnName;
    }

    @Override
    public String getPropertyType() {
        return propertyType;
    }

    @Override
    public String getPropertyTypeClass(boolean includeProperty) {
        if(!includeProperty){
            if(Objects.equals("SQLAnyColumn",sqlColumnName)){
                return "__cast(Object.class)";
            }
        }
        return propertyType+".class";
    }
    @Override
    public String getImport() {
        return IMPORT_MAPPING.get(sqlColumnName);
    }

    @Override
    public String getSQLColumnMethod() {
        switch (sqlColumnName) {
            case "SQLNumberColumn":
                return "getNumberColumn";
            case "SQLBooleanColumn":
                return "getBooleanColumn";
            case "SQLStringColumn":
                return "getStringColumn";
            case "SQLDateTimeColumn":
                return "getDateTimeColumn";
            case "SQLAnyColumn":
                return "getAnyColumn";
        }
        return "get";
    }

    @Override
    public String getNavigateProxyName() {
        return navigateProxyName;
    }

    @Override
    public void setNavigateProxyName(String navigateProxyName) {
        this.navigateProxyName = navigateProxyName;
    }
    @Override
    public boolean isAnyType(){
        return Objects.equals("getAnyColumn",sqlColumnName);
    }
}