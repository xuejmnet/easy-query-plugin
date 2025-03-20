package com.easy.query.plugin.core.inspection;

import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * EasyQuery Where 表达式检查
 * 确保 where 条件中使用正确的表达式格式
 */
public class EasyQueryWhereExpressionInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final String ABSTRACT_PROXY_ENTITY = "com.easy.query.core.proxy.AbstractProxyEntity";

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getDisplayName() {
        return "EasyQuery Where 表达式检查";
    }

    @Override
    public boolean runForWholeFile() {
        return true;
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                // 检查是否是 where 方法调用
                if (!"where".equals(expression.getMethodExpression().getReferenceName())) {
                    return;
                }

                expressionExistsCheck(expression);
            }

            /**
             * expression().exists() 检查<br/>
             * 1. 内部的调用 左侧实体必须是内部查询对象<br/>
             * @param expression  表达式
             */
            private void expressionExistsCheck(PsiMethodCallExpression expression) {
                // 检查是否在 expression().exists() 调用中
                if (!isInsideExpressionExists(expression)) {
                    return;
                }

                // 获取 where 方法的参数（Lambda 表达式）
                PsiExpression[] arguments = expression.getArgumentList().getExpressions();
                if (arguments.length != 1 || !(arguments[0] instanceof PsiLambdaExpression)) {
                    return;
                }

                PsiLambdaExpression lambda = (PsiLambdaExpression) arguments[0];
                PsiParameter[] parameters = lambda.getParameterList().getParameters();
                if (parameters.length == 0) {
                    return;
                }

                // 获取所有 Lambda 参数名（查询对象名）
                Set<String> queryObjectNames = new HashSet<>();
                for (PsiParameter parameter : parameters) {
                    String paramName = parameter.getName();
                    queryObjectNames.add(paramName);
                }

                // 检查 Lambda 内的方法调用
                PsiElement lambdaBody = lambda.getBody();
                if (lambdaBody == null) {
                    return;
                }

                // 查找所有的方法调用表达式
                Collection<PsiMethodCallExpression> methodCalls = PsiTreeUtil.findChildrenOfType(lambdaBody, PsiMethodCallExpression.class);
                for (PsiMethodCallExpression methodCall : methodCalls) {
                    // 获取 eq 方法的调用链
                    PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
                    if (qualifier instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression leftSide = (PsiMethodCallExpression) qualifier;

                        // 检查方法调用者的类型是否继承自 AbstractProxyEntity
                        PsiType qualifierType = leftSide.getMethodExpression().getQualifierExpression().getType();
                        if (qualifierType == null) {
                            continue;
                        }

                        if (!(qualifierType instanceof PsiClassType)) {
                            continue;
                        }

                        PsiClass psiClass = ((PsiClassType) qualifierType).resolve();
                        if (psiClass == null) {
                            continue;
                        }

                        // 如果不是继承自 AbstractProxyEntity，跳过检查
                        if (!InheritanceUtil.isInheritor(psiClass, ABSTRACT_PROXY_ENTITY)) {
                            continue;
                        }

                        String leftSideText = leftSide.getMethodExpression().getQualifierExpression().getText();

                        // 检查左侧是否以任一查询对象开头
                        if (!queryObjectNames.contains(leftSideText)) {
                            holder.registerProblem(methodCall,
                                    "EQ插件检测：Where 条件表达式可能不正确，左侧应该使用查询对象(" + String.join(", ", queryObjectNames) + ")的字段",
                                    ProblemHighlightType.WARNING);
                        }
                    }

                }
            }

            /**
             * 检查方法调用是否在 expression().exists() 中
             */
            private boolean isInsideExpressionExists(PsiMethodCallExpression whereExpression) {
                PsiElement parent = whereExpression.getParent();
                while (parent != null) {
                    if (parent instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression methodCall = (PsiMethodCallExpression) parent;
                        if (StrUtil.equalsAny(methodCall.getMethodExpression().getReferenceName(),"exists","notExists")) {
                            PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
                            if (qualifier instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression expressionCall = (PsiMethodCallExpression) qualifier;
                                if ("expression".equals(expressionCall.getMethodExpression().getReferenceName())) {
                                    return true;
                                }
                            }
                        }
                    }
                    parent = parent.getParent();
                }
                return false;
            }
        };
    }
}