package com.easy.query.plugin.core.inspection;

import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * EasyQuery Where 表达式检查
 * 确保 where 条件中使用正确的表达式格式
 */
public class EasyQueryWhereExpressionInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final String INSPECTION_PREFIX = "[EQ插件检查-WHERE表达式] ";
    private static final String INSPECTION_SHORT_NAME = "EasyQueryWhereExpression";

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getDisplayName() {
        return "EasyQuery Where 表达式检查";
    }

    @Override
    public boolean runForWholeFile() {
        return true;
    }

    /**
     * 修复 Where 中写反的表达式的 QuickFix
     */
    private static class SwapWhereExpressionQuickFix implements LocalQuickFix {
        private final String methodName;
        private final String leftText;
        private final String rightText;

        public SwapWhereExpressionQuickFix(String methodName, String leftText, String rightText) {
            this.methodName = methodName;
            this.leftText = leftText;
            this.rightText = rightText;
        }

        @Override
        public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getName() {
            return "交换 where 表达式左右两边";
        }

        @Override
        public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getFamilyName() {
            return getName();
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (!(element instanceof PsiMethodCallExpression)) {
                return;
            }

            PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
            String newExpression = rightText + "." + methodName + "(" + leftText + ")";
            PsiExpression expression = factory.createExpressionFromText(newExpression, element.getContext());
            element.replace(expression);
        }
    }

    /**
     * 抑制警告的 QuickFix
     */
    private static class SuppressWarningQuickFix implements LocalQuickFix {
        @Override
        public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getName() {
            return "抑制警告 (添加 //noinspection " + INSPECTION_SHORT_NAME + ")";
        }

        @Override
        public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getFamilyName() {
            return getName();
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiStatement statement = PsiTreeUtil.getParentOfType(element, PsiStatement.class, false);
            if (statement == null) {
                // 尝试查找表达式，如果它不直接在语句中（例如，在 return 内部）
                PsiExpression expression = PsiTreeUtil.getParentOfType(element, PsiExpression.class, false);
                statement = PsiTreeUtil.getParentOfType(expression, PsiStatement.class, false);
                if (statement == null) {
                    return; // 找不到
                }
            }

            PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
            // 创建 noinspection 注释
            PsiComment comment = factory.createCommentFromText("//noinspection " + INSPECTION_SHORT_NAME, null);
            PsiElement parent = statement.getParent();
            if (parent != null) {
                parent.addBefore(comment, statement);
            }
        }
    }

    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);

                String methodName = expression.getMethodExpression().getReferenceName();

                if ("where".equals(methodName)) {
                    // 检查 expression().exists() 模式
                    if (isInsideExpressionExists(expression)) {
                        expressionExistsWhereCheck(expression, holder);
                    }
                }
            }

            /**
             * 检查 expression().exists().where(...) 内部表达式的有效性
             */
            private void expressionExistsWhereCheck(PsiMethodCallExpression whereExpression, ProblemsHolder holder) {
                PsiExpression[] arguments = whereExpression.getArgumentList().getExpressions();
                if (arguments.length != 1 || !(arguments[0] instanceof PsiLambdaExpression)) {
                    return;
                }

                PsiLambdaExpression lambda = (PsiLambdaExpression) arguments[0];
                PsiParameter[] parameters = lambda.getParameterList().getParameters();
                if (parameters.length == 0) {
                    return;
                }

                Set<String> queryObjectNames = new HashSet<>();
                for (PsiParameter parameter : parameters) {
                    queryObjectNames.add(parameter.getName());
                }

                PsiElement lambdaBody = lambda.getBody();
                if (lambdaBody == null) {
                    return;
                }

                // 递归检查
                lambdaBody.accept(new JavaRecursiveElementWalkingVisitor() {
                    @Override
                    public void visitMethodCallExpression(PsiMethodCallExpression methodCall) {
                        super.visitMethodCallExpression(methodCall);
                        checkWhereLambdaMethodCall(methodCall, queryObjectNames, holder);
                    }
                });
            }

            /**
             * 检查 expression().exists().where() lambda 内部的特定方法调用。
             */
            private void checkWhereLambdaMethodCall(PsiMethodCallExpression methodCall, Set<String> queryObjectNames, ProblemsHolder holder) {
                String currentMethodName = methodCall.getMethodExpression().getReferenceName();
                if (!StrUtil.equalsAny(currentMethodName, "eq", "ne", "like", "notLike", "likeMatchLeft", "likeMatchRight", "notLikeMatchLeft", "notLikeMatchRight", "in", "notIn", "le", "lt", "ge", "gt")) {
                    return;
                }

                // 获取导致方法调用的完整限定符链，例如 "t1.name.getValue()"
                PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
                if (!(qualifier instanceof PsiMethodCallExpression)) { // 期望类似 x.property() 的形式
                    return;
                }
                PsiMethodCallExpression propertyCall = (PsiMethodCallExpression) qualifier;

                PsiExpression propertyQualifier = propertyCall.getMethodExpression().getQualifierExpression();
                if (propertyQualifier == null) {
                    return;
                }

                // 检查调用的基对象是否为 lambda 参数之一
                String baseIdentifier = getBaseIdentifierText(propertyQualifier);
                if (baseIdentifier != null && !queryObjectNames.contains(baseIdentifier)) {
                    // 问题：左侧基对象不是当前 Lambda 的参数，可能写反或使用了错误的查询对象。
                    PsiExpression[] methodArgs = methodCall.getArgumentList().getExpressions();
                    if (methodArgs.length > 0) {
                        String rightSideText = methodArgs[0].getText();
                        String leftSideText = qualifier.getText(); // 完整的左侧链

                        LocalQuickFix swapFix = new SwapWhereExpressionQuickFix(
                                currentMethodName,
                                leftSideText,
                                rightSideText
                        );
                        LocalQuickFix suppressFix = new SuppressWarningQuickFix();

                        holder.registerProblem(methodCall,
                                INSPECTION_PREFIX + "Where 条件表达式左侧 '" + baseIdentifier + "' 不是有效的查询对象 (" + String.join(", ", queryObjectNames) + ")，可能写反或范围错误",
                                ProblemHighlightType.WARNING,
                                swapFix, suppressFix);
                    }
                }
            }

            /**
             * 获取表达式链的基本标识符文本(例如，从 "t1.customer.address" 中获取 "t1")
             */
            private String getBaseIdentifierText(PsiExpression expression) {
                while (expression instanceof PsiReferenceExpression && ((PsiReferenceExpression) expression).getQualifierExpression() != null) {
                    expression = ((PsiReferenceExpression) expression).getQualifierExpression();
                }
                if (expression instanceof PsiReferenceExpression) {
                    return expression.getText();
                }
                return null; // 不是简单的标识符链
            }

            /**
             * 检查给定的 PsiElement 是否嵌套在 expression().exists() 或 expression().notExists() 结构中。
             */
            private boolean isInsideExpressionExists(PsiElement element) {
                PsiElement current = element;
                while (current != null && !(current instanceof PsiMethod) && !(current instanceof PsiClass) && !(current instanceof PsiFile)) {
                    if (current instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression methodCall = (PsiMethodCallExpression) current;
                        if (StrUtil.equalsAny(methodCall.getMethodExpression().getReferenceName(), "exists", "notExists")) {
                            PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
                            if (qualifier instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression expressionCall = (PsiMethodCallExpression) qualifier;
                                if ("expression".equals(expressionCall.getMethodExpression().getReferenceName())) {
                                    if (PsiTreeUtil.isAncestor(methodCall, element, false)) {
                                        return true;
                                    }
                                }
                            }
                        }
                    }
                    current = current.getParent();
                }
                return false;
            }
        };
    }
}