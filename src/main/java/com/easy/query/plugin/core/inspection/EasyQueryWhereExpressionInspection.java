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
 * EasyQuery Where 和 SetColumns 表达式检查
 * 确保 where 条件中使用正确的表达式格式
 * 确保 setColumns 中使用正确的更新方法
 */
public class EasyQueryWhereExpressionInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final String ABSTRACT_PROXY_ENTITY = "com.easy.query.core.proxy.AbstractProxyEntity";
    private static final String INSPECTION_SHORT_NAME = "EasyQueryExpressionInspection"; // 重命名以适应更广的检查范围

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getDisplayName() {
        return "EasyQuery Where/SetColumns 表达式检查";
    }

    @Override
    public boolean runForWholeFile() {
        return true;
    }

    // --- QuickFixes ---

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
     * 修复 SetColumns 中错误使用方法的 QuickFix
     */
    private static class ChangeToSetQuickFix implements LocalQuickFix {
        private final String propertyChainText; // 例如 "row.transactionStatus()"
        private final String argumentsText;     // 例如 "QueueTransactionStatus.FAIL.getValue()"

        public ChangeToSetQuickFix(String propertyChainText, String argumentsText) {
            this.propertyChainText = propertyChainText;
            this.argumentsText = argumentsText;
        }

        @Override
        public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getName() {
            return "将方法更改为 'set'";
        }

        @Override
        public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getFamilyName() {
            return getName();
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement(); // 有问题的方法调用，例如 eq(...)
            if (!(element instanceof PsiMethodCallExpression)) {
                return;
            }

            PsiElementFactory factory = JavaPsiFacade.getInstance(project).getElementFactory();
            String newExpressionText = propertyChainText + ".set(" + argumentsText + ")";
            PsiExpression newExpression = factory.createExpressionFromText(newExpressionText, element.getContext());
            element.replace(newExpression);
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
                    // 如果需要，以后可以添加对常规 where 子句的检查
                } else if ("setColumns".equals(methodName)) {
                    setColumnsCheck(expression, holder);
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
                                "EQ插件检测：Where 条件表达式左侧 '" + baseIdentifier + "' 不是有效的查询对象 (" + String.join(", ", queryObjectNames) + ")，可能写反或范围错误",
                                ProblemHighlightType.WARNING,
                                swapFix, suppressFix); // 提供交换和抑制选项
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
             * 检查 setColumns(...) 内部表达式的有效性
             */
            private void setColumnsCheck(PsiMethodCallExpression setColumnsExpression, ProblemsHolder holder) {
                PsiExpression[] args = setColumnsExpression.getArgumentList().getExpressions();
                if (args.length != 1 || !(args[0] instanceof PsiLambdaExpression)) {
                    return;
                }

                PsiLambdaExpression lambda = (PsiLambdaExpression) args[0];
                PsiParameter[] parameters = lambda.getParameterList().getParameters();
                if (parameters.length != 1) { // 期望只有一个参数（行/更新器）
                    return;
                }
                String rowParamName = parameters[0].getName();

                PsiElement lambdaBody = lambda.getBody();
                if (lambdaBody == null) {
                    return;
                }

                // 使用访问者递归遍历 Lambda 体，查找需要检查的表达式语句
                if (lambdaBody instanceof PsiCodeBlock) {
                    // 如果是代码块，使用 Visitor 遍历内部语句
                    lambdaBody.accept(new SetColumnsLambdaVisitor(holder, rowParamName));
                } else if (lambdaBody instanceof PsiExpression) {
                    // 如果是单个表达式，直接分析该表达式
                    analyzeSetColumnsExpression((PsiExpression) lambdaBody, rowParamName, holder);
                }
                // 其他类型的 Lambda 体(理论上不常见)将被忽略
            }

            /**
             * 用于分析 setColumns lambda 体内语句的访问者。
             */
            class SetColumnsLambdaVisitor extends JavaRecursiveElementWalkingVisitor {
                private final ProblemsHolder holder;
                private final String rowParamName;

                public SetColumnsLambdaVisitor(ProblemsHolder holder, String rowParamName) {
                    this.holder = holder;
                    this.rowParamName = rowParamName;
                }

                @Override
                public void visitExpressionStatement(@NotNull PsiExpressionStatement statement) {
                    // 分析顶层表达式语句，不需要再向下遍历此语句的子元素进行相同检查。
                    analyzeSetColumnsExpression(statement.getExpression(), rowParamName, holder);
                }

                // 只关心 ExpressionStatements, 其他访问方法（visitIfStatement 等）由递归特性隐式处理。
            }

            /**
             * 分析 setColumns lambda 主体内的单个顶级表达式语句。
             * 检查以下不允许的模式：
             * 1. 语句仅为 `row.property()`
             * 2. 语句为 `row.property().disallowedMethod(...)` (方法不是 set*, increment, decrement)
             */
            private void analyzeSetColumnsExpression(PsiExpression expression, String rowParamName, ProblemsHolder holder) {
                if (!(expression instanceof PsiMethodCallExpression)) {
                    // 顶级表达式不是方法调用，当前检查不适用
                    return;
                }
                PsiMethodCallExpression topCall = (PsiMethodCallExpression) expression;

                PsiExpression qualifier = topCall.getMethodExpression().getQualifierExpression();
                String methodName = topCall.getMethodExpression().getReferenceName();

                // 情况 1: 检查是否为独立的 row.property()
                if (qualifier instanceof PsiReferenceExpression && Objects.equals(qualifier.getText(), rowParamName)) {
                    // 结构是 row.property()，这是不允许的独立调用
                    LocalQuickFix suppressFix = new SuppressWarningQuickFix();
                    holder.registerProblem(topCall, // 在 property() 调用上报告问题
                            "EQ插件检测：SetColumns 中不允许仅调用属性访问器，请使用 'set', 'increment', 'decrement' 等方法进行更新",
                            ProblemHighlightType.WARNING,
                            suppressFix);
                    return; // 模式匹配，处理完毕
                }

                // 情况 2: 检查是否为 row.property().method()
                if (qualifier instanceof PsiMethodCallExpression) {
                    PsiMethodCallExpression propertyCall = (PsiMethodCallExpression) qualifier;
                    PsiExpression baseQualifier = propertyCall.getMethodExpression().getQualifierExpression();

                    // 检查基限定符是否为 row 参数
                    if (baseQualifier instanceof PsiReferenceExpression && Objects.equals(baseQualifier.getText(), rowParamName)) {
                        // 结构是 row.property().method()

                        // 检查 methodName 是否 *不允许*
                        if (!(methodName != null && methodName.startsWith("set")) &&
                                !Objects.equals(methodName, "increment") &&
                                !Objects.equals(methodName, "decrement")) {

                            // --- 为不允许的方法注册问题
                            registerDisallowedMethodProblem(holder, topCall, qualifier, methodName);
                            // --- 问题注册结束
                        }
                        // 如果方法允许 (set*, increment, decrement)，则此结构有效，无需操作。
                        return; // 模式匹配 (无论有效/无效)，处理完毕
                    }
                }

                // 如果两种模式都不匹配，根据规则假定表达式有效。
            }

            /**
             * 辅助方法：注册不允许的方法调用问题
             */
            private void registerDisallowedMethodProblem(ProblemsHolder holder, PsiMethodCallExpression invalidCall, PsiExpression propertyChainExpression, String disallowedMethodName) {
                PsiExpression[] methodArgs = invalidCall.getArgumentList().getExpressions();
                String argsText = "";
                if (methodArgs.length > 0) {
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < methodArgs.length; i++) {
                        sb.append(methodArgs[i].getText());
                        if (i < methodArgs.length - 1) {
                            sb.append(", ");
                        }
                    }
                    argsText = sb.toString();
                }

                String propertyChainText = propertyChainExpression.getText(); // 例如 "row.transactionStatus()"
                LocalQuickFix changeToSetFix = new ChangeToSetQuickFix(propertyChainText, argsText);
                LocalQuickFix suppressFix = new SuppressWarningQuickFix();
                holder.registerProblem(invalidCall, // 在最终的无效方法调用上报告问题
                        "EQ插件检测：SetColumns 中应使用以 'set' 开头、'increment' 或 'decrement' 的方法更新字段，而不是 '" + disallowedMethodName + "'",
                        ProblemHighlightType.WARNING,
                        changeToSetFix, suppressFix);
            }

            /**
             * 检查给定的 PsiElement 是否嵌套在 expression().exists() 或 expression().notExists() 结构中。
             * @param element 要检查的元素，通常是内部的 where 方法调用。
             */
            private boolean isInsideExpressionExists(PsiElement element) {
                PsiElement current = element;
                while (current != null && !(current instanceof PsiMethod) && !(current instanceof PsiClass) && !(current instanceof PsiFile)) {
                    // 向上查找，直到遇到方法定义、类定义或文件结束

                    if (current instanceof PsiMethodCallExpression) {
                        PsiMethodCallExpression methodCall = (PsiMethodCallExpression) current;
                        if (StrUtil.equalsAny(methodCall.getMethodExpression().getReferenceName(), "exists", "notExists")) {
                            PsiExpression qualifier = methodCall.getMethodExpression().getQualifierExpression();
                            if (qualifier instanceof PsiMethodCallExpression) {
                                PsiMethodCallExpression expressionCall = (PsiMethodCallExpression) qualifier;
                                if ("expression".equals(expressionCall.getMethodExpression().getReferenceName())) {
                                    // 找到了 expression().exists() 或 expression().notExists() 结构，
                                    // 并且正在检查的原始 element (例如内部 where) 是它的子孙节点。
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