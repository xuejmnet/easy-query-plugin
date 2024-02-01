package com.easy.query.plugin.core.contributor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.AnonymousParseContext;
import com.easy.query.plugin.core.entity.QueryType;
import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.MyCollectionUtil;
import com.easy.query.plugin.core.util.MyStringUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.PsiUtil;
import com.easy.query.plugin.core.util.TrieTree;
import com.easy.query.plugin.core.util.VirtualFileUtils;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiExpressionStatement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * create time 2024/1/29 21:37
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyQueryApiCompletionContributor extends CompletionContributor {

    private static final Set<EasyContributor> API_METHODS = new HashSet<>(Arrays.asList(
            new EasyContributor("select", "select", false),
            new EasyContributor("where", "where", false),
            new EasyContributor("where", "where_code_block", true),
            new EasyContributor("orderBy", "orderBy", false),
            new EasyContributor("orderBy", "orderBy_code_block", true),
            new EasyGroupContributor("groupBy", "groupBy", false),
            new EasyContributor("having", "having", false),
            new EasyContributor("having", "having_code_block", true)));
    private static final TrieTree API_MATCH_TREE = new TrieTree(API_METHODS.stream().map(o -> o.getTipWord()).collect(Collectors.toList()));
    private static final Set<EasyContributor> ON_METHODS = new HashSet<>(Arrays.asList(
            new EasyOnContributor("", "on", false),
            new EasyOnContributor("", "on_code_block", true)));
    private static final TrieTree ON_MATCH_TREE = new TrieTree(ON_METHODS.stream().map(o -> o.getTipWord()).collect(Collectors.toList()));
    private static final Set<String> JOIN_METHODS = new HashSet<>(Arrays.asList("leftJoin", "rightJoin", "innerJoin"));


    private static final Set<EasyContributor> ANONYMOUS_METHODS = new HashSet<>(Arrays.asList(
            new EasyAnonymousContributor("", "anonymous", false)));
    private static final TrieTree ANONYMOUS_MATCH_TREE = new TrieTree(ANONYMOUS_METHODS.stream().map(o -> o.getTipWord()).collect(Collectors.toList()));


    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        try {
//            PsiElement originalPosition = parameters.getOriginalPosition();
            Editor editor = parameters.getEditor();
            Project project = parameters.getPosition().getProject();
            Document document = editor.getDocument();
            PsiFile psiFile = VirtualFileUtils.getPsiFile(project, document);
            int offset = editor.getCaretModel().getOffset();
            PsiElement originalPosition = parameters.getOriginalPosition();
            if (originalPosition == null) {
                return;
            }

            String inputText = document.getText(TextRange.create(originalPosition.getTextOffset(), offset));


            boolean matchApi = matchApi(parameters.getOriginalPosition(), inputText);
            if (matchApi) {
                addApiCodeTip(result, project, psiFile, offset);
            } else {
                boolean matchJoin = matchJoin(parameters.getOriginalPosition(), inputText);
                if (matchJoin) {
                    addJoinCodeTip(result, project, psiFile, offset);
                }
            }
//
//            boolean matchAnonymous = matchAnonymous(parameters.getOriginalPosition(), inputText);
//            if (matchAnonymous) {
//                addAnonymousCodeTip(result, project, psiFile, offset);
//            }
//
//            if (StrUtil.isBlank(inputText)||(
//                    document.getText(TextRange.create(originalPosition.getTextOffset()-1, offset)).startsWith(".")
//                    &&
//                    API_METHODS.stream().anyMatch(o->o.startsWith(inputText))
//                    )) {
//                boolean accepts = PsiJavaPatterns.psiElement().withParent(PsiExpressionStatement.class).accepts(parameters.getOriginalPosition());
//                if (accepts) {
//                    addApiCodeTip(result, project, psiFile, offset);
//                }
//            } else {
//                if(ON_METHODS.stream().anyMatch(o->o.startsWith(inputText))){
//
//                    boolean accepts = PsiJavaPatterns.psiElement().withParent(PsiReferenceExpression.class).withSuperParent(2, PsiExpressionList.class).accepts(parameters.getOriginalPosition());
//                    if (accepts) {
//                        addJoinCodeTip(result, project, psiFile, offset);
//                    }
//                }
//            }
            //处理api提示 否则处理内部predicate等处理
//            if (text.contains(".")) {
//                boolean accepts=PsiJavaPatterns.psiElement().withParent(PsiReferenceExpression.class)
//                        .withSuperParent(2,PsiExpressionStatement.class)
//                        .accepts(parameters.getOriginalPosition());
//                if (accepts) {
//
//                    if (StrUtil.isBlank(inputText)) {
//                        boolean accepts=PsiJavaPatterns.psiElement().withParent(PsiReferenceExpression.class)
//                                .withSuperParent(2,PsiExpressionStatement.class)
//                                .accepts(parameters.getOriginalPosition());
//                        if(accepts){
//                            addCodeTip(result, project, psiFile, offset);
//                        }
//                    }
////                    if(alphabet.stream().anyMatch(o -> o.startsWith(inputText))){
////
////                    }
//                }
//            }else{
//                System.out.println("1111");
//
//            }
//            if(prevSibling instanceof PsiExpressionStatement){
//                PsiExpressionStatement expressionStatement = (PsiExpressionStatement) prevSibling;
//                PsiElement firstChild = expressionStatement.getFirstChild();
//                if(firstChild instanceof PsiReferenceExpression){
//                    PsiReferenceExpression referenceExpression = (PsiReferenceExpression) firstChild;
//                    PsiElement target = referenceExpression.resolve();
//                    if (target instanceof PsiClass) {
//                        PsiClass psiClass = (PsiClass) target;
//                        String className = psiClass.getName();
//                        String packageName = psiClass.getQualifiedName();
//                    }
//                }
//            }
//            String text = parameters.getPosition().getText();
//            String inputText = StrUtil.subBefore(text, "IntellijIdeaRulezzz", true);
//            if (StrUtil.isBlank(inputText)) {
//                return;
//            }
//            System.out.println("输入内容：" + inputText);
//            System.out.println("输入内容index：" + offset);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            super.fillCompletionVariants(parameters, result);
        }
    }

    private boolean matchApi(PsiElement psiElement, String inputText) {
        if(StrUtil.isBlank(inputText)){
            return true;
        }
        boolean match =  API_MATCH_TREE.fstMatch(inputText);
        if (!match) {
            return false;
        }
        try {

            return PsiJavaPatterns.psiElement().withParent(PsiReferenceExpression.class).withSuperParent(2, PsiExpressionStatement.class).accepts(psiElement);
        } catch (Exception ex) {
        }
        return true;
    }

    private boolean matchJoin(PsiElement psiElement, String inputText) {
        boolean match = ON_MATCH_TREE.fstMatch(inputText);
        if (!match) {
            return false;
        }
        try {
            return PsiJavaPatterns.psiElement().withParent(PsiReferenceExpression.class).withSuperParent(2, PsiExpressionList.class).accepts(psiElement);
        } catch (Exception ex) {

        }
        return true;
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
            return PsiJavaPatterns.psiElement().withParent(PsiReferenceExpression.class).withSuperParent(2, PsiExpressionList.class).accepts(psiElement);
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
                        .withTypeText("EasyQueryPlugin", true)
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
                                List<QueryType> queryTypes = parseQueryable(canonicalText);
                                if (CollUtil.isEmpty(queryTypes)) {
                                    return;
                                }
                                QueryType joinFirstParameterType = getJoinFirstParameterType(psiMethodCallExpression);
                                queryTypes.add(joinFirstParameterType);

                                easyContributor.insertString(context, queryTypes, false);

                            } catch (Exception ex) {

                            }
                        }).withIcon(Icons.EQ);
                completionResultSet.addElement(elementBuilder);
            }
        }catch (Exception e){}
    }

    private void addAnonymousCodeTip(@NotNull CompletionResultSet result, Project project, PsiFile psiFile, int offset) {
        try {
            CompletionResultSet completionResultSet = result.caseInsensitive();
            for (EasyContributor easyContributor : ANONYMOUS_METHODS) {
                LookupElementBuilder elementBuilder = LookupElementBuilder.create(easyContributor.getTipWord())
                        .withTypeText("EasyQueryPlugin", true)
                        .withInsertHandler((context, item) -> {

                            try {
                                easyContributor.insertString(context, Collections.emptyList(), false);
                            } catch (Exception ex) {

                            }
                        }).withIcon(Icons.EQ);
                completionResultSet.addElement(elementBuilder);
            }
        }catch (Exception ex){

        }
    }

    private QueryType getJoinFirstParameterType(PsiMethodCallExpression psiMethodCallExpression) {
        PsiType[] expressionTypes = psiMethodCallExpression.getArgumentList().getExpressionTypes();
        if (expressionTypes.length > 1) {
            PsiType expressionType = expressionTypes[0];
            if (expressionType != null) {
                String canonicalText = expressionType.getCanonicalText();
                String genericType = PsiUtil.parseGenericType(canonicalText);
                String className = StrUtil.subAfter(genericType, ".", true);
                String shortName = MyStringUtil.lambdaShortName(className);
                return new QueryType(shortName);
            }
        }
        return new QueryType("obj");
    }

    private void addApiCodeTip(@NotNull CompletionResultSet result, Project project, PsiFile psiFile, int offset) {
        try {

            // 获取忽略大小写的结果集
            CompletionResultSet completionResultSet = result.caseInsensitive();
            for (EasyContributor easyContributor : API_METHODS) {
                LookupElementBuilder apiPlugin = LookupElementBuilder.create(easyContributor.getTipWord())
                        .withTypeText("EasyQueryPlugin", true)
                        .withInsertHandler((context, item) -> {

                            try {

                                PsiElement elementAt = psiFile.findElementAt(offset);
                                if (elementAt == null) {
                                    return;
                                }
                                PsiMethodCallExpression psiMethodCallExpression = getMethodCallExpression(elementAt);
                                if (psiMethodCallExpression == null) {
                                    psiMethodCallExpression = getMethodCallExpressionFirstChild(elementAt);
                                    if (psiMethodCallExpression == null) {
                                        return;
                                    }
                                }
                                PsiType returnType = psiMethodCallExpression.getType();
                                if (returnType == null) {
                                    return;
                                }
                                String canonicalText = returnType.getCanonicalText();
                                List<QueryType> queryTypes = parseQueryable(canonicalText);
                                if (CollUtil.isEmpty(queryTypes)) {
                                    return;
                                }
                                easyContributor.insertString(context, queryTypes, true);

                                if(easyContributor instanceof  EasyGroupContributor){

                                    PsiClass newClass = JavaPsiFacade.getInstance(project).findClass("com.easy.query.core.proxy.sql.GroupKeys", GlobalSearchScope.projectScope(project));
                                    if(newClass!=null){
                                        WriteCommandAction.runWriteCommandAction(project, () -> {
                                            if(psiFile instanceof PsiJavaFile){
                                                javaImport(project,(PsiJavaFile)psiFile,newClass);
                                            }
                                        });
                                    }

                                }
                            } catch (Exception e) {
                            }
                        })
                        .withIcon(Icons.EQ);
                completionResultSet.addElement(apiPlugin);

            }
        } catch (Exception ex) {
            System.out.println(ex);
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

    private static final String QUERYABLE_CLIENT = "com.easy.query.core.basic.api.select.ClientQueryable";
    private static final String QUERYABLE_4J = "com.easy.query.api4j.select.Queryable";
    private static final String QUERYABLE_4KT = "com.easy.query.api4kt.select.KtQueryable";
    private static final String QUERYABLE_ENTITY = "com.easy.query.api.proxy.entity.select.EntityQueryable";
    private static final String QUERYABLE_END = ">";

    private List<QueryType> parseQueryable(String queryable) {

        if (queryable.startsWith(QUERYABLE_ENTITY)) {

            //com.easy.query.api.proxy.entity.select.EntityQueryable<org.example.entity.proxy.VCTable,org.example.entity.ValueCompany>
            if (queryable.contains("<") && queryable.endsWith(">")) {
                String replaceQueryable = StrUtil.subBefore(queryable, "<", false) + "<";
                String typeString = queryable.replaceAll(replaceQueryable, "").replaceAll(QUERYABLE_END, "");
                if(typeString.startsWith("com.easy.query.core.proxy.grouping.proxy.Grouping")){
                    return Collections.singletonList(new QueryType("group",true));
                }
                return MyCollectionUtil.partition(StrUtil.split(typeString, ","), 2).stream().map(o -> {
                    if (o.size() < 2) {
                        return null;
                    } else {
                        String classFullName = o.get(1);
                        String className = StrUtil.subAfter(classFullName, ".", true);

                        return new QueryType(MyStringUtil.lambdaShortName(className));
                    }
                }).collect(Collectors.toList());

            }
        }
        if(queryable.startsWith(QUERYABLE_CLIENT) || queryable.startsWith(QUERYABLE_4J) || queryable.startsWith(QUERYABLE_4KT)){
            if (queryable.contains("<") && queryable.endsWith(">")) {
                String replaceQueryable = StrUtil.subBefore(queryable, "<", false) + "<";
                String typeString = queryable.replaceAll(replaceQueryable, "").replaceAll(QUERYABLE_END, "");
                return StrUtil.split(typeString, ",").stream().map(o -> {
                    if (StrUtil.isBlank(o)) {
                        return null;
                    } else {
                        String classFullName = o;
                        String className = StrUtil.subAfter(classFullName, ".", true);

                        return new QueryType(MyStringUtil.lambdaShortName(className));
                    }
                }).collect(Collectors.toList());

            }
        }
        return Collections.emptyList();
    }
}
