package com.easy.query.plugin.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import com.intellij.database.model.DasTable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * create time 2024/11/23 11:31
 * 文件说明
 *
 * @author xuejiaming
 */
public class TableUtil {

    /**
     * 得到选中表名
     *
     * @param actionEvent 行动事件
     * @return {@code List<TableInfo>}
     */
    public static List<String> getSelectedTableName(AnActionEvent actionEvent) {
        return getSelectedTable(actionEvent).stream().map(DasTable::getName).collect(Collectors.toList());
    }

    /**
     * 得到选中表
     *
     * @param actionEvent 行动事件
     * @return {@code List<DasTable>}
     */

    public static List<DasTable> getSelectedTable(AnActionEvent actionEvent) {
        DataKey<Object[]> databaseNodes = DataKey.create("DATABASE_NODES");
        Object[] data = actionEvent.getData(databaseNodes);
        if (ArrayUtil.isEmpty(data)) {
            return new ArrayList<>();
        }
        return Arrays.stream(data).map(item -> {
            DasTable dasTable = (DasTable) item;
            return dasTable;
        }).collect(Collectors.toList());
    }

    public static DasTable getSelectedSingleTable(AnActionEvent actionEvent) {
        List<DasTable> selectedTable = getSelectedTable(actionEvent);
        if(CollUtil.isEmpty(selectedTable)){
            return null;
        }
        return selectedTable.get(0);
    }
}
