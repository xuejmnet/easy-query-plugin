package com.easy.query.plugin.core.contributor;

import org.jetbrains.annotations.NotNull;

/**
 * create time 2024/2/7 15:46
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasySetColumnsContributor extends EasyContributor{
    public EasySetColumnsContributor(@NotNull String insertWord, @NotNull String tipWord, boolean blockCode) {
        super(insertWord, tipWord, blockCode);
    }

    @Override
    public boolean accept(String beforeMethodReturnTypeName) {
        return beforeMethodReturnTypeName.startsWith("com.easy.query.api.proxy.entity.update.ExpressionUpdatable") ||
                beforeMethodReturnTypeName.startsWith("com.easy.query.api.proxy.entity.update.EntityUpdatable") ;
    }
}
