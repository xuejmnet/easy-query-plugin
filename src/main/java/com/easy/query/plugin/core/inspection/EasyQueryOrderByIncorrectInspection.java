package com.easy.query.plugin.core.inspection;

import cn.hutool.core.collection.CollectionUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsMethodImpl;
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

/**
 * EasyQuery OrderBy 调用检测
 * @author link2fun
 */
public class EasyQueryOrderByIncorrectInspection extends AbstractBaseJavaLocalInspectionTool {

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getDisplayName() {
        return "EasyQuery OrderBy 检测";
    }

    @Override
    public boolean runForWholeFile() {
        return true;
    }


    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitClass(@NotNull PsiClass currentClass) {


                // 看看当前类中是否注入了 com.easy.query.api.proxy.client.EasyEntityQuery
                PsiField[] fields = currentClass.getFields();
                if (Arrays.stream(fields).noneMatch(field -> field.getType().getCanonicalText().equals("com.easy.query.api.proxy.client.EasyEntityQuery"))) {
                    // 没有这个字段, 那么不需要检查
                    return;
                }

                // 如果有，那么就检测当前类中的方法是否使用了 orderBy 方法

                Collection<PsiIdentifier> identifierList = PsiTreeUtil.findChildrenOfType(currentClass, PsiIdentifier.class);

                for (PsiIdentifier identifier : identifierList) {
                    if (!identifier.getText().equals("orderBy")) {
                        continue;
                    }
                    PsiReference identifierRef = identifier.getParent().getReference();
                    if (identifierRef == null) {
                        continue;
                    }
                    if(!(identifierRef.resolve() instanceof ClsMethodImpl)){
                        continue;
                    }
                    ClsMethodImpl resolved = (ClsMethodImpl) identifierRef.resolve();
                    if (resolved == null || resolved.getContainingClass() == null || resolved.getContainingClass().getQualifiedName() == null) {
                        continue;
                    }
                    if (!resolved.getContainingClass().getQualifiedName().startsWith("com.easy.query.api.proxy.entity.select.extension.queryable")) {
                        // 不是 com.easy.query.api.proxy.entity.select.extension.queryable.Queryable 的方法
                        continue;
                    }

                    PsiElement parentNextSibling = identifier.getParent().getNextSibling();
                    if (parentNextSibling instanceof PsiExpressionList) {
                        Collection<PsiExpressionStatement> normalExpressions = PsiTreeUtil.findChildrenOfType(parentNextSibling, PsiExpressionStatement.class);
                        for (PsiExpressionStatement normalExpression : normalExpressions) {
                            PsiExpression expression = normalExpression.getExpression();
                            if (expression instanceof PsiMethodCallExpression) {
                                PsiMethod resolvedMethod = ((PsiMethodCallExpression) expression).resolveMethod();

                                String resolvedMethodName = Optional.ofNullable(resolvedMethod).map(PsiMethod::getName).orElse(StrUtil.EMPTY);

                                String methodClassQualifiedName = Optional.ofNullable(resolvedMethod).map(PsiMember::getContainingClass).map(PsiClass::getQualifiedName).orElse(StrUtil.EMPTY);

                                if (StrUtil.equalsAny(resolvedMethodName, "asc", "desc") && methodClassQualifiedName.startsWith("com.easy.query.core.proxy.SQLSelectExpression")) {
                                    continue;
                                }
                            }

                            holder.registerProblem(expression, "OrderBy语句需要以 .asc() / .desc() 方法调用作为结尾");
                        }
                        Collection<PsiLambdaExpression> lambdaExpressions = PsiTreeUtil.findChildrenOfType(parentNextSibling, PsiLambdaExpression.class);
                        for (PsiLambdaExpression expression : lambdaExpressions) {
                            boolean correct = PsiTreeUtil.findChildrenOfType(expression, PsiMethodCallExpression.class).stream()
                                    .anyMatch(ele -> {
                                        PsiMethod resolvedMethod = ele.resolveMethod();
                                        String resolvedMethodName = Optional.ofNullable(resolvedMethod).map(PsiMethod::getName).orElse(StrUtil.EMPTY);

                                        if (!StrUtil.equalsAny(resolvedMethodName, "asc", "desc")) {
                                            return false;
                                        }
                                        String methodClassQualifiedName = Optional.ofNullable(resolvedMethod).map(PsiMember::getContainingClass).map(PsiClass::getQualifiedName).orElse(StrUtil.EMPTY);

                                        return methodClassQualifiedName.startsWith("com.easy.query.core.proxy.SQLSelectExpression");
                                    });
                            if (!correct) {
                                holder.registerProblem(expression, "OrderBy语句需要以 .asc() / .desc() 方法调用作为结尾");
                            }

                        }

                        if (CollectionUtil.isEmpty(normalExpressions) && CollectionUtil.isEmpty(lambdaExpressions)) {
                            holder.registerProblem(parentNextSibling, "OrderBy语句需要以 .asc() / .desc() 方法调用作为结尾, 请不要直接使用 lambda 字段");
                        }

                    } else {
                        holder.registerProblem(identifier, "OrderBy语句需要以 .asc() / .desc() 方法调用作为结尾");
                    }
                }


            }
        };
    }
}
