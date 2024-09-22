package com.easy.query.plugin.core.contributor.kt;

import cn.hutool.core.collection.CollUtil;
import com.easy.query.plugin.core.entity.QueryType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * create time 2024/2/7 15:46
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyKtSelectorContributor extends EasyKtContributor {
    public EasyKtSelectorContributor(@NotNull String insertWord, @NotNull String tipWord) {
        super(insertWord, tipWord);
    }

    @Override
    public boolean accept(String beforeMethodReturnTypeName) {
        return beforeMethodReturnTypeName.startsWith("com.easy.query.api.proxy.entity.select.EntityQueryable") ;
    }
    @Override
    protected String getLambdaBody(Collection<QueryType> queries, String lambdaBody) {

        if(CollUtil.isNotEmpty(queries)){
            QueryType first = CollUtil.getFirst(queries);
            return String.format("%s.FETCHER", first.getShortName());
        }else{
            return super.getLambdaBody(queries,lambdaBody);
        }
    }
}
