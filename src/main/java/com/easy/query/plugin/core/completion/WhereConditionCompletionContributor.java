package com.easy.query.plugin.core.completion;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.EasyQueryElementUtil;
import com.easy.query.plugin.core.util.PsiCommentUtil;
import com.easy.query.plugin.core.util.PsiJavaClassUtil;
import com.easy.query.plugin.core.util.PsiJavaFieldUtil;
import com.easy.query.plugin.core.util.PsiUtil;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.completion.SkipAutopopupInStrings;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * create time 2025/2/24 21:20
 * 文件说明
 *
 * @author xuejiaming
 */
public class WhereConditionCompletionContributor extends CompletionContributor {


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

        boolean easyQueryAnnotation = PsiUtil.isEasyQueryWhereConditionAnnotation(position);
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

        List<String> linkClassFields = Arrays.stream(linkPsiClass.getAllFields())
            .filter(field -> !PsiJavaFieldUtil.ignoreField(field))
            .filter(field -> !EasyQueryElementUtil.hasNavigateAnnotation(field))
            .map(field -> field.getName())
            .collect(Collectors.toList());
        for (String currentClassField : linkClassFields) {
            result.addElement(LookupElementBuilder.create(currentClassField).withIcon(Icons.EQ));
        }


        PrefixMatcher prefixMatcher = result.getPrefixMatcher();
        String annoValue = prefixMatcher.getPrefix();
        List<String> tipFields = getNavigateFields(project, linkPsiClass, annoValue, true, annoValue);
        for (String tipField : tipFields) {
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