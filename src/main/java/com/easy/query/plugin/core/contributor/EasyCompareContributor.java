package com.easy.query.plugin.core.contributor;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.contributor.java.EasyContributor;
import com.easy.query.plugin.core.entity.QueryType;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.editor.Document;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * create time 2024/2/1 08:26
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyCompareContributor extends EasyContributor {

    private final String lambdaExpression;

    public EasyCompareContributor(@NotNull String insertWord, @NotNull String tipWord, String lambdaExpression) {
        super(insertWord, tipWord, false);
        this.lambdaExpression = lambdaExpression;
    }

    @Override
    protected String getLambdaBodyExpression(Collection<QueryType> queries, String lambdaBody, boolean outBracket) {
        return lambdaExpression;
    }
    @Override
    protected int realBackOffset(int backOffset) {
        return backOffset-1;
    }

    @Override
    public void insertString(InsertionContext context, Collection<QueryType> queries, boolean failBracket) {
        Document document = context.getDocument();
        int insertPosition = context.getSelectionEndOffset();
        int wordBackOffset = -tipWord.length();
        try {
            String lambdaExpression =getLambdaBodyExpression(queries,StrUtil.EMPTY,true);
            int realBackOffset = realBackOffset(wordBackOffset);
            document.insertString(insertPosition+wordBackOffset, lambdaExpression);
            insertPosition += lambdaExpression.length();
            deleteString(document,insertPosition,wordBackOffset);
            insertPosition = insertPosition + realBackOffset;
            context.getEditor().getCaretModel().getCurrentCaret().moveToOffset(insertPosition);
        }catch (Exception ex){
            if(failBracket){
//                document.insertString(insertPosition, "()");
                context.getEditor().getCaretModel().getCurrentCaret().moveToOffset(insertPosition - wordBackOffset);
            }
        }
    }
}
