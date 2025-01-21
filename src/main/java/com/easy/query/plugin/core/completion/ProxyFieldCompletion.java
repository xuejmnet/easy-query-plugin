package com.easy.query.plugin.core.completion;

import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
        PsiJavaCodeReferenceElement proxyEntityClassReference = newProxyDefine.getClassReference();
        if (Objects.isNull(newProxyDefine) || proxyEntityClassReference == null) {
            return;
        }
        PsiFile proxyEntityContainingFile = proxyEntityClassReference.getContainingFile();
        Project project = position.getProject();
        String proxyEntityClassName = proxyEntityClassReference.getQualifiedName();
        PsiClass proxyEntityPsiClass = PsiJavaFileUtil.getPsiClass(project, proxyEntityClassName);

        List<PsiMethodCallExpression> methodCallExpressionList = PsiTreeUtil.findChildrenOfType(position.getParent(), PsiMethodCallExpression.class).stream().collect(Collectors.toList());

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
        List<String> proxyFieldSettled = methodCallListFiltered.stream().map(callInfo -> callInfo.resolveMethod().getName()).collect(Collectors.toList());

        // 从 proxyEntityPsiClass 中找到 所有 返回值类型是 com.easy.query.core.proxy.columns.types.SQLLongTypeColumn 的方法
        List<PsiMethod> proxyFieldCanSet = Arrays.stream(proxyEntityPsiClass.getMethods())
                .filter(method -> !method.isConstructor() && method.getReturnType() != null)
                .filter(method -> {
                    return method.getReturnType().getCanonicalText().startsWith("com.easy.query.core.proxy.columns.types.SQLLongTypeColumn");
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
            LookupElement lookupElementWithEq = PrioritizedLookupElement.withPriority(
                    LookupElementBuilder.create("eq: set" + methodToCall.getName())
                            .withTypeText("尚未设置")
                            .withInsertHandler((context, item) -> {
                                String target = methodToCall.getName() + "().set()";
                                context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), target);

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
                                target.append("." + method.getName()).append("().set()\n");
                            }

                            context.getDocument().replaceString(context.getStartOffset() - 1, context.getTailOffset(), target.toString());
                        })
                        .withIcon(Icons.EQ),
                400d);

        tips.add(lookupElementWithSetAll);

        result.addAllElements(tips);
    }
}
