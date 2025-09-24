package com.easy.query.plugin.core.contributor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.contributor.java.EasyAndOrContributor;
import com.easy.query.plugin.core.contributor.java.EasyContributor;
import com.easy.query.plugin.core.contributor.java.EasyGroupContributor;
import com.easy.query.plugin.core.entity.QueryType;
import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.persistent.EasyQueryQueryPluginConfigData;
import com.easy.query.plugin.core.util.GenericTypeParserUtil;
import com.easy.query.plugin.core.util.MyCollectionUtil;
import com.easy.query.plugin.core.util.MyStringUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.PsiUtil;
import com.easy.query.plugin.core.util.VirtualFileUtils;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrefixMatcher;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiReferenceParameterList;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.source.PsiParameterImpl;
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * create time 2024/9/20 08:52
 * 文件说明
 *
 * @author xuejiaming
 */
public class JavaEasyQueryApiCompletionContributor extends BaseEasyQueryApiCompletionContributor {

    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        Editor editor = parameters.getEditor();
        Project project = parameters.getPosition().getProject();

        Document document = editor.getDocument();
        PsiFile psiFile = VirtualFileUtils.getPsiFile(project, document);
        int offset = editor.getCaretModel().getOffset();

        PsiElement originalPosition = parameters.getOriginalPosition();
        if (originalPosition == null) {
            return;
        }

        PrefixMatcher originalPrefixMatcher = result.getPrefixMatcher();
        String inputText = originalPrefixMatcher.getPrefix();
        String inputTextPrefix = document.getText(TextRange.create(originalPosition.getTextOffset() - 1, originalPosition.getTextOffset()));
//        System.out.println("inputText:" + inputText);
//        System.out.println("inputTextPrefix:" + inputTextPrefix);
        if (StrUtil.isBlank(inputText)) {
            if (StrUtil.isNotBlank(inputTextPrefix)) {
                if (Objects.equals(".", inputTextPrefix)) {
                    result.restartCompletionOnAnyPrefixChange();
                } else if (Objects.equals(">", inputTextPrefix) || Objects.equals("<", inputTextPrefix)) {
                    if (matchQueryableMethodNameByCompare(project, parameters.getPosition())) {
                        result = result.withPrefixMatcher(inputTextPrefix);
                        Set<EasyContributor> easyContributors = Objects.equals(">", inputTextPrefix) ? COMPARE_GREATER_METHODS : COMPARE_LESS_METHODS;
                        addCompareCodeTip(result, project, psiFile, offset, easyContributors);
                    }
                } else if (Objects.equals("=", inputTextPrefix)) {
                    inputTextPrefix = document.getText(TextRange.create(originalPosition.getTextOffset() - 2, originalPosition.getTextOffset()));
                    if (Objects.equals(">=", inputTextPrefix) || Objects.equals("<=", inputTextPrefix)) {
                        if (matchQueryableMethodNameByCompare(project, parameters.getPosition())) {
                            result = result.withPrefixMatcher(inputTextPrefix);
                            Set<EasyContributor> easyContributors = Objects.equals(">=", inputTextPrefix) ? COMPARE_GREATER_METHODS : COMPARE_LESS_METHODS;
                            addCompareCodeTip(result, project, psiFile, offset, easyContributors);
                        }
                    } else if (Objects.equals("==", inputTextPrefix)) {
                        if (matchQueryableMethodNameByCompare(project, parameters.getPosition())) {
                            result = result.withPrefixMatcher(inputTextPrefix);
                            addCompareCodeTip(result, project, psiFile, offset, COMPARE_EQUALS_METHODS);
                        }
                    } else if (Objects.equals("!=", inputTextPrefix)) {
                        if (matchQueryableMethodNameByCompare(project, parameters.getPosition())) {
                            result = result.withPrefixMatcher(inputTextPrefix);
                            addCompareCodeTip(result, project, psiFile, offset, COMPARE_NOT_EQUALS_METHODS);
                        }
                    } else if (Objects.equals(")=", inputTextPrefix)) {
                        if (matchQueryableMethodNameByCompare(project, parameters.getPosition())) {
                            result = result.withPrefixMatcher("=");
                            addSetValueCodeTip(result, project, psiFile, offset, SET_VALUE_METHODS);
                        }
                    }
                } else if (Objects.equals("!", inputTextPrefix)) {
                    if (matchQueryableMethodNameByCompare(project, parameters.getPosition())) {
                        result = result.withPrefixMatcher("!=");
                        addCompareCodeTip(result, project, psiFile, offset, COMPARE_NOT_EQUALS_METHODS);
                    }
                }
            }
            return;
        }

        if (".".equals(inputTextPrefix)) {
            String matchApiMethodReturnTypeName = matchApi(parameters.getPosition(), inputText, project);
            if (matchApiMethodReturnTypeName != null) {
                addApiCodeTip(result, project, psiFile, offset, matchApiMethodReturnTypeName);
            } else if (StrUtil.equalsAny(inputText, "like")) {
                addCompareCodeTip(result, project, psiFile, offset, COMPARE_LIKE_METHODS);
            } else if (StrUtil.equalsAny(inputText, "")) {
//                result.restartCompletionOnPrefixChange("like");
                addCompareCodeTip(result, project, psiFile, offset, COMPARE_LIKE_METHODS);
            }else{
                String paramApi = matchParamApi(parameters.getPosition(), inputText, project);
                if (paramApi != null) {
                    addParamApiCodeTip(result, project, psiFile, offset, paramApi);
                }else {

                    result.restartCompletionOnAnyPrefixChange();
                }
            }
            //匹配匿名对象
        } else {

            boolean matchJoin = matchJoin(parameters.getPosition(), inputText);
            if (matchJoin) {
                addJoinCodeTip(result, project, psiFile, offset);
            }
            boolean matchAnonymous = matchAnonymous(parameters.getPosition(), inputText);
            if (matchAnonymous) {
                addAnonymousCodeTip(result, project, psiFile, offset);
            }
            if (PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement().withText("new")).accepts(parameters.getPosition())) {
                System.out.println("new 后面:" + inputText);
            }
        }
    }


    private static final PsiElementPattern.Capture<PsiElement> AFTER_EASY_QUERY_METHOD = PlatformPatterns.psiElement().withParent(PsiReferenceExpression.class);

    private PsiElementPattern.Capture<PsiElement> getAfterMethodPattern() {
        return AFTER_EASY_QUERY_METHOD;
    }

    private String matchApi(PsiElement psiElement, String inputText, Project project) {

        boolean match = API_MATCH_TREE.fstMatch(inputText);
        if (!match) {
            return null;
        }
        boolean accepts = getAfterMethodPattern().accepts(psiElement);
        if (!accepts) {
            return null;
        }
        String queryableMethodName = getQueryableMethodName(psiElement);
        System.out.println("queryableMethodName:" + queryableMethodName);
        if (StrUtil.isBlank(queryableMethodName)) {
            return null;
        }
        if (ENTITY_QUERY_RETURN_TYPE_MATCH.stream().anyMatch(o -> queryableMethodName.startsWith(o))
            || EASY_QUERY_RETURN_TYPE_MATCH.stream().anyMatch(o -> queryableMethodName.startsWith(o))) {
            return queryableMethodName;
        }
        return null;
    }
    private String matchParamApi(PsiElement psiElement, String inputText, Project project) {

        boolean match = API_MATCH_TREE.fstMatch(inputText);
        if (!match) {
            return null;
        }
        boolean accepts = getAfterMethodPattern().accepts(psiElement);
        if (!accepts) {
            return null;
        }

        PsiElement prevSibling = psiElement.getPrevSibling();
        if (prevSibling != null) {
            PsiElement prevSibling1 = prevSibling.getPrevSibling();
            if (prevSibling1 != null) {
                if (prevSibling1.getPrevSibling() instanceof PsiReferenceExpression) {
                    PsiElement resolve = ((PsiReferenceExpression) prevSibling1.getPrevSibling()).resolve();
                    if (resolve instanceof PsiParameter) {
                        PsiType type = ((PsiParameter) resolve).getType();
                        if(type instanceof PsiClassType){
                            PsiClass myClass = ((PsiClassType) type).resolve();
                            if(myClass!=null){

                                PsiClass abstractProxy = JavaPsiFacade.getInstance(project)
                                    .findClass("com.easy.query.core.proxy.AbstractProxyEntity", GlobalSearchScope.allScope(project));
                                if (InheritanceUtil.isInheritorOrSelf(myClass, abstractProxy, true)) {
                                    if ("or".startsWith(inputText)) {
                                        return "or";
                                    }
                                    if ("and".startsWith(inputText)) {
                                        return "and";
                                    }
                                }
                            }

                        }
                    }
                }
            }
        }

        return null;
    }

    private static final PsiElementPattern.Capture<PsiElement> AFTER_EASY_QUERY_JOIN = PlatformPatterns.psiElement().withParent(PsiReferenceExpression.class).withSuperParent(2, PsiExpressionList.class);

    private boolean matchJoin(PsiElement psiElement, String inputText) {
        boolean match = ON_MATCH_TREE.fstMatch(inputText);
        if (!match) {
            return false;
        }
        return AFTER_EASY_QUERY_JOIN.accepts(psiElement);
    }

    private boolean matchAnonymous(PsiElement psiElement, String inputText) {
        boolean match = ANONYMOUS_MATCH_TREE.fstMatch(inputText);
        if (!match) {
            return false;
        }
        try {
            if (!PlatformPatterns.psiElement().afterLeaf(PlatformPatterns.psiElement().withText("new")).accepts(psiElement)) {
                return false;
            }
//            return PsiJavaPatterns.psiElement().withParent(PsiReferenceExpression.class).withSuperParent(2, PsiExpressionList.class).accepts(psiElement);
        } catch (Exception ex) {

        }
        return true;
    }
//
//    private boolean getMethodCallExpression(PsiElement element) {
//        try {
//
//            PsiMethodCallExpression methodCallExpression = getMethodCallExpression0(element);
//            if (methodCallExpression == null) {
//                return false;
//            }
//            PsiType returnType = methodCallExpression.getType();
//            if (returnType == null) {
//                return false;
//            }
//
//            String canonicalText = returnType.getCanonicalText();
//            return canonicalText.contains("Queryable<");
//        } catch (Exception ex) {
//            //抛错无法计算就直接提示
//            return true;
//        }
//    }

    private PsiMethodCallExpression getMethodCallExpression(PsiElement element) {
        PsiElement prevSibling = element.getPrevSibling();
        if (prevSibling == null) {
            return null;
        }
        if (prevSibling instanceof PsiMethodCallExpression) {
            return (PsiMethodCallExpression) prevSibling;
        }
        return getMethodCallExpression(prevSibling);
    }

    private PsiMethodCallExpression getMethodCallExpressionFirstChild(PsiElement element) {
        PsiElement firstChild = element.getFirstChild();
        if (firstChild == null) {
            return null;
        }
        if (firstChild instanceof PsiMethodCallExpression) {
            return (PsiMethodCallExpression) firstChild;
        }
        return getMethodCallExpressionFirstChild(firstChild);
    }

    private PsiMethodCallExpression getMethodCallExpressionByParent(PsiElement element) {
        PsiElement elementParent = element.getParent();
        if (elementParent == null) {
            return null;
        }

        if (elementParent instanceof PsiMethodCallExpression) {
            return (PsiMethodCallExpression) elementParent;
        }
        return getMethodCallExpressionByParent(elementParent);
    }

    private void addJoinCodeTip(@NotNull CompletionResultSet result, Project project, PsiFile psiFile, int offset) {
        try {
            CompletionResultSet completionResultSet = result.caseInsensitive();
            for (EasyContributor easyContributor : ON_METHODS) {
                LookupElementBuilder elementBuilder = LookupElementBuilder.create(easyContributor.getTipWord())
                    .withTypeText("EasyQueryPlugin" + easyContributor.getDesc(), true)
                    .withInsertHandler((context, item) -> {

                        try {
                            PsiElement elementAt = psiFile.findElementAt(offset);
                            if (elementAt == null) {
                                return;
                            }
                            PsiMethodCallExpression psiMethodCallExpression = getMethodCallExpressionByParent(elementAt);
                            if (psiMethodCallExpression == null) {
                                return;
                            }
                            PsiReferenceExpression methodExpression = psiMethodCallExpression.getMethodExpression();
                            PsiElement lastChild = methodExpression.getLastChild();
                            if (lastChild == null) {
                                return;
                            }
                            String joinText = lastChild.getText();
                            if (!JOIN_METHODS.contains(joinText)) {
                                return;
                            }
                            PsiExpression qualifierExpression = methodExpression.getQualifierExpression();
                            if (qualifierExpression == null) {
                                return;
                            }
                            PsiType returnType = qualifierExpression.getType();
                            if (returnType == null) {
                                return;
                            }
                            String canonicalText = returnType.getCanonicalText();
                            List<QueryType> queryTypes = parseQueryable(project, canonicalText);
                            if (CollUtil.isEmpty(queryTypes)) {
                                return;
                            }
                            QueryType joinFirstParameterType = getJoinFirstParameterType(project, queryTypes.size(), queryTypes.size() + 1, psiMethodCallExpression);
                            queryTypes.add(joinFirstParameterType);

                            easyContributor.insertString(context, queryTypes, false);
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                        }
                    }).withIcon(Icons.EQ);
                completionResultSet.addElement(elementBuilder);
            }
        } catch (Exception e) {
        }
    }

    private void addCompareCodeTip(@NotNull CompletionResultSet result, Project project, PsiFile psiFile, int offset, Set<EasyContributor> compareMethods) {
        try {
            CompletionResultSet completionResultSet = result.caseInsensitive();
            for (EasyContributor easyContributor : compareMethods) {
                LookupElementBuilder elementBuilder = LookupElementBuilder.create(easyContributor.getTipWord())
                    .withTypeText("EasyQueryPlugin" + easyContributor.getDesc(), true)
                    .withInsertHandler((context, item) -> {

                        try {
//                            PsiElement elementAt = psiFile.findElementAt(offset);
//                            if (elementAt == null) {
//                                return;
//                            }
//                            PsiMethodCallExpression psiMethodCallExpression = getMethodCallExpressionByParent(elementAt);
//                            if (psiMethodCallExpression == null) {
//                                return;
//                            }
//                            PsiReferenceExpression methodExpression = psiMethodCallExpression.getMethodExpression();
//                            PsiElement lastChild = methodExpression.getLastChild();
//                            if (lastChild == null) {
//                                return;
//                            }
//                            String joinText = lastChild.getText();
//                            if (!PREDICATE_METHODS.contains(joinText)) {
//                                return;
//                            }
                            easyContributor.insertString(context, Collections.emptyList(), false);
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                        }
                    }).withIcon(Icons.EQ);
                completionResultSet.addElement(elementBuilder);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addSetValueCodeTip(@NotNull CompletionResultSet result, Project project, PsiFile psiFile, int offset, Set<EasyContributor> compareMethods) {
        try {
            CompletionResultSet completionResultSet = result.caseInsensitive();
            for (EasyContributor easyContributor : compareMethods) {
                LookupElementBuilder elementBuilder = LookupElementBuilder.create(easyContributor.getTipWord())
                    .withTypeText("EasyQueryPlugin" + easyContributor.getDesc(), true)
                    .withInsertHandler((context, item) -> {

                        try {
                            PsiElement elementAt = psiFile.findElementAt(offset);
                            if (elementAt == null) {
                                return;
                            }
                            PsiMethodCallExpression psiMethodCallExpression = getMethodCallExpressionByParent(elementAt);
                            if (psiMethodCallExpression == null) {
                                return;
                            }
                            PsiReferenceExpression methodExpression = psiMethodCallExpression.getMethodExpression();
                            PsiElement lastChild = methodExpression.getLastChild();
                            if (lastChild == null) {
                                return;
                            }
                            String childText = lastChild.getText();
                            if (!"select".contains(childText) && !"setColumns".contains(childText) && !"adapter".contains(childText)) {
                                return;
                            }
                            easyContributor.insertString(context, Collections.emptyList(), false);
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                        }
                    }).withIcon(Icons.EQ);
                completionResultSet.addElement(elementBuilder);
            }
        } catch (Exception e) {
        }
    }

    private void addAnonymousCodeTip(@NotNull CompletionResultSet result, Project project, PsiFile psiFile, int offset) {
        try {
            CompletionResultSet completionResultSet = result.caseInsensitive();
            for (EasyContributor easyContributor : ANONYMOUS_METHODS) {
                LookupElementBuilder elementBuilder = LookupElementBuilder.create(easyContributor.getTipWord())
                    .withTypeText("EasyQueryPlugin" + easyContributor.getDesc(), true)
                    .withInsertHandler((context, item) -> {

                        try {

                            PsiElement elementAt = psiFile.findElementAt(offset);
                            if (elementAt == null) {
                                return;
                            }
                            PsiMethodCallExpression psiMethodCallExpression = getMethodCallExpressionByParent(elementAt);
                            if (psiMethodCallExpression == null) {
                                return;
                            }
                            PsiReferenceExpression methodExpression = psiMethodCallExpression.getMethodExpression();
                            PsiElement lastChild = methodExpression.getLastChild();
                            if (lastChild == null) {
                                return;
                            }
                            String joinText = lastChild.getText();
                            if (!"select".contains(joinText)) {
                                return;
                            }
                            PsiExpression qualifierExpression = methodExpression.getQualifierExpression();
                            if (qualifierExpression == null) {
                                return;
                            }
                            PsiType returnType = qualifierExpression.getType();
                            if (returnType == null) {
                                return;
                            }
                            String canonicalText = returnType.getCanonicalText();
                            if (!isQueryable(canonicalText)) {
                                return;
                            }
                            easyContributor.insertString(context, Collections.emptyList(), false);
                        } catch (Exception ex) {
                            System.out.println(ex.getMessage());
                        }
                    }).withIcon(Icons.EQ);
                completionResultSet.addElement(elementBuilder);
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    private QueryType getJoinFirstParameterType(Project project, int index, int total, PsiMethodCallExpression psiMethodCallExpression) {
        PsiType[] expressionTypes = psiMethodCallExpression.getArgumentList().getExpressionTypes();
        if (expressionTypes.length > 1) {
            PsiType expressionType = expressionTypes[0];
            if (expressionType != null) {

                Map<Integer, List<String>> matchNames = getMatchNames(project);
                String canonicalText = expressionType.getCanonicalText();
                String genericType = PsiUtil.parseGenericType(canonicalText);
                return new QueryType(getShortName(project, index, total, genericType, matchNames));
            }
        }
        return new QueryType("obj");
    }

//    private boolean matchQueryableMethodName(PsiElement psiElement) {
//        if (psiElement == null) {
//            return false;
//        }
//        String queryableMethodName = getQueryableMethodName(psiElement);
//        if (StrUtil.isBlank(queryableMethodName)) {

//            return false;
//        }
//        return queryableMethodName.startsWith(QUERYABLE_ENTITY) || queryableMethodName.startsWith(QUERYABLE_CLIENT) || queryableMethodName.startsWith(QUERYABLE_4J) || queryableMethodName.startsWith(QUERYABLE_4KT);
//    }

    private String getQueryableMethodName(PsiElement psiElement) {
        if (psiElement == null) {
            return null;
        }

        PsiMethodCallExpression psiMethodCallExpression = getMethodCallExpression(psiElement);
        if (psiMethodCallExpression == null) {
            psiMethodCallExpression = getMethodCallExpressionFirstChild(psiElement);
            if (psiMethodCallExpression == null) {
                return null;
            }
        }
        PsiType returnType = psiMethodCallExpression.getType();
        if (returnType == null) {
            return null;
        }
        return returnType.getCanonicalText();
    }

    private boolean matchQueryableMethodNameByCompare(Project project, PsiElement psiElement) {
        if (psiElement == null) {
            return false;
        }
        //((PsiReferenceExpression)parameters.getPosition().getParent().getParent().getFirstChild()).getType().getCanonicalText()
        PsiElement parent = psiElement.getParent();
        if (parent == null) {
            return false;
        }
        PsiElement firstChild = parent.getParent().getFirstChild();
        if (firstChild == null) {
            return false;
        }
        if (firstChild instanceof PsiMethodCallExpression) {
            PsiMethodCallExpression methodCallExpression = (PsiMethodCallExpression) firstChild;
            PsiType type = methodCallExpression.getType();
            if (type != null) {
                PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass("com.easy.query.core.proxy.TablePropColumn", GlobalSearchScope.allScope(project));
                if (psiClass != null) {
                    PsiType tablePropColumnType = JavaPsiFacade.getElementFactory(project).createType(psiClass);
                    return tablePropColumnType.isAssignableFrom(type);
                }
            } else {
                //.getParent().getParent().getParent().getText()
                PsiElement parent1 = parent.getParent().getParent();
                if (parent1 != null) {
                    PsiElement parent2 = parent1.getParent();
                    if (parent2 != null) {
                        PsiElement parent3 = parent2.getParent();
                        if (parent3 != null) {
                            String text = parent3.getText();
                            if (text.contains(".leftJoin(") || text.contains(".rightJoin(") || text.contains(".innerJoin(")) {
                                return true;
                            }
                        }
                    }
                }
            }
        } else if (firstChild instanceof PsiReferenceExpression) {
            PsiReferenceExpression psiReferenceExpression = (PsiReferenceExpression) firstChild;
            PsiType type = psiReferenceExpression.getType();
            if (type != null) {
                String canonicalText = type.getCanonicalText();
                if (
                    canonicalText.startsWith("com.easy.query.core.expression.parser.core.base.WherePredicate<") ||
                        canonicalText.startsWith("com.easy.query.core.expression.parser.core.base.WhereAggregatePredicate<") ||
                        canonicalText.startsWith("com.easy.query.api4j.sql.SQLWherePredicate<") ||
                        canonicalText.startsWith("com.easy.query.api4j.sql.SQLWhereAggregatePredicate<") ||
                        canonicalText.startsWith("com.easy.query.api4kt.sql.SQLKtWherePredicate<") ||
                        canonicalText.startsWith("com.easy.query.api4kt.sql.SQLKtWhereAggregatePredicate<")) {
                    return true;
                } else {
                    PsiElement parent1 = parent.getParent().getParent();
                    if (parent1 != null) {
                        PsiElement parent2 = parent1.getParent();
                        if (parent2 != null) {
                            PsiElement parent3 = parent2.getParent();
                            if (parent3 != null) {
                                String text = parent3.getText();
                                if (text.contains(".leftJoin(") || text.contains(".rightJoin(") || text.contains(".innerJoin(")) {
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    //    private boolean isExtendsFromTablePropColumn(PsiType psiClassType){
//        for (PsiType superType : psiClassType.getSuperTypes()) {
//            if(Objects.equals("com.easy.query.core.proxy.TablePropColumn",superType.getCanonicalText())){
//                return true;
//            }else{
//                return isExtendsFromTablePropColumn(superType);
//            }
//        }
//        return false;
//    }
    private void addApiCodeTip(@NotNull CompletionResultSet result, Project project, PsiFile psiFile, int offset, String beforeMethodReturnTypeName) {
        try {

            // 获取忽略大小写的结果集
            CompletionResultSet completionResultSet = result.caseInsensitive();
            for (EasyContributor easyContributor : API_METHODS) {
                if (!easyContributor.accept(beforeMethodReturnTypeName)) {
                    continue;
                }
                LookupElementBuilder apiPlugin = LookupElementBuilder.create(easyContributor.getTipWord())
                    .withTypeText("EasyQueryPlugin" + easyContributor.getDesc(), true)
                    .withInsertHandler((context, item) -> {

                        try {

                            PsiElement elementAt = psiFile.findElementAt(offset);
                            if (elementAt == null) {
                                return;
                            }
                            if (easyContributor instanceof EasyAndOrContributor) {

                                easyContributor.insertString(context, Collections.emptyList(), true);
                                return;
                            }

                            String queryableMethodName = getQueryableMethodName(elementAt);
                            List<QueryType> queryTypes = parseQueryable(project, queryableMethodName);
                            if (CollUtil.isEmpty(queryTypes)) {
                                return;
                            }
                            easyContributor.insertString(context, queryTypes, true);

                            if (easyContributor instanceof EasyGroupContributor) {

                                PsiClass newClass = JavaPsiFacade.getInstance(project).findClass("com.easy.query.core.proxy.sql.GroupKeys", GlobalSearchScope.allScope(project));
                                if (newClass != null) {
                                    WriteCommandAction.runWriteCommandAction(project, () -> {
                                        if (psiFile instanceof PsiJavaFile) {
                                            javaImport(project, (PsiJavaFile) psiFile, newClass);
                                        }
                                    });
                                }

                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    })
                    .withIcon(Icons.EQ);

                completionResultSet.addElement(PrioritizedLookupElement.withPriority(apiPlugin, 100 - 1));

            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
    private void addParamApiCodeTip(@NotNull CompletionResultSet result, Project project, PsiFile psiFile, int offset, String beforeMethodReturnTypeName) {
        try {

            // 获取忽略大小写的结果集
            CompletionResultSet completionResultSet = result.caseInsensitive();
            for (EasyContributor easyContributor : PARAM_API_METHODS) {
                if (!easyContributor.accept(beforeMethodReturnTypeName)) {
                    continue;
                }
                LookupElementBuilder apiPlugin = LookupElementBuilder.create(easyContributor.getTipWord())
                    .withTypeText("EasyQueryPlugin" + easyContributor.getDesc(), true)
                    .withInsertHandler((context, item) -> {

                        try {

                            PsiElement elementAt = psiFile.findElementAt(offset);
                            if (elementAt == null) {
                                return;
                            }
                            easyContributor.insertString(context, Collections.emptyList(), true);
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    })
                    .withIcon(Icons.EQ);

                completionResultSet.addElement(PrioritizedLookupElement.withPriority(apiPlugin, 100 - 1));

            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void addApiCodeTip(@NotNull CompletionResultSet result, Project project, PsiFile psiFile, int offset) {
        try {

            // 获取忽略大小写的结果集
            CompletionResultSet completionResultSet = result.caseInsensitive();
            for (EasyContributor easyContributor : API_METHODS) {
                LookupElementBuilder apiPlugin = LookupElementBuilder.create(easyContributor.getTipWord())
                    .withTypeText("EasyQueryPlugin" + easyContributor.getDesc(), true)
                    .withInsertHandler((context, item) -> {

                        try {

                            PsiElement elementAt = psiFile.findElementAt(offset);
                            if (elementAt == null) {
                                return;
                            }

                            String queryableMethodName = getQueryableMethodName(elementAt);
                            List<QueryType> queryTypes = parseQueryable(project, queryableMethodName);
                            if (CollUtil.isEmpty(queryTypes)) {
                                return;
                            }
                            easyContributor.insertString(context, queryTypes, true);

                            if (easyContributor instanceof EasyGroupContributor) {

                                PsiClass newClass = JavaPsiFacade.getInstance(project).findClass("com.easy.query.core.proxy.sql.GroupKeys", GlobalSearchScope.allScope(project));
                                if (newClass != null) {
                                    WriteCommandAction.runWriteCommandAction(project, () -> {
                                        if (psiFile instanceof PsiJavaFile) {
                                            javaImport(project, (PsiJavaFile) psiFile, newClass);
                                        }
                                    });
                                }

                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                        }
                    })
                    .withIcon(Icons.EQ);

                completionResultSet.addElement(PrioritizedLookupElement.withPriority(apiPlugin, 100 - 1));

            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    public void javaImport(Project project, PsiJavaFile psiJavaFile, PsiClass psiClass) {
        Set<String> importSet = PsiJavaFileUtil.getImportSet(psiJavaFile);
        PsiImportStatement importStatement = PsiElementFactory.getInstance(project).createImportStatement(psiClass);
        // 如果已经导入了，就不再导入
        if (importSet.contains(importStatement.getText())) {
            return;
        }
        psiJavaFile.getImportList().add(importStatement);
    }

//    private static final String QUERYABLE_CLIENT = "com.easy.query.core.basic.api.select.ClientQueryable";
//    private static final String QUERYABLE_4J = "com.easy.query.api4j.select.Queryable";
//    private static final String QUERYABLE_4KT = "com.easy.query.api4kt.select.KtQueryable";

    private static final List<String> ENTITY_QUERY_RETURN_TYPE_MATCH = Arrays.asList(
        "com.easy.query.api.proxy.entity.select.EntityQueryable",
        "com.easy.query.api.proxy.entity.update.ExpressionUpdatable",
        "com.easy.query.api.proxy.entity.update.EntityUpdatable",
        "com.easy.query.api.proxy.entity.delete.ExpressionDeletable"
    );
    private static final List<String> EASY_QUERY_RETURN_TYPE_MATCH = Arrays.asList(
        "com.easy.query.core.basic.api.select.ClientQueryable",
        "com.easy.query.api4j.select.Queryable",
        "com.easy.query.api4kt.select.KtQueryable",
        "com.easy.query.api4j.update.EntityUpdatable",
        "com.easy.query.api4j.delete.ExpressionDeletable",
        "com.easy.query.api4kt.update.KtEntityUpdatable",
        "com.easy.query.api4kt.delete.KtExpressionDeletable",
        "com.easy.query.core.basic.api.update.ClientEntityUpdatable",
        "com.easy.query.core.basic.api.delete.ClientExpressionDeletable"
    );
    private static final String QUERYABLE_END = ">";

    //获取注解没有就返回单一对象o有就用他的
    private List<QueryType> parseQueryable(Project project, String queryable) {

        if (ENTITY_QUERY_RETURN_TYPE_MATCH.stream().anyMatch(o -> queryable.startsWith(o))) {

            //com.easy.query.api.proxy.entity.select.EntityQueryable<org.example.entity.proxy.VCTable,org.example.entity.ValueCompany>
            if (queryable.contains("<") && queryable.endsWith(">")) {
                String replaceQueryable = StrUtil.subBefore(queryable, "<", false) + "<";
                String typeString = queryable.replaceAll(replaceQueryable, "").replaceAll(QUERYABLE_END, "");
                if (typeString.startsWith("com.easy.query.core.proxy.grouping.proxy.Grouping")) {
                    ArrayList<QueryType> objects = new ArrayList<>();
                    objects.add(new QueryType("group", true));
                    return objects;
                }
//                if (typeString.startsWith("com.easy.query.core.proxy.core.draft.proxy.Draft")) {
//                    ArrayList<QueryType> objects = new ArrayList<>();
//                    objects.add(new QueryType("draft", false));
//                    return objects;
//                }

                GenericTypeParserUtil.ParsedType parsedType = GenericTypeParserUtil.parseGenericType(queryable);

                List<List<String>> types = MyCollectionUtil.partition(parsedType.typeArguments, 2);

                Map<Integer, List<String>> matchNames = getMatchNames(project);

                List<QueryType> shortNames = new ArrayList<>(types.size());
                for (int i = 0; i < types.size(); i++) {
                    List<String> typeList = types.get(i);
                    if (typeList.size() < 2) {
                        shortNames.add(new QueryType("unknown" + (i + 1)));
                    }
                    String classFullName = typeList.get(1);
                    if (StrUtil.isBlank(classFullName)) {
                        shortNames.add(new QueryType("unknown" + (i + 1)));
                    } else {
                        shortNames.add(new QueryType(getShortName(project, i, types.size(), classFullName, matchNames)));
                    }
                }
                return shortNames;
            }
        }
        if (EASY_QUERY_RETURN_TYPE_MATCH.stream().anyMatch(o -> queryable.startsWith(o))) {
            if (queryable.contains("<") && queryable.endsWith(">")) {
                String replaceQueryable = StrUtil.subBefore(queryable, "<", false) + "<";
                String typeString = queryable.replaceAll(replaceQueryable, "").replaceAll(QUERYABLE_END, "");
                List<String> types = StrUtil.split(typeString, ",");

                Map<Integer, List<String>> matchNames = getMatchNames(project);
                List<QueryType> shortNames = new ArrayList<>(types.size());
                for (int i = 0; i < types.size(); i++) {
                    String classFullName = types.get(i);
                    if (StrUtil.isBlank(classFullName)) {
                        shortNames.add(new QueryType("unknown" + (i + 1)));
                    } else {
                        shortNames.add(new QueryType(getShortName(project, i, types.size(), classFullName, matchNames)));
                    }
                }
                return shortNames;
            }
        }
        return Collections.emptyList();
    }

    private boolean isQueryable(String queryable) {

//        if (queryable.startsWith(QUERYABLE_ENTITY)) {
//
//            //com.easy.query.api.proxy.entity.select.EntityQueryable<org.example.entity.proxy.VCTable,org.example.entity.ValueCompany>
//            if (queryable.contains("<") && queryable.endsWith(">")) {
//                return true;
//            }
//        }
        if (ENTITY_QUERY_RETURN_TYPE_MATCH.stream().anyMatch(o -> queryable.startsWith(o)) || EASY_QUERY_RETURN_TYPE_MATCH.stream().anyMatch(o -> queryable.startsWith(o))) {
            if (queryable.contains("<") && queryable.endsWith(">")) {
                return true;
            }
        }
        return false;
    }

    private String getShortName(Project project, int index, int total, String fullClassName, Map<Integer, List<String>> matchNames) {
        //com.easy.query.core.annotation
        PsiClass newClass = findClass(project, fullClassName);
        if (newClass != null) {
            PsiAnnotation easyAlias = newClass.getAnnotation("com.easy.query.core.annotation.EasyAlias");
            if (easyAlias != null) {
                String easyAliasName = PsiUtil.getPsiAnnotationValueIfEmpty(easyAlias, "value", "");
                if (StrUtil.isNotBlank(easyAliasName)) {
                    return easyAliasName;
                }
            }
        }
        if (CollUtil.isNotEmpty(matchNames)) {
            List<String> names = matchNames.get(total);
            if (CollUtil.isNotEmpty(names) && names.size() > index) {
                return names.get(index);
            }
        }
        String removeClassGeneric = StrUtil.subBefore(fullClassName, "<", false);

        String className = StrUtil.subAfter(removeClassGeneric, ".", true);
        if(removeClassGeneric.startsWith("com.easy.query.core.proxy.core.draft.Draft")||removeClassGeneric.startsWith("com.easy.query.core.proxy.core.tuple.Tuple")
            ||removeClassGeneric.startsWith("com.easy.query.core.proxy.part.Part")){
            return className.toLowerCase();
        }
        return MyStringUtil.lambdaShortName(className, index, total);
    }

    private Map<Integer, List<String>> getMatchNames(Project project) {

        EasyQueryConfig allEnvQuickSetting = EasyQueryQueryPluginConfigData.getAllEnvQuickSetting(null);
        if (allEnvQuickSetting != null) {
            Map<String, String> config = allEnvQuickSetting.getConfig();
            if (config != null) {
                String settingVal = config.get(project.getName());
                if (StrUtil.isNotBlank(settingVal)) {
                    String[] shortNames = settingVal.split(",");
                    return Arrays.stream(shortNames).collect(Collectors.toMap(o -> o.split(":").length, o -> StrUtil.split(o, ":")));

                }
            }
        }
        return new HashMap<>(0);
    }

    private PsiClass findClass(Project project, String fullClassName) {
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        PsiClass newClass = javaPsiFacade.findClass(fullClassName, GlobalSearchScope.projectScope(project));
        if (newClass == null) {
            newClass = javaPsiFacade.findClass(fullClassName, GlobalSearchScope.allScope(project));
        }
        return newClass;
    }
}
