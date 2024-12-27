package com.easy.query.plugin.core.contributor.kt;

import com.easy.query.plugin.core.entity.QueryType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * create time 2024/2/1 08:26
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyKtGroupTableContributor extends EasyKtContributor {

    public EasyKtGroupTableContributor(@NotNull String insertWord, @NotNull String tipWord) {
        super(insertWord, tipWord);
    }

    @Override
    protected String getLambdaBody(Collection<QueryType> queries, String lambdaBody) {
        return String.format("GroupKeys.TABLE%s.of()", queries.size());
//        if(blockCode){
//            return StrUtil.format("{ %s };",groupExpression);
//        }

//        return groupExpression;
    }

    @Override
    protected int realBackOffset(int backOffset) {
        return backOffset-2;
    }

    @Override
    public boolean accept(String beforeMethodReturnTypeName) {
        return beforeMethodReturnTypeName.startsWith("com.easy.query.api.proxy.entity.select.EntityQueryable");
    }
}
