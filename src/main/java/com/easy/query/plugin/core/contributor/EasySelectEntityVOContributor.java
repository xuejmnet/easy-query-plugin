package com.easy.query.plugin.core.contributor;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.QueryType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * create time 2024/2/7 15:46
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasySelectEntityVOContributor extends EasyContributor{
    public EasySelectEntityVOContributor(@NotNull String insertWord, @NotNull String tipWord, boolean blockCode) {
        super(insertWord, tipWord, blockCode);
    }
    @Override
    protected String getLambdaBody(Collection<QueryType> queries, String lambdaBody) {
        QueryType next = queries.iterator().next();
        String collect = queries.stream().map(o -> o.getShortName() + ".FETCHER.allFields()").collect(Collectors.joining(",\n"));
        return String.format("Select.of(\n%s\n)", collect);
    }

    @Override
    protected String getLambdaBodyExpression(Collection<QueryType> queries, String lambdaBody, boolean outBracket) {

        String leftParameterBracket = StrUtil.EMPTY;
        String leftOutParameterBracket = StrUtil.EMPTY;
        String rightParameterBracket = StrUtil.EMPTY;
        String rightOutParameterBracket = StrUtil.EMPTY;
        if (queries.size() > 1) {
            leftParameterBracket = "(";
            rightParameterBracket = ")";
        }
        if(outBracket){
            leftOutParameterBracket = "(";
            rightOutParameterBracket = ")";
        }
        QueryType queryType = queries.stream().findFirst().orElse(null);
        boolean group = queryType != null && queryType.isGroup();
        if(group){
            return leftOutParameterBracket + leftParameterBracket + "group" + rightParameterBracket + " -> " + getLambdaBody(queries,lambdaBody) + rightOutParameterBracket;
        }else{
            String parameters = queries.stream().map(o -> o.getShortName()).collect(Collectors.joining(", "));
            return leftOutParameterBracket + "VO.class, "+leftParameterBracket + parameters + rightParameterBracket + " -> " + getLambdaBody(queries,lambdaBody) + rightOutParameterBracket;
        }
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
