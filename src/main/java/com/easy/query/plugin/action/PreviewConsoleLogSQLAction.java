package com.easy.query.plugin.action;

import com.easy.query.plugin.core.expression.SimpleFunction;
import com.easy.query.plugin.windows.SQLPreviewDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

/**
 * 预览控制台日志的SQL
 */
public class PreviewConsoleLogSQLAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(PreviewConsoleLogSQLAction.class);
    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        // 如果是从项目视图中右键点击的进来的则创建新的类
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        Project project = e.getProject();
        if (editor != null) {
            String selectedText = editor.getSelectionModel().getSelectedText();
            preview(project,selectedText, () -> {
            });
        }
    }


    //预览
    public void preview(Project project,String selectedText, SimpleFunction function) {
        try {
            SQLPreviewDialog sqlPreviewDialog = new SQLPreviewDialog(project,StringUtils.isBlank(selectedText)?"":selectedText);
            SwingUtilities.invokeLater(() -> {
                sqlPreviewDialog.setVisible(true);
            });
        } catch (Exception e) {
            LOG.error(e);
        } finally {
            // new File("MybatisFlexSqlPreview").delete();
            function.apply();
        }
    }
}
