package com.easy.query.plugin.core.completion;

import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.IdeaUtil;
import com.easy.query.plugin.core.util.PsiJavaClassUtil;
import com.easy.query.plugin.core.util.PsiJavaFieldUtil;
import com.easy.query.plugin.core.util.PsiUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DtoFieldAutoCompletion extends CompletionContributor {

    private final Pattern pattern = Pattern.compile("private\\s+(?:List<)?(\\w+)(?:>)?\\s+\\w+;");
    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {

        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }


        PsiElement position = parameters.getPosition();
        if (SkipAutopopupInStrings.isInStringLiteral(position)) {
            return;
        }
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

        PsiField[] entityFields = linkPsiClass.getAllFields();
        Map<String, PsiField> entityFieldMap = Arrays.stream(entityFields).filter(field -> !PsiUtil.fieldIsStatic(field)).collect(Collectors.toMap(o -> o.getName(), o -> o, (k1, k2) -> k2));
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
                    LookupElementBuilder.create("eq实体字段:" + entityFieldRaw.getName() + " " + shortComment)
                        .withTypeText(entityFieldRaw.getType().getPresentableText())
                        .withInsertHandler((context, item) -> {
                            PsiField dtoField = PsiJavaFieldUtil.copyAndPureFieldBySchema(entityFieldRaw, dtoSchema);
                            context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), dtoField.getText());
                        })
                        .withIcon(Icons.EQ),
                    400d);
                result.addElement(lookupElementWithEq);
                PsiAnnotation navigateAnnotation = entityFieldRaw.getAnnotation("com.easy.query.core.annotation.Navigate");
                if (navigateAnnotation != null) {

                    // 再添加一个eq:开头的, 进行索引
                    LookupElement lookupElementWithEqInternalClass = PrioritizedLookupElement.withPriority(
                        LookupElementBuilder.create("eq实体字段InternalClass:" + entityFieldRaw.getName() + " " + shortComment)
                            .withTypeText(entityFieldRaw.getType().getPresentableText())
                            .withInsertHandler((context, item) -> {
                                PsiField dtoField = PsiJavaFieldUtil.copyAndPureFieldBySchema(entityFieldRaw, dtoSchema);
                                String text = dtoField.getText();
                                String field = "private "+StrUtil.subAfter(text, "private ", true);
                                String className ="XXXXXXX";
                                Matcher matcher = pattern.matcher(field);
                                if (matcher.find()) {
                                    className = matcher.group(1);
                                }

                                StringBuilder fieldWithInternalClass = new StringBuilder();
                                fieldWithInternalClass.append(text);
                                String newLine = IdeaUtil.lineSeparator();

                                fieldWithInternalClass.append("/**");
                                fieldWithInternalClass.append(newLine);
                                fieldWithInternalClass.append("* {@link }");
                                fieldWithInternalClass.append(newLine);
                                fieldWithInternalClass.append("**/");
                                fieldWithInternalClass.append(newLine);
                                fieldWithInternalClass.append("public static class Internal").append(className).append(" {");
                                fieldWithInternalClass.append(newLine);
                                fieldWithInternalClass.append("}");


                                context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), fieldWithInternalClass.toString());
                            })
                            .withIcon(Icons.EQ),
                        400d);
                    result.addElement(lookupElementWithEqInternalClass);
                }

                appendAllFields = true;
            }
        }
        if (appendAllFields) {
            // 再添加一个eq:开头的, 进行索引
            LookupElement lookupElementWithEq = PrioritizedLookupElement.withPriority(
                LookupElementBuilder.create("eq实体字段:ALL_FIELDS")
                    .withInsertHandler((context, item) -> {
                        StringBuilder fieldStringBuilder = new StringBuilder();

                        for (int i = 0; i < entityFields.length; i++) {
                            PsiField entityField = entityFields[i];
                            if (!entityFieldMap.containsKey(entityField.getName())) {
                                // 不是需要的字段
                                continue;
                            }
                            if (dtoFieldNameSet.contains(entityField.getName())) {
                                // 字段已经有了
                                continue;
                            }
                            if (i != 0) {
                                fieldStringBuilder.append(System.lineSeparator());
                            }
                            PsiField dtoField = PsiJavaFieldUtil.copyAndPureFieldBySchema(entityField, dtoSchema);
                            fieldStringBuilder.append(dtoField.getText());
                        }

                        // 需要将内容中的 \r 替换掉, 否则 win 下会报错 com.intellij.openapi.util.text.StringUtil.assertValidSeparators 将 \r 视作非法字符
                        String newContent = fieldStringBuilder.toString().replaceAll("\r\n", "\n").replaceAll("\r", "\n");
                        context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), newContent);
                    })
                    .withIcon(Icons.EQ),
                400d);
            result.addElement(lookupElementWithEq);
        }
        if (!dtoFieldNameSet.contains("EXTRA_AUTO_INCLUDE_CONFIGURE")) {
            String easyAlias = getEasyAlias(linkPsiClass);
            // 再添加一个eq:开头的, 进行索引
            LookupElement lookupElementWithEq = PrioritizedLookupElement.withPriority(
                LookupElementBuilder.create("eq_extra_auto_include_configure")
                    .withInsertHandler((context, item) -> {
//                        String filedText = String.format("private static final ExtraAutoIncludeConfigure EXTRA_AUTO_INCLUDE_CONFIGURE= %s.TABLE.EXTRA_AUTO_INCLUDE_CONFIGURE();", linkPsiClass.getName() + "Proxy");
                        String filedText = String.format("\n" +
                            "        private static final ExtraAutoIncludeConfigure EXTRA_AUTO_INCLUDE_CONFIGURE = %s.TABLE.EXTRA_AUTO_INCLUDE_CONFIGURE()\n" +
                            "                .where(%s -> {})\n" +
                            "                .select(%s -> Select.of());", linkPsiClass.getName() + "Proxy", easyAlias, easyAlias);

                        context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), filedText);
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

    private String getEasyAlias(PsiClass psiClass) {

        PsiAnnotation easyAlias = psiClass.getAnnotation("com.easy.query.core.annotation.EasyAlias");
        if (easyAlias != null) {
            String easyAliasName = PsiUtil.getPsiAnnotationValueIfEmpty(easyAlias, "value", "");
            if (cn.hutool.core.util.StrUtil.isNotBlank(easyAliasName)) {
                return easyAliasName;
            }
        }
        return "o";
    }


}
