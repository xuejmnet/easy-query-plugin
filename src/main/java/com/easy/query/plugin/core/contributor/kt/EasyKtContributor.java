package com.easy.query.plugin.core.contributor.kt;

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
public class EasyKtContributor {
    protected final String tipWord;
    protected final String insertWord;

    public EasyKtContributor(@NotNull String insertWord, @NotNull String tipWord) {

        this.insertWord = insertWord;
        this.tipWord = tipWord;
    }

    public String getTipWord() {
        return tipWord;
    }

    public void insertString(InsertionContext context, Collection<QueryType> queries, boolean failBracket) {
        Document document = context.getDocument();
        int insertPosition = context.getSelectionEndOffset();
        int wordBackOffset = insertWord.length() - tipWord.length();
        try {

            String lambdaBody = StrUtil.EMPTY;
            String lambdaExpression = getLambdaBodyExpression(queries, lambdaBody, true);
            int realBackOffset = realBackOffset(wordBackOffset);
            document.insertString(insertPosition + wordBackOffset, lambdaExpression);
            insertPosition += lambdaExpression.length();
            deleteString(document, insertPosition, wordBackOffset);
            insertPosition = insertPosition + realBackOffset;
            context.getEditor().getCaretModel().getCurrentCaret().moveToOffset(insertPosition);
        } catch (Exception ex) {
            if (failBracket) {
                document.insertString(insertPosition, "()");
                context.getEditor().getCaretModel().getCurrentCaret().moveToOffset(insertPosition - wordBackOffset);
            }
        }
    }

    protected void deleteString(Document document, int insertPosition, int wordBackOffset) {
        if (wordBackOffset < 0) {
            document.deleteString(insertPosition + wordBackOffset, insertPosition);
        }
    }

    protected String getLambdaBody(Collection<QueryType> queries, String lambdaBody) {
        return lambdaBody;
    }

    protected int realBackOffset(int backOffset) {
//        if (blockCode) {
//            return backOffset - 2;
//        }
        return backOffset - 1;
    }

    protected String getLambdaBodyExpression(Collection<QueryType> queries, String lambdaBody, boolean outBracket) {
        String leftOutParameterBracket = StrUtil.EMPTY;
        String rightOutParameterBracket = StrUtil.EMPTY;
        if (outBracket) {
            leftOutParameterBracket = " {";
            rightOutParameterBracket = "}";
        }
        QueryType queryType = queries.stream().findFirst().orElse(null);
        boolean group = queryType != null && queryType.isGroup();
        if (group) {
            return leftOutParameterBracket + " return " + getLambdaBody(queries, lambdaBody) + rightOutParameterBracket;
        } else {
            if (queries.size() == 1) {
                return leftOutParameterBracket + rightOutParameterBracket;
            } else {
                String parameters = queries.stream().map(o -> o.getShortName()).collect(Collectors.joining(", "));
                return leftOutParameterBracket + parameters + " -> " + getLambdaBody(queries, lambdaBody) + rightOutParameterBracket;
            }

        }
    }

    public boolean accept(String beforeMethodReturnTypeName) {
        return true;
    }
    public String getDesc(){
        return "";
    }
}
