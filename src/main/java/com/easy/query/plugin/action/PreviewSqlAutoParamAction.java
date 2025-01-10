package com.easy.query.plugin.action;

import lombok.extern.slf4j.Slf4j;

/**
 * 预览SQL 自动生成参数 并弹窗, 运行后删除代码
 *
 * @author link2fun
 */
@Slf4j
public class PreviewSqlAutoParamAction extends AbstractPreviewSqlAction {


    public PreviewSqlAutoParamAction() {
        super("auto");
    }
}
