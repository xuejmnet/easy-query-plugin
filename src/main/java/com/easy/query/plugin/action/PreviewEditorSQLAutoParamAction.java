package com.easy.query.plugin.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * 预览SQL 自动生成参数 并弹窗, 运行后删除代码
 *
 * @author link2fun
 */
@Slf4j
public class PreviewEditorSQLAutoParamAction extends PreviewEditorSQLAbstractAction {


    public PreviewEditorSQLAutoParamAction() {
        super("auto");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }
}
