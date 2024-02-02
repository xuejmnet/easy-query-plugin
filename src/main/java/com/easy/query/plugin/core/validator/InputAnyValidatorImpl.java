package com.easy.query.plugin.core.validator;

import com.intellij.openapi.ui.InputValidator;

/**
 * create time 2023/12/11 17:12
 * 文件说明
 *
 * @author xuejiaming
 */
public class InputAnyValidatorImpl implements InputValidator {
    @Override
    public boolean checkInput(String inputString) {
        return true;
    }

    @Override
    public boolean canClose(String inputString) {
        return true;
    }
}
