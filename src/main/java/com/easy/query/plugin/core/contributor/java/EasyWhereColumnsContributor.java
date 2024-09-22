package com.easy.query.plugin.core.contributor.java;

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
public class EasyWhereColumnsContributor extends EasyContributor {
    public EasyWhereColumnsContributor(@NotNull String insertWord, @NotNull String tipWord, boolean blockCode) {
        super(insertWord, tipWord, blockCode);
    }

    @Override
    protected String getLambdaBody(Collection<QueryType> queries, String lambdaBody) {

        if(CollUtil.isNotEmpty(queries)){
            QueryType first = CollUtil.getFirst(queries);
            if(blockCode){
                return String.format("{return %s.FETCHER.columnKeys()}", first.getShortName());
            }
            return String.format("%s.FETCHER.columnKeys()", first.getShortName());
        }else{
            return super.getLambdaBody(queries,lambdaBody);
        }
    }

    @Override
    public boolean accept(String beforeMethodReturnTypeName) {
        return beforeMethodReturnTypeName.startsWith("com.easy.query.api.proxy.entity.update.EntityUpdatable") ||
                beforeMethodReturnTypeName.startsWith("com.easy.query.api4j.update.EntityUpdatable") ||
                beforeMethodReturnTypeName.startsWith("com.easy.query.api4kt.update.KtEntityUpdatable") ||
                beforeMethodReturnTypeName.startsWith("com.easy.query.core.basic.api.update.ClientEntityUpdatable") ;
    }
}
