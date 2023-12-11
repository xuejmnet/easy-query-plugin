package com.easy.query.plugin.core.validator;

import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.openapi.ui.InputValidator;

/**
 * create time 2023/12/11 17:12
 * 文件说明
 *
 * @author xuejiaming
 */
public class InputValidatorImpl implements InputValidator {
    @Override
    public boolean checkInput(String inputString) {
        return StrUtil.isNotEmpty(inputString);
    }

    @Override
    public boolean canClose(String inputString) {
        return StrUtil.isNotEmpty(inputString);
    }
}
