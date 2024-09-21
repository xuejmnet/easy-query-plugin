package com.easy.query.plugin.core.contributor;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;

/**
 * create time 2024/1/29 21:37
 * 您可以在开源软件中使用此代码但是请标明出处
 *
 * @author xuejiaming
 */
public class EasyQueryApiCompletionContributor extends CompletionContributor {


    public EasyQueryApiCompletionContributor(){

    }

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        try {
           boolean ktFile =parameters.getOriginalFile() instanceof KtFile;
           if(ktFile){
               new KotlinEasyQueryApiCompletionContributor().fillCompletionVariants(parameters,result);
           }else{
               new JavaEasyQueryApiCompletionContributor().fillCompletionVariants(parameters,result);
           }

        } catch (Exception e) {
            System.out.println(e);
        } finally {
            super.fillCompletionVariants(parameters, result);
        }
    }
}
