//package com.easy.query.plugin.core.completion;
//
//import com.easy.query.plugin.core.util.PsiJavaClassUtil;
//import com.easy.query.plugin.core.util.PsiUtil;
//import com.intellij.codeInsight.completion.CompletionContributor;
//import com.intellij.codeInsight.completion.CompletionParameters;
//import com.intellij.codeInsight.completion.CompletionProvider;
//import com.intellij.codeInsight.completion.CompletionResultSet;
//import com.intellij.codeInsight.completion.CompletionType;
//import com.intellij.codeInsight.lookup.LookupElementBuilder;
//import com.intellij.patterns.PlatformPatterns;
//import com.intellij.psi.PsiAnnotation;
//import com.intellij.psi.PsiClass;
//import com.intellij.psi.PsiElement;
//import com.intellij.psi.PsiField;
//import com.intellij.psi.PsiLiteralExpression;
//import com.intellij.psi.PsiNameValuePair;
//import com.intellij.psi.util.PsiTreeUtil;
//import com.intellij.util.ProcessingContext;
//import org.jetbrains.annotations.NotNull;
//
//import java.util.Arrays;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.stream.Collectors;
//
//import static com.intellij.internal.statistic.eventLog.events.EventFields.Language;
//
///**
// * create time 2025/2/24 21:20
// * 文件说明
// *
// * @author xuejiaming
// */
//public class NavigateCompletionContributor extends CompletionContributor {
//
//    public NavigateCompletionContributor() {
//        extend(CompletionType.BASIC, // Completion type
//            PlatformPatterns.psiElement(), // Ensure we're dealing with a string literal
//            new CompletionProvider<CompletionParameters>() {
//                @Override
//                protected void addCompletions(
//                    @NotNull CompletionParameters parameters,
//                    @NotNull ProcessingContext context,
//                    @NotNull CompletionResultSet result
//                ) {
//                    PsiElement element = parameters.getPosition();
//                    if (isInAnnotationString(element)) {
//                        // Example: Add the predefined values for pathAlias
//                        result.addElement(LookupElementBuilder.create("value1"));
//                        result.addElement(LookupElementBuilder.create("value2"));
//                        result.addElement(LookupElementBuilder.create("value3"));
//                    }
//                }
//            });
//    }
//
//    // Helper method to check if the cursor is in the annotation's string parameter
//    private boolean isInAnnotationString(PsiElement element) {
//        // Check if the element is inside a string literal and is part of an annotation argument
//        PsiElement parent = element.getParent();
//        if (parent instanceof PsiLiteralExpression) {
//            // Ensure that the string is inside a method parameter in an annotation
//            PsiElement grandParent = parent.getParent();
//            if (grandParent != null && grandParent.getText().startsWith("@NavigateFlat")) {
//                return true;
//            }
//        }
//        return false;
//    }
////    @Override
////    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
////
////        if (parameters.getCompletionType() != CompletionType.BASIC) {
////            return;
////        }
////
////
////        PsiElement position = parameters.getPosition();
////        PsiClass topLevelDtoClass = PsiTreeUtil.getTopmostParentOfType(position, PsiClass.class);
////        if (!PsiJavaClassUtil.isElementRelatedToClass(position)) {
////            // 只处理类下面的直接元素, 方法内的不处理
////            return;
////        }
////        PsiClass currentPsiClass = PsiTreeUtil.getParentOfType(position, PsiClass.class);
////        if (Objects.isNull(currentPsiClass)) {
////            return;
////        }
////
////        PsiClass linkPsiClass = PsiJavaClassUtil.getLinkPsiClass(currentPsiClass);
////        boolean hasAnnoTable = PsiJavaClassUtil.hasAnnoTable(linkPsiClass);
////        if (!hasAnnoTable) {
////            return;
////        }
////
////        PsiField[] psiFields = linkPsiClass.getAllFields();
////        Map<String, PsiField> entityFieldMap = Arrays.stream(psiFields).filter(field -> !PsiUtil.fieldIsStatic(field)).collect(Collectors.toMap(o -> o.getName(), o -> o, (k1, k2) -> k2));
////
////        for (Map.Entry<String, PsiField> psiFieldEntry : entityFieldMap.entrySet()) {
////            // 添加建议到结果集
////            result.addElement(LookupElementBuilder.create(psiFieldEntry.getKey()));
////        }
////    }
//}