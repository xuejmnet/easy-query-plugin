package com.easy.query.plugin.action;

import com.easy.query.plugin.core.util.ProjectUtils;
import com.easy.query.plugin.windows.EntityTableGenerateDialog;
import com.intellij.database.model.DasTable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;

import javax.swing.*;

public class TableGenerateAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        EntityTableGenerateDialog entityTableGenerateDialog = new EntityTableGenerateDialog(e);
        SwingUtilities.invokeLater(() -> {
            entityTableGenerateDialog.setVisible(true);
        });
    }
    /**
     * 判断选中的是否是表，是表则显示，否则不显示
     *
     * @param e 事件
     */
    @Override
    public void update(AnActionEvent e) {
        Object selectedElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        boolean isSelectedTable = selectedElement instanceof DasTable;
        e.getPresentation().setVisible(isSelectedTable);
    }

}
