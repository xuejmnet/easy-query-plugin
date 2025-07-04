package com.easy.query.plugin.action;

import com.easy.query.plugin.core.VersionUtil;
import com.easy.query.plugin.core.util.TableUtil;
import com.easy.query.plugin.windows.EntityTableGenerateDialog;
import com.intellij.database.model.DasTable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SingleTableGenerateAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // Get the selected table and pass it to the dialog
        DasTable selectedTable = getSelectedTable(e);
        if (selectedTable != null) {
            EntityTableGenerateDialog entityTableGenerateDialog = new EntityTableGenerateDialog(e, selectedTable.getName());
            SwingUtilities.invokeLater(() -> {
                entityTableGenerateDialog.setVisible(true);
            });
        }
    }
    
    /**
     * 获取选中的表
     * @param e 事件
     * @return 选中的表
     */
    private DasTable getSelectedTable(AnActionEvent e) {
        boolean after20243 = VersionUtil.isAfter2024_3();
        try {
            if (after20243) {
                return TableUtil.getSelectedSingleTable(e);
            }
        } catch (Exception ignored) {
        }
        Object selectedElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        return selectedElement instanceof DasTable ? (DasTable) selectedElement : null;
    }
    
    /**
     * 判断选中的是否是表，是表则显示，否则不显示
     *
     * @param e 事件
     */
    @Override
    public void update(AnActionEvent e) {
        DasTable selectedTable = getSelectedTable(e);
        e.getPresentation().setVisible(selectedTable != null);
    }

    /**
     * 2022.2.5才有的方法
     * @return
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
