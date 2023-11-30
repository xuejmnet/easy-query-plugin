package com.easy.query.plugin.core.util;

import com.easy.query.plugin.core.entity.ColumnInfo;
import com.easy.query.plugin.core.entity.TableInfo;
import com.intellij.database.dialects.DatabaseDialectEx;
import com.intellij.database.model.DasColumn;
import com.intellij.database.model.DasObject;
import com.intellij.database.model.DasTable;
import com.intellij.database.model.ObjectKind;
import com.intellij.database.psi.DbDataSourceImpl;
import com.intellij.database.psi.DbElement;
import com.intellij.database.psi.DbTableImpl;
import com.intellij.database.util.JdbcUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.util.containers.JBIterable;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
    public static List<TableInfo> getAllTables(AnActionEvent event) {
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
    public static List<TableInfo> getTableInfoList(List<DasTable> selectedTableList) {
        List<TableInfo> tableInfoList = new ArrayList<>();
        DasTable dasTable = selectedTableList.get(0);
        DatabaseDialectEx dialect = getDialect(dasTable);
        for (DasTable table : selectedTableList) {
            TableInfo tableInfo = new TableInfo();
            tableInfo.setName(table.getName());
            tableInfo.setComment(table.getComment());
            List<ColumnInfo> columnList = new CopyOnWriteArrayList<>();
            JBIterable<? extends DasObject> columns = table.getDasChildren(ObjectKind.COLUMN);
            for (DasObject column : columns) {
                ColumnInfo columnInfo = new ColumnInfo();
                DasColumn dasColumn = (DasColumn) column;
                columnInfo.setName(dasColumn.getName());
                columnInfo.setFieldName(StrUtil.toCamelCase(dasColumn.getName().toLowerCase()));
                String jdbcTypeStr = dasColumn.getDataType().toString();
                int jdbc = dialect.getJavaTypeForNativeType(jdbcTypeStr);
                String jdbcTypeName = JdbcUtil.getJdbcTypeName(jdbc);
                String fieldType = getFieldType(jdbc, tableInfo, jdbcTypeName, dasColumn.getDataType().size, jdbcTypeStr.toLowerCase());
                columnInfo.setFieldType(fieldType);
                columnInfo.setNotNull(dasColumn.isNotNull());
                columnInfo.setComment(ObjectUtil.defaultIfNull(dasColumn.getComment(), "").replaceAll("\n", ""));
                columnInfo.setMethodName(StrUtil.upperFirst(columnInfo.getFieldName()));
                columnInfo.setType(jdbcTypeName);
                columnInfo.setPrimaryKey(table.getColumnAttrs(dasColumn).contains(DasColumn.Attribute.PRIMARY_KEY));
                columnInfo.setAutoIncrement(table.getColumnAttrs(dasColumn).contains(DasColumn.Attribute.AUTO_GENERATED));
                columnList.add(columnInfo);
            }
            tableInfo.setColumnList(columnList);
            tableInfoList.add(tableInfo);
        }
        return tableInfoList;
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

        boolean flag = className.contains(";");
        if (flag) {
            className = className.replace(";", "").replace("[L", "");
        }
        tableInfo.addImportClassItem(className);
        String fieldType = className.substring(className.lastIndexOf(".") + 1);
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
                return java.sql.Time.class;
            case Types.TIMESTAMP:
                return java.sql.Timestamp.class;
            case Types.DATE:
                return Date.class;
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

}
