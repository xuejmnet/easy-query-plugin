package com.easy.query.plugin.action;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

/**
 * 预览SQL 手动参数 并生成运行配置, 运行后不删除代码
 *
 * @author link2fun
 */
@Slf4j
public class PreviewEditorSQLManualParamAction extends PreviewEditorSQLAbstractAction {

    public PreviewEditorSQLManualParamAction() {
        super("manual");
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        // 使用EDT线程更新UI操作
        return ActionUpdateThread.EDT;
    }
}
