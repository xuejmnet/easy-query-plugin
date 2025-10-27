package com.easy.query.plugin.core.contributor.java;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.QueryType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

/**
 * create time 2024/2/1 08:26
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyInclude2Contributor extends EasyContributor {

    public EasyInclude2Contributor(@NotNull String insertWord, @NotNull String tipWord) {
        super(insertWord, tipWord, true);
    }

    @Override
    protected String getLambdaBody(Collection<QueryType> queries, String lambdaBody) {
        return "{}";
    }

    @Override
    protected String getLambdaBodyExpression(Collection<QueryType> queries, String lambdaBody, boolean outBracket) {
        ArrayList<QueryType> queryTypes = new ArrayList<>();
        queryTypes.add(new QueryType("context", false));
        QueryType queryType = queries.stream().findFirst().orElse(null);
        if (queryType != null) {
            queryTypes.add(queryType);
        } else {
            queryTypes.add(new QueryType("table", false));
        }
        return super.getLambdaBodyExpression(queryTypes, lambdaBody, outBracket);
    }

    @Override
    protected int realBackOffset(int backOffset) {
        return backOffset - 2;
    }

    @Override
    public boolean accept(String beforeMethodReturnTypeName) {
        return beforeMethodReturnTypeName.startsWith("com.easy.query.api.proxy.entity.select.EntityQueryable") ||
            beforeMethodReturnTypeName.startsWith("com.easy.query.core.proxy.DbSet");
    }
}
