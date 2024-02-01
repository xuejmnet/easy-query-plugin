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
public class EasyContributor {
    protected final String tipWord;
    protected final String insertWord;
    protected final boolean blockCode;

    public EasyContributor( @NotNull String insertWord,@NotNull String tipWord, boolean blockCode){

        this.insertWord = insertWord;
        this.tipWord = tipWord;
        this.blockCode = blockCode;
    }

    public String getTipWord() {
        return tipWord;
    }

    public void insertString(InsertionContext context, Collection<QueryType> queries, boolean failBracket){
        Document document = context.getDocument();
        int insertPosition = context.getSelectionEndOffset();
        int wordBackOffset = insertWord.length() - tipWord.length();
        try {

            String lambdaBody = StrUtil.EMPTY;
            int insertTipPosition = insertPosition;
            int backOffset = wordBackOffset;
            if (blockCode) {
                lambdaBody = "{}";
                insertTipPosition = insertPosition - wordBackOffset;
                backOffset = backOffset-1;
            }
            String lambdaExpression =getLambdaBodyExpression(queries,lambdaBody,true);
            int realBackOffset = realBackOffset(backOffset);
            document.insertString(insertTipPosition+wordBackOffset, lambdaExpression);
            insertPosition += lambdaExpression.length();
            if (wordBackOffset!=0) {
                document.deleteString(insertPosition + wordBackOffset, insertPosition);
            }
            insertPosition = insertPosition + realBackOffset;
            context.getEditor().getCaretModel().getCurrentCaret().moveToOffset(insertPosition);
        }catch (Exception ex){
            if(failBracket){
                document.insertString(insertPosition, "()");
                context.getEditor().getCaretModel().getCurrentCaret().moveToOffset(insertPosition - wordBackOffset);
            }
        }
    }

    protected String getLambdaBody(Collection<QueryType> queries,String lambdaBody){
        return lambdaBody;
    }
    protected int realBackOffset(int backOffset){
        return backOffset;
    }
    protected String getLambdaBodyExpression(Collection<QueryType> queries,String lambdaBody,boolean outBracket){
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
        String parameters = queries.stream().map(o -> o.getShortName()).collect(Collectors.joining(", "));
        return leftOutParameterBracket + leftParameterBracket + parameters + rightParameterBracket + " -> " + getLambdaBody(queries,lambdaBody) + rightOutParameterBracket;
    }
}
