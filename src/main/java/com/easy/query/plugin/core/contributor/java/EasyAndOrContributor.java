package com.easy.query.plugin.core.contributor.java;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.QueryType;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.openapi.editor.Document;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * create time 2024/2/1 08:26
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyAndOrContributor extends EasyContributor{

    public EasyAndOrContributor(@NotNull String insertWord, @NotNull String tipWord, boolean blockCode){
        super(insertWord, tipWord, blockCode);

    }
    @Override
    public void insertString(InsertionContext context, Collection<QueryType> queries, boolean failBracket){
        Document document = context.getDocument();
        int insertPosition = context.getSelectionEndOffset();
        int wordBackOffset = insertWord.length() - tipWord.length();
        try {

            String lambdaBody = StrUtil.EMPTY;
            if (blockCode) {
                lambdaBody = "{}";
            }
            String lambdaExpression =getLambdaBodyExpression(queries,lambdaBody,true);
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
    protected String getLambdaBodyExpression(Collection<QueryType> queries,String lambdaBody,boolean outBracket){
        String leftParameterBracket = StrUtil.EMPTY;
        String leftOutParameterBracket = StrUtil.EMPTY;
        String rightParameterBracket = StrUtil.EMPTY;
        String rightOutParameterBracket = StrUtil.EMPTY;

        leftParameterBracket = "(";
        rightParameterBracket = ")";
        if(outBracket){
            leftOutParameterBracket = "(";
            rightOutParameterBracket = ")";
        }
        return leftOutParameterBracket + leftParameterBracket + rightParameterBracket + " -> " + getLambdaBody(queries,lambdaBody) + rightOutParameterBracket;
    }
}
