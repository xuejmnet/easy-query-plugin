package com.easy.query.plugin.core.completion;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.PsiJavaClassUtil;
import com.easy.query.plugin.core.util.PsiJavaFieldUtil;
import com.easy.query.plugin.core.util.PsiUtil;
import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionInitializationContext;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.completion.SkipAutopopupInStrings;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Conditions;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PlainTextTokenTypes;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiNameValuePair;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.intellij.internal.statistic.eventLog.events.EventFields.Language;

/**
 * create time 2025/2/24 21:20
 * 文件说明
 *
 * @author xuejiaming
 */
public class NavigateCompletionContributor extends CompletionContributor {


    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {

        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }


        PsiElement position = parameters.getPosition();
        Project project = parameters.getEditor().getProject();
        if (Objects.isNull(project)) {
            return;
        }

        if (!SkipAutopopupInStrings.isInStringLiteral(position)) {
            return;
        }

        boolean easyQueryAnnotation = PsiUtil.isEasyQueryNavigateFlatJoinAnnotation(position);
        if (!easyQueryAnnotation) {
            return;
        }
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
        PrefixMatcher prefixMatcher = result.getPrefixMatcher();
        String annoValue = prefixMatcher.getPrefix();
//        PsiField[] psiFields = linkPsiClass.getAllFields();
//        Map<String, PsiField> entityFieldMap = Arrays.stream(psiFields).filter(field -> !PsiUtil.fieldIsStatic(field)&&field.getAnnotation("com.easy.query.core.annotation.Navigate")!=null).collect(Collectors.toMap(o -> o.getName(), o -> o, (k1, k2) -> k2));
//
//        for (Map.Entry<String, PsiField> psiFieldEntry : entityFieldMap.entrySet()) {
//            // 添加建议到结果集
//            result.addElement(LookupElementBuilder.create(psiFieldEntry.getKey()).withIcon(Icons.EQ));
//        }
        List<String> tipFields = getNavigateFields(project, linkPsiClass, annoValue, true, annoValue);
        for (String tipField : tipFields) {
            // 添加建议到结果集
//            if (annoValue.contains(".")) {
//                String prefix = StrUtil.subBefore(annoValue, ".", true);
//            } else {
//                result.addElement(LookupElementBuilder.create(tipField.getName()).withIcon(Icons.EQ));
//            }
            result.addElement(LookupElementBuilder.create(tipField).withIcon(Icons.EQ));
        }
    }

    public List<String> getNavigateFields(Project project, PsiClass linkPsiClass, String annoValue, boolean root, String originalAnnoValue) {
        if (annoValue == null) {
            return new ArrayList<>();
        }
        if (linkPsiClass == null) {
            return new ArrayList<>();
        }
        boolean hasAnnoTable = PsiJavaClassUtil.hasAnnoTable(linkPsiClass);
        if (!hasAnnoTable) {
            return new ArrayList<>();
        }
        PsiField[] psiFields = linkPsiClass.getAllFields();
        if (!annoValue.contains(".")) {

            if (root) {
                ArrayList<String> results = new ArrayList<>();
                List<PsiField> navigateFields = Arrays.stream(psiFields).filter(field -> !PsiUtil.fieldIsStatic(field) && field.getAnnotation("com.easy.query.core.annotation.Navigate") != null).collect(Collectors.toList());
                for (PsiField navigateField : navigateFields) {
                    results.add(navigateField.getName());
                    String psiFieldPropertyType = PsiUtil.getPsiFieldPropertyType(navigateField, true);
                    PsiClass fieldClass = JavaPsiFacade.getInstance(project).findClass(psiFieldPropertyType, GlobalSearchScope.allScope(project));
                    if (fieldClass != null) {
                        PsiField[] fields = fieldClass.getAllFields();
                        for (PsiField field : fields) {
                            if (!PsiUtil.fieldIsStatic(field)) {
                                results.add(navigateField.getName() + "." + field.getName());
                            }
                        }
                    }
                }
                return results;
            } else {
                ArrayList<String> results = new ArrayList<>();
                String prefix = StrUtil.subBefore(originalAnnoValue, ".", true) + ".";
                List<PsiField> navigateFields = Arrays.stream(psiFields).filter(field -> !PsiUtil.fieldIsStatic(field)).collect(Collectors.toList());
                for (PsiField navigateField : navigateFields) {
                    results.add(prefix + navigateField.getName());
                    String psiFieldPropertyType = PsiUtil.getPsiFieldPropertyType(navigateField, true);
                    PsiClass fieldClass = JavaPsiFacade.getInstance(project).findClass(psiFieldPropertyType, GlobalSearchScope.allScope(project));
                    if (fieldClass != null) {
                        PsiField[] fields = fieldClass.getAllFields();
                        for (PsiField field : fields) {
                            if (!PsiUtil.fieldIsStatic(field)) {
                                results.add(prefix + navigateField.getName() + "." + field.getName());
                            }
                        }
                    }
                }
                return results;
            }
        }
        String fieldName = StrUtil.subBefore(annoValue, ".", false);
        PsiField psiField = Arrays.stream(psiFields).filter(field -> Objects.equals(field.getName(), fieldName)).findFirst().orElse(null);
        if (psiField == null) {
            return new ArrayList<>();
        }
        String nextAnnoValue = StrUtil.subAfter(annoValue, ".", false);
        String psiFieldPropertyType = PsiUtil.getPsiFieldPropertyType(psiField, true);
        PsiClass fieldClass = JavaPsiFacade.getInstance(project).findClass(psiFieldPropertyType, GlobalSearchScope.allScope(project));
        return getNavigateFields(project, fieldClass, nextAnnoValue, false, originalAnnoValue);
    }
}