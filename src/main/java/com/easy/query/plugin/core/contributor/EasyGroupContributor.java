package com.easy.query.plugin.core.contributor;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.QueryType;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.editor.Document;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * create time 2024/2/1 08:26
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyGroupContributor  extends EasyContributor{

    public EasyGroupContributor(@NotNull String insertWord, @NotNull String tipWord, boolean blockCode) {
        super(insertWord, tipWord, blockCode);
    }

    @Override
    protected String getLambdaBody(Collection<QueryType> queries, String lambdaBody) {
        String groupExpression = String.format("GroupKeys.TABLE%s.of()", queries.size());
        if(blockCode){
            return StrUtil.format("{ %s };",groupExpression);
        }

        return groupExpression;
    }

    @Override
    protected int realBackOffset(int backOffset) {
        return backOffset+1;
    }
}
