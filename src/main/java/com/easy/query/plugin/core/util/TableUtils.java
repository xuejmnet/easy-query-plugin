package com.easy.query.plugin.core.util;

import cn.hutool.core.util.ReflectUtil;
import com.easy.query.plugin.core.entity.ColumnInfo;
import com.easy.query.plugin.core.entity.ColumnMetadata;
import com.easy.query.plugin.core.entity.MatchTypeMapping;
import com.easy.query.plugin.core.entity.TableInfo;
import com.easy.query.plugin.core.entity.TableMetadata;
import com.intellij.database.dialects.DatabaseDialectEx;
import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DasObject;
import com.intellij.database.model.DasTable;
import com.intellij.database.model.DasTypedObject;
import com.intellij.database.model.DataType;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbDataSourceImpl;
import com.intellij.database.psi.DbElement;
import com.intellij.database.psi.DbTableImpl;
import com.intellij.database.util.JdbcUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.util.containers.JBIterable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * create time 2023/11/28 22:40
 * 文件说明
 *
 * @author xuejiaming
 */
public class TableUtils {

    /**
     * 得到所有表
     *
     * @param event 事件
     * @return {@code List<TableInfo>}
     */
    public static List<TableMetadata> getAllTables(AnActionEvent event) {
        DbTableImpl table = (DbTableImpl) event.getData(CommonDataKeys.PSI_ELEMENT);
        DbElement tableParent = table.getParent();
        assert tableParent != null;
        List<DasTable> list = tableParent.getDasChildren(ObjectKind.TABLE).map(el -> (DasTable) el)
                .toList();
        List<DasTable> viewList = tableParent.getDasChildren(ObjectKind.VIEW).map(el -> (DasTable) el)
                .toList();
        List<DasTable> dasTables = new ArrayList<>(list);
        dasTables.addAll(viewList);
        return getTableInfoList(dasTables);
    }

    public static List<TableMetadata> getTableInfoList(List<DasTable> selectedTableList) {
        List<TableMetadata> tableInfoList = new ArrayList<>();
        DasTable dasTable = selectedTableList.get(0);
        DatabaseDialectEx dialect = getDialect(dasTable);
        for (DasTable table : selectedTableList) {
            TableMetadata tableInfo = new TableMetadata(table.getName(), table.getComment());
            List<ColumnMetadata> columnList = new ArrayList<>();
            JBIterable<? extends DasObject> columns = table.getDasChildren(ObjectKind.COLUMN);
            for (DasObject column : columns) {
//                ColumnInfo columnInfo = new ColumnInfo();
                DasColumn dasColumn = (DasColumn) column;
//                columnInfo.setName(dasColumn.getName());
//                columnInfo.setFieldName(StrUtil.toCamelCase(dasColumn.getName()));
                DataType dataType = getDataType(dasColumn);
                String jdbcTypeStr = dataType==null?"varchar(32)":dataType.toString();
//                String jdbcTypeStr = dasColumn.getDataType().toString();
//                String jdbcTypeStr = dasColumn.getDasType().toDataType().toString();
                int jdbc = dialect.getJavaTypeForNativeType(jdbcTypeStr);
//                String jdbcTypeName = JdbcUtil.getJdbcTypeName(jdbc);
//                String fieldType = getFieldType(jdbc, tableInfo, jdbcTypeName, dasColumn.getDasType().toDataType().size, jdbcTypeStr.toLowerCase());
//                columnInfo.setFieldType(fieldType);
//                columnInfo.setNotNull(dasColumn.isNotNull());
//                columnInfo.setComment();
//                columnInfo.setMethodName(StrUtil.upperFirst(columnInfo.getFieldName()));
//                columnInfo.setType(jdbcTypeName);
//                columnInfo.setPrimaryKey(table.getColumnAttrs(dasColumn).contains(DasColumn.Attribute.PRIMARY_KEY));
//                columnInfo.setAutoIncrement(table.getColumnAttrs(dasColumn).contains(DasColumn.Attribute.AUTO_GENERATED));
                String columnComment = ObjectUtil.defaultIfNull(dasColumn.getComment(), "" ).replaceAll("\n" , "" );
                boolean primary = table.getColumnAttrs(dasColumn).contains(DasColumn.Attribute.PRIMARY_KEY);
                boolean autoIncrement = table.getColumnAttrs(dasColumn).contains(DasColumn.Attribute.AUTO_GENERATED);
                columnList.add(new ColumnMetadata(dasColumn.getName(), jdbcTypeStr, jdbc, dasColumn.isNotNull(), columnComment, primary, autoIncrement, dataType==null?0:dataType.size));
            }
            tableInfo.getColumns().addAll(columnList);
            tableInfoList.add(tableInfo);
        }
        return tableInfoList;
    }

    public static DataType getDataType(DasColumn dasColumn) {
        Method getDataType = ReflectUtil.getMethod(DasTypedObject.class, "getDataType" );
        if (getDataType != null) {
            Object dataType = ReflectUtil.invoke(dasColumn, getDataType);
            if (dataType != null) {
                return (DataType)dataType;
            }
        }
        Method getDasType = ReflectUtil.getMethod(DasTypedObject.class, "getDasType" );
        if (getDasType != null) {
            Object dasType = ReflectUtil.invoke(dasColumn, getDasType);
            if (dasType != null) {
                Method toDataType = ReflectUtil.getMethod(dasType.getClass(), "toDataType" );
                if (toDataType != null) {
                    Object invoke = ReflectUtil.invoke(dasColumn, toDataType);
                    if (invoke != null) {
                        return (DataType)invoke;
                    }
                }
            }
        }
        return null;
    }

    private static String getFieldType(int jdbc, TableInfo tableInfo, String jdbcTypeName, int size, String jdbcTypeStr) {
//        Map<String, List<MatchTypeMapping>> typeMapping = MybatisFlexPluginConfigData.getTypeMapping();
//        if (typeMapping.containsKey("ORDINARY")) {
//            for (MatchTypeMapping mapping : typeMapping.get("ORDINARY")) {
//                if (jdbcTypeStr.equals(mapping.getColumType())) {
//                    return mapping.getJavaField();
//                }
//            }
//        }
//        if (typeMapping.containsKey("REGEX")) {
//            for (MatchTypeMapping mapping : typeMapping.get("REGEX")) {
//                String group0 = ReUtil.getGroup0(mapping.getColumType(), jdbcTypeStr);
//                if (StrUtil.isNotEmpty(group0)) {
//                    return mapping.getJavaField();
//                }
//            }
//        }

        String className = convert(jdbc, size).getName();
//        if (Object.class.getName().equals(className)) {
//            String fieldType = MybatisFlexPluginConfigData.getFieldType(jdbcTypeName);
//            if (StrUtil.isNotBlank(fieldType)) {
//                className = fieldType;
//            }
//        }

        boolean flag = className.contains(";" );
        if (flag) {
            className = className.replace(";" , "" ).replace("[L" , "" );
        }
        tableInfo.addImportClassItem(className);
        String fieldType = className.substring(className.lastIndexOf("." ) + 1);
        if (flag) {
            fieldType += "[]";
        }
        return fieldType;
    }

    public static DatabaseDialectEx getDialect(DasTable dasTable) {
        DbTableImpl table = (DbTableImpl) dasTable;
        DbDataSourceImpl dataSource = table.getDataSource();
        return dataSource.getDatabaseDialect();
    }

    public static Class<?> convert(int sqlType, int size) {
        switch (sqlType) {
            case Types.BIT:
                return Boolean.class;
            case Types.SMALLINT:
                return Short.class;
            case Types.INTEGER:
                return Integer.class;
            case Types.BIGINT:
                return Long.class;
            case Types.FLOAT:
            case Types.REAL:
                return Float.class;
            case Types.DOUBLE:
                return Double.class;
            case Types.NUMERIC:
            case Types.DECIMAL:
                return java.math.BigDecimal.class;
            case Types.CHAR:
            case Types.NCHAR:
            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
                return String.class;
            case Types.TINYINT:
                if (size == 1) {
                    return Boolean.class;
                } else if (size == 2) {
                    return Short.class;
                } else {
                    return Integer.class;
                }
            case Types.TIME:
                return LocalTime.class;
            case Types.TIMESTAMP:
//                return java.sql.Timestamp.class;
            case Types.DATE:
                return LocalDateTime.class;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
                return Byte[].class;
            // 返回对象，在点击生成代码是时候让用户自行选择
            // case Types.NCLOB:
            //     return java.sql.NClob.class;
            // case Types.ARRAY:
            //     return java.sql.Array.class;
            // case Types.STRUCT:
            //     return java.sql.Struct.class;
            // case Types.REF:
            //     return java.sql.Ref.class;
            // case Types.SQLXML:
            //     return java.sql.SQLXML.class;
            default:
                return Object.class;
        }
    }

    /**
     * 得到类名
     *
     * @param tableName   表名
     * @param tablePrefix 表前缀
     * @return {@code String}
     */
    public static String getClassName(String tableName, String tablePrefix) {
        tableName = getTableName(tableName, tablePrefix);
        return StrUtil.upperFirst(tableName);
    }

    /**
     * 得到表名
     *
     * @param tableName   表名
     * @param tablePrefix 表前缀
     * @return {@code String}
     */
    public static String getTableName(String tableName, String tablePrefix) {
        tablePrefix = ObjectUtil.defaultIfNull(tablePrefix, "" );
        String[] tablePrefixArr = tablePrefix.split(";" );
        for (String prefix : tablePrefixArr) {
            if (tableName.startsWith(prefix)) {
                tableName = tableName.replaceFirst(prefix, "" );
                break;
            }
        }
        return tableName;
    }

    public static Map<String, List<MatchTypeMapping>> getDefaultTypeMappingMap() {
        Map<String, List<MatchTypeMapping>> map = new HashMap<>();
        map.put("REGEX" , getRegexTypeMapping());
        map.put("ORDINARY" , getOrdinaryTypeMapping());
        return map;
    }

    public static List<MatchTypeMapping> getRegexTypeMapping() {
        List<MatchTypeMapping> list = new ArrayList<>();
        list.add(new MatchTypeMapping("REGEX" , "java.lang.String" , "varchar(\\(\\d+\\))?" ));
        list.add(new MatchTypeMapping("REGEX" , "java.lang.String" , "char(\\(\\d+\\))?" ));
        list.add(new MatchTypeMapping("REGEX" , "java.lang.String" , "(tiny|medium|long)*text" ));
        list.add(new MatchTypeMapping("REGEX" , "java.math.BigDecimal" , "decimal(\\(\\d+,\\d+\\))?" ));
        list.add(new MatchTypeMapping("REGEX" , "java.lang.Integer" , "(tiny|small|medium)*int(\\(\\d+\\))?" ));
        list.add(new MatchTypeMapping("REGEX" , "java.lang.Long" , "bigint(\\(\\d+\\))?" ));
        return list;
    }

    public static List<MatchTypeMapping> getOrdinaryTypeMapping() {

        List<MatchTypeMapping> list = new ArrayList<>();
        list.add(new MatchTypeMapping("ORDINARY" , "java.lang.Boolean" , "tinyint(1)" ));
        list.add(new MatchTypeMapping("ORDINARY" , "java.lang.Integer" , "integer" ));
        list.add(new MatchTypeMapping("ORDINARY" , "java.lang.String" , "int4" ));
        list.add(new MatchTypeMapping("ORDINARY" , "java.lang.Long" , "int8" ));
        list.add(new MatchTypeMapping("ORDINARY" , "java.time.LocalDate" , "date" ));
        list.add(new MatchTypeMapping("ORDINARY" , "java.time.LocalDateTime" , "datetime" ));
        list.add(new MatchTypeMapping("ORDINARY" , "java.util.Date" , "timestamp" ));
        list.add(new MatchTypeMapping("ORDINARY" , "java.time.LocalTime" , "time" ));
        list.add(new MatchTypeMapping("ORDINARY" , "java.lang.Boolean" , "boolean" ));
        return list;
    }
}
