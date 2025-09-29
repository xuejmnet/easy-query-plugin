package com.easy.query.plugin.core.contributor.java;

import org.jetbrains.annotations.NotNull;

/**
 * create time 2024/2/7 15:46
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyIncludesContributor extends EasyContributor {
    public EasyIncludesContributor(@NotNull String insertWord, @NotNull String tipWord, boolean blockCode) {
        super(insertWord, tipWord, blockCode);
    }

    @Override
    public boolean accept(String beforeMethodReturnTypeName) {
        return beforeMethodReturnTypeName.startsWith("com.easy.query.api.proxy.entity.select.EntityQueryable")||
            beforeMethodReturnTypeName.startsWith("com.easy.query.core.proxy.DbSet");
    }
}
