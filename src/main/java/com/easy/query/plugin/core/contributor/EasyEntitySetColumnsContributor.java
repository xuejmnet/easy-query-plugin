package com.easy.query.plugin.core.contributor;

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
public class EasyEntitySetColumnsContributor extends EasyContributor{
    public EasyEntitySetColumnsContributor(@NotNull String insertWord, @NotNull String tipWord, boolean blockCode) {
        super(insertWord, tipWord, blockCode);
    }

    @Override
    public boolean accept(String beforeMethodReturnTypeName) {
        return beforeMethodReturnTypeName.startsWith("com.easy.query.api.proxy.entity.update.EntityUpdatable") ;
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
