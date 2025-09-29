package com.easy.query.plugin.core.contributor.java;

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
public class EasyConfigureGroupJoinContributor extends EasyContributor {

    public EasyConfigureGroupJoinContributor(@NotNull String insertWord, @NotNull String tipWord, boolean blockCode) {
        super(insertWord, tipWord, blockCode);
    }

    @Override
    protected String getLambdaBody(Collection<QueryType> queries, String lambdaBody) {
        return "";
    }

    @Override
    public void insertString(InsertionContext context, Collection<QueryType> queries, boolean failBracket){
        Document document = context.getDocument();
        int insertPosition = context.getSelectionEndOffset();
        int wordBackOffset = tipWord.length() *-1;
        try {

            String lambdaExpression ="configure(s->s.getBehavior().add(EasyBehaviorEnum.ALL_SUB_QUERY_GROUP_JOIN))";
            int realBackOffset = realBackOffset(wordBackOffset);
            document.insertString(insertPosition+wordBackOffset, lambdaExpression);
            insertPosition += lambdaExpression.length();
            deleteString(document,insertPosition,wordBackOffset);
            insertPosition = insertPosition + realBackOffset;
            context.getEditor().getCaretModel().getCurrentCaret().moveToOffset(insertPosition);
        }catch (Exception ex){
            if(failBracket){
                document.insertString(insertPosition, "()");
                context.getEditor().getCaretModel().getCurrentCaret().moveToOffset(insertPosition - wordBackOffset);
            }
        }
    }

    @Override
    protected int realBackOffset(int backOffset) {
        return backOffset;
    }

    @Override
    public boolean accept(String beforeMethodReturnTypeName) {
        return beforeMethodReturnTypeName.startsWith("com.easy.query.api.proxy.entity.select.EntityQueryable")||
            beforeMethodReturnTypeName.startsWith("com.easy.query.core.proxy.DbSet");
    }
}
