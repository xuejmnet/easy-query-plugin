package com.easy.query.plugin.core.contributor;

import com.easy.query.plugin.core.contributor.java.EasyContributor;
import com.easy.query.plugin.core.entity.QueryType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * create time 2024/2/1 08:26
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyOnContributor extends EasyContributor {

    public EasyOnContributor(@NotNull String insertWord, @NotNull String tipWord, boolean blockCode) {
        super(insertWord, tipWord, blockCode);
    }

    @Override
    protected String getLambdaBodyExpression(Collection<QueryType> queries, String lambdaBody, boolean outBracket) {
        return super.getLambdaBodyExpression(queries, lambdaBody, false);
    }
    @Override
    protected int realBackOffset(int backOffset) {
        if(blockCode){
            return backOffset-1;
        }
        return backOffset;
    }
}
