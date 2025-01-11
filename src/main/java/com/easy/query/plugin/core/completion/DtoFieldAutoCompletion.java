package com.easy.query.plugin.core.completion;

import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.PsiJavaClassUtil;
import com.easy.query.plugin.core.util.PsiJavaFieldUtil;
import com.easy.query.plugin.core.util.PsiUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DtoFieldAutoCompletion extends CompletionContributor {

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {

        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }


        PsiElement position = parameters.getPosition();
        PsiClass topLevelDtoClass = PsiTreeUtil.getTopmostParentOfType(position, PsiClass.class);
        if (!PsiJavaClassUtil.isElementRelatedToClass(position)) {
            // 只处理类下面的直接元素, 方法内的不处理
            return;
        }
        PsiClass currentPsiClass = PsiTreeUtil.getParentOfType(position, PsiClass.class);
        if (Objects.isNull(currentPsiClass)) {
            return;
        }

        PsiClass linkPsiClass = PsiJavaClassUtil.getLinkPsiClass(currentPsiClass);
        boolean hasAnnoTable = PsiJavaClassUtil.hasAnnoTable(linkPsiClass);
        if (!hasAnnoTable) {
            return;
        }

        PsiField[] dtoFields = currentPsiClass.getAllFields();
        PsiField[] psiFields = linkPsiClass.getAllFields();

        String dtoSchema = PsiJavaClassUtil.getDtoSchema(topLevelDtoClass);


        // 找到 psiFields 不在 dtoFields 中的字段
        for (PsiField entityFieldRaw : psiFields) {
            boolean isExist = false;
            for (PsiField dtoField : dtoFields) {
                if (Objects.equals(entityFieldRaw.getName(), dtoField.getName())) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) {


                LookupElement lookupElementWithoutEq = PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create(entityFieldRaw.getName())
                                .withTypeText(entityFieldRaw.getType().getPresentableText())
                                .withInsertHandler((context, item) -> {

                                    PsiField dtoField = PsiJavaFieldUtil.copyAndPureFieldBySchema(entityFieldRaw, dtoSchema);
                                    context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), dtoField.getText());
                                })
                                .withIcon(Icons.EQ),
                        400d);
                result.addElement(lookupElementWithoutEq);
                String psiFieldComment = PsiUtil.getPsiFieldClearComment(entityFieldRaw);
                String shortComment = StrUtil.subSufByLength(psiFieldComment, 15);
                // 再添加一个eq:开头的, 进行索引
                LookupElement lookupElementWithEq = PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create("EQ实体字段:" + entityFieldRaw.getName()+" "+shortComment)
                                .withTypeText(entityFieldRaw.getType().getPresentableText())
                                .withInsertHandler((context, item) -> {
                                    PsiField dtoField = PsiJavaFieldUtil.copyAndPureFieldBySchema(entityFieldRaw, dtoSchema);
                                    context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), dtoField.getText());
                                })
                                .withIcon(Icons.EQ),
                        400d);
                result.addElement(lookupElementWithEq);
            }
        }

    }


}
