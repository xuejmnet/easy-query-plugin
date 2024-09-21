package com.easy.query.plugin.core.contributor.java;

import com.easy.query.plugin.core.entity.QueryType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * create time 2024/2/1 08:26
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyAnonymousContributor extends EasyContributor {

    public EasyAnonymousContributor(@NotNull String insertWord, @NotNull String tipWord, boolean blockCode){
        super(insertWord, tipWord, blockCode);

    }
    protected String getLambdaBodyExpression(Collection<QueryType> queries,String lambdaBody,boolean outBracket){
        return "new MapProxy(\"AnonymousTypeName\",Object.class)\n" +
                ".put(\"\",)";
    }
}
