package com.easy.query.plugin.action;

import com.easy.query.plugin.core.VersionUtil;
import com.easy.query.plugin.core.util.ProjectUtils;
import com.easy.query.plugin.core.util.TableUtil;
import com.easy.query.plugin.windows.EntityTableGenerateDialog;
import com.intellij.database.model.DasTable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationInfo;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

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
        boolean selectedTable = shouldShow(e);
        e.getPresentation().setVisible(selectedTable);
    }

    private boolean shouldShow(AnActionEvent e) {
        boolean after20243 = VersionUtil.isAfter2024_3();
        try {

            if (after20243) {
                DasTable dasTable = TableUtil.getSelectedSingleTable(e);
                return dasTable != null;
            }
        } catch (Exception ignored) {

        }
        Object selectedElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        return selectedElement instanceof DasTable;
    }

    /**
     * 2022.2.5才有的方法
     *
     * @return
     */
    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
