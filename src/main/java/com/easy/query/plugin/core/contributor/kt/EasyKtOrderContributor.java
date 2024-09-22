package com.easy.query.plugin.core.contributor.kt;

import com.easy.query.plugin.core.contributor.java.EasyContributor;
import org.jetbrains.annotations.NotNull;

/**
 * create time 2024/2/7 15:46
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyKtOrderContributor extends EasyKtContributor {
    public EasyKtOrderContributor(@NotNull String insertWord, @NotNull String tipWord) {
        super(insertWord, tipWord);
    }

    @Override
    public boolean accept(String beforeMethodReturnTypeName) {
        return beforeMethodReturnTypeName.startsWith("com.easy.query.api.proxy.entity.select.EntityQueryable");
    }
}
