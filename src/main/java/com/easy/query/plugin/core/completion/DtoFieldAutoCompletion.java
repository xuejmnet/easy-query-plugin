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

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
        Set<String> dtoFieldNameSet = Arrays.stream(dtoFields).filter(field -> !PsiUtil.fieldIsStatic(field)).map(o -> o.getName()).collect(Collectors.toSet());

        PsiField[] psiFields = linkPsiClass.getAllFields();
        Map<String, PsiField> entityFieldMap = Arrays.stream(psiFields).filter(field -> !PsiUtil.fieldIsStatic(field)).collect(Collectors.toMap(o -> o.getName(), o -> o, (k1, k2) -> k2));
        String dtoSchema = PsiJavaClassUtil.getDtoSchema(topLevelDtoClass);


        boolean appendAllFields = false;
        // 找到 psiFields 不在 dtoFields 中的字段
        for (Map.Entry<String, PsiField> entityFieldKv : entityFieldMap.entrySet()) {
            String fieldName = entityFieldKv.getKey();
            PsiField entityFieldRaw = entityFieldKv.getValue();
            if (!dtoFieldNameSet.contains(fieldName)) {

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
                String psiFieldComment = PsiUtil.getPsiFieldOnlyComment(entityFieldRaw);
                String shortComment = StrUtil.subSufByLength(psiFieldComment, 15);
                // 再添加一个eq:开头的, 进行索引
                LookupElement lookupElementWithEq = PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create("EQ实体字段:" + entityFieldRaw.getName() + " " + shortComment)
                                .withTypeText(entityFieldRaw.getType().getPresentableText())
                                .withInsertHandler((context, item) -> {
                                    PsiField dtoField = PsiJavaFieldUtil.copyAndPureFieldBySchema(entityFieldRaw, dtoSchema);
                                    context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), dtoField.getText());
                                })
                                .withIcon(Icons.EQ),
                        400d);
                result.addElement(lookupElementWithEq);
                appendAllFields = true;
            }
        }
        if (appendAllFields) {
            // 再添加一个eq:开头的, 进行索引
            LookupElement lookupElementWithEq = PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create("EQ实体字段:ALL_FIELDS")
                            .withInsertHandler((context, item) -> {
                                StringBuilder fieldStringBuilder = new StringBuilder();
                                int i = 0;
                                for (Map.Entry<String, PsiField> innerEntityFieldKv : entityFieldMap.entrySet()) {
                                    String fieldName = innerEntityFieldKv.getKey();
                                    PsiField entityFieldRaw = innerEntityFieldKv.getValue();
                                    if (!dtoFieldNameSet.contains(fieldName)) {
                                        if (i != 0) {
                                            fieldStringBuilder.append(System.lineSeparator());
                                        }
                                        i++;
                                        PsiField dtoField = PsiJavaFieldUtil.copyAndPureFieldBySchema(entityFieldRaw, dtoSchema);
                                        fieldStringBuilder.append(dtoField.getText());
                                    }
                                }
                                // 需要将内容中的 \r 替换掉, 否则 win 下会报错 com.intellij.openapi.util.text.StringUtil.assertValidSeparators 将 \r 视作非法字符
                                String newContent = fieldStringBuilder.toString().replaceAll("\r\n", "\n").replaceAll("\r", "\n");
                                context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), newContent);
                            })
                            .withIcon(Icons.EQ),
                    400d);
            result.addElement(lookupElementWithEq);
        }
//        for (PsiField entityFieldRaw : psiFields) {
//            boolean fieldIsStatic = PsiUtil.fieldIsStatic(entityFieldRaw);
//            if(fieldIsStatic){
//                continue;
//            }
//            boolean isExist = false;
////            for (PsiField dtoField : dtoFields) {
////                if (Objects.equals(entityFieldRaw.getName(), dtoField.getName())) {
////                    isExist = true;
////                    break;
////                }
////            }
//            if (!isExist) {
//
//
//            }
//        }

    }


}
