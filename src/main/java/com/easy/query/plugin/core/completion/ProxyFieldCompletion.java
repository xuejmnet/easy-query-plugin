package com.easy.query.plugin.core.completion;

import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.EasyQueryElementUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocToken;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 代理字段自动补全
 *
 * @author link2fun
 */
public class ProxyFieldCompletion extends CompletionContributor {


    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }

        PsiElement position = parameters.getPosition();
        if (!(position.getParent() instanceof PsiReferenceExpression)) {
            return;
        }

        PsiNewExpression newProxyDefine = PsiTreeUtil.findChildOfType(position.getParent(), PsiNewExpression.class);
        if (newProxyDefine == null) {
            return;
        }
        PsiJavaCodeReferenceElement proxyEntityClassReference = newProxyDefine.getClassReference();
        if (proxyEntityClassReference == null) {
            return;
        }
        Project project = position.getProject();
        String proxyEntityClassName = proxyEntityClassReference.getQualifiedName();

//        // 必须是 Proxy 类
//        if (!StrUtil.contains(proxyEntityClassName, ".proxy.")) {
//            return;
//        }

        PsiClass proxyEntityPsiClass = PsiJavaFileUtil.getPsiClass(project, proxyEntityClassName);
// 必须是 Proxy 类
        if (!EasyQueryElementUtil.isExtendAbstractProxyEntity(proxyEntityPsiClass)) {
            return;
        }

        Collection<PsiMethodCallExpression> expressionCollection = PsiTreeUtil.findChildrenOfType(position.getParent(), PsiMethodCallExpression.class);
        List<PsiMethodCallExpression> methodCallExpressionList = expressionCollection.stream().collect(Collectors.toList());

        // 需要从中找出 proxyEntityClassName 调用过的方法
        List<PsiMethodCallExpression> methodCallListFiltered = methodCallExpressionList.stream().filter(methodCall -> {
            PsiMethod resolvedMethod = methodCall.resolveMethod();
            if (Objects.isNull(resolvedMethod)) {
                return false;
            }
            PsiClass methodContainingClass = resolvedMethod.getContainingClass();
            if (Objects.isNull(methodContainingClass)) {
                return false;
            }
            String methodContainingClassQualifiedName = methodContainingClass.getQualifiedName();
            if (StrUtil.isBlank(methodContainingClassQualifiedName)) {
                return false;
            }
            return methodContainingClassQualifiedName.equals(proxyEntityClassName);
        }).collect(Collectors.toList());


        // 看看调用了哪些方法
        List<String> proxyFieldSettled = methodCallListFiltered.stream().map(callInfo -> Optional.ofNullable(callInfo.resolveMethod()).map(PsiMethod::getName).orElse(null))
            .filter(StrUtil::isNotBlank)
            .collect(Collectors.toList());

        // 从 proxyEntityPsiClass 中找到 所有 返回值类型是 com.easy.query.core.proxy.columns.types.SQLLongTypeColumn 的方法
        List<PsiMethod> proxyFieldCanSet = Arrays.stream(proxyEntityPsiClass.getMethods())
            .filter(method -> !method.isConstructor() && method.getReturnType() != null)
            .filter(method -> {
                return method.getReturnType().getCanonicalText().startsWith("com.easy.query.core.proxy.columns.types.");
            }).collect(Collectors.toList());
        // 再去 看看 proxy 类中还有哪些字段没有设置

        List<PsiMethod> methodToCallList = proxyFieldCanSet.stream().filter(method -> {
                return !proxyFieldSettled.contains(method.getName());
            })
            .collect(Collectors.toList());


        // 现在是找到了 需要提示的方法了， 现在开始创建提示

        List<LookupElement> tips = Lists.newArrayList();
        for (PsiMethod methodToCall : methodToCallList) {
            PsiDocComment docComment = methodToCall.getDocComment();
            String commentStr = PsiTreeUtil.findChildrenOfType(docComment, PsiDocToken.class).stream().filter(ele -> ele.getTokenType() == JavaDocTokenType.DOC_COMMENT_DATA)
                .map(PsiElement::getText)
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trimToEmpty)
                .collect(Collectors.joining("; "));
            LookupElement lookupElementWithEq = PrioritizedLookupElement.withPriority(
                LookupElementBuilder.create("eq: set" + methodToCall.getName() + "() // " + StrUtil.subPre(commentStr, 10))
                    .withTypeText("尚未设置")
                    .withInsertHandler((context, item) -> {
                        String target = methodToCall.getName() + "().set()" ;
                        String comment = StrUtil.isBlank(commentStr) ? "" : " // " + commentStr;
                        context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), target + comment);
                        context.getEditor().getCaretModel().getCurrentCaret().moveToOffset(context.getStartOffset() + target.length() - 1);
                    })
                    .withIcon(Icons.EQ),
                400d);

            tips.add(lookupElementWithEq);
        }

        // 再来一个 setAll
        LookupElement lookupElementWithSetAll = PrioritizedLookupElement.withPriority(
            LookupElementBuilder.create("eq: set all")
                .withTypeText("设置所有字段")
                .withInsertHandler((context, item) -> {

                    StringBuilder target = new StringBuilder();
                    for (PsiMethod method : methodToCallList) {
                        PsiDocComment docComment = method.getDocComment();
                        String commentStr = PsiTreeUtil.findChildrenOfType(docComment, PsiDocToken.class).stream().filter(ele -> ele.getTokenType() == JavaDocTokenType.DOC_COMMENT_DATA)
                            .map(PsiElement::getText)
                            .filter(StrUtil::isNotBlank)
                            .map(StrUtil::trimToEmpty)
                            .collect(Collectors.joining("; "));
                        String comment = StrUtil.isBlank(commentStr) ? "" : " // " + commentStr;
                        target.append(".").append(method.getName()).append("().set()").append(comment).append("\n");
                    }

                    context.getDocument().replaceString(context.getStartOffset() - 1, context.getTailOffset(), target.toString());
                })
                .withIcon(Icons.EQ),
            400d);

        tips.add(lookupElementWithSetAll);

        result.addAllElements(tips);
    }
}
