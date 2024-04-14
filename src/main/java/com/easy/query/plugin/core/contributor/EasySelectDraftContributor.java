package com.easy.query.plugin.core.contributor;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.QueryType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * create time 2024/2/1 08:26
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasySelectDraftContributor extends EasyContributor{

    public EasySelectDraftContributor(@NotNull String insertWord, @NotNull String tipWord, boolean blockCode) {
        super(insertWord, tipWord, blockCode);
    }

    @Override
    protected String getLambdaBody(Collection<QueryType> queries, String lambdaBody) {
        String groupExpression = "Select.DRAFT.of()";
        if(blockCode){
            return StrUtil.format("{ %s };",groupExpression);
        }

        return groupExpression;
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
