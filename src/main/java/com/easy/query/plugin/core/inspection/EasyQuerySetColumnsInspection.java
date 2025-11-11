package com.easy.query.plugin.core.inspection;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.Objects;

/**
 * EasyQuery SetColumns 表达式检查
 * 确保 setColumns 中使用正确的更新方法
 */
public class EasyQuerySetColumnsInspection extends AbstractBaseJavaLocalInspectionTool {

    private static final String INSPECTION_PREFIX = "[EQ插件检查-setColumns表达式] ";
    private static final String INSPECTION_SHORT_NAME = "EasyQuerySetColumns";
    private static final String SQL_COLUMN_SELECTOR_CLASS_NAME = "com.easy.query.api4j.sql.SQLColumnOnlySelector";

    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getDisplayName() {
        return "EasyQuery SetColumns 表达式检查";
    }

    @Override
    public boolean runForWholeFile() {
        return true;
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
        // 在构建 Visitor 时查找一次 SQLColumnOnlySelector 类
        Project project = holder.getProject();
        PsiClass sqlColumnSelectorClass = JavaPsiFacade.getInstance(project)
                .findClass(SQL_COLUMN_SELECTOR_CLASS_NAME, GlobalSearchScope.allScope(project));
        // 返回新的 Visitor 实例，传入 holder 和缓存的 PsiClass
        return new SetColumnsVisitor(holder, sqlColumnSelectorClass);
    }

    /**
     * 用于访问和检查 setColumns 方法调用的 Visitor
     */
    private static class SetColumnsVisitor extends JavaElementVisitor {
        private final ProblemsHolder holder;
        private final PsiClass sqlColumnSelectorClass; // 缓存的 PsiClass

        public SetColumnsVisitor(ProblemsHolder holder, PsiClass sqlColumnSelectorClass) {
            this.holder = holder;
            this.sqlColumnSelectorClass = sqlColumnSelectorClass; // 存储缓存的类
        }

        @Override
        public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
            super.visitMethodCallExpression(expression);

            String methodName = expression.getMethodExpression().getReferenceName();
            if ("setColumns".equals(methodName)) {
                setColumnsCheck(expression);
            }
        }

        /**
         * 检查 setColumns(...) 内部表达式的有效性
         */
        private void setColumnsCheck(PsiMethodCallExpression setColumnsExpression) {
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
                lambdaBody.accept(new SetColumnsLambdaVisitor(rowParamName));
            } else if (lambdaBody instanceof PsiExpression) {
                // 如果是单个表达式，直接分析该表达式
                analyzeSetColumnsExpression((PsiExpression) lambdaBody, rowParamName);
            }
            // 其他类型的 Lambda 体(理论上不常见)将被忽略
        }

        /**
         * 用于分析 setColumns lambda 体内语句的访问者。
         */
        private class SetColumnsLambdaVisitor extends JavaRecursiveElementWalkingVisitor {
            private final String rowParamName;

            public SetColumnsLambdaVisitor(String rowParamName) {
                this.rowParamName = rowParamName;
            }

            @Override
            public void visitExpressionStatement(@NotNull PsiExpressionStatement statement) {
                // 分析顶层表达式语句，不需要再向下遍历此语句的子元素进行相同检查。
                analyzeSetColumnsExpression(statement.getExpression(), rowParamName);
            }

            // 只关心 ExpressionStatements, 其他访问方法（visitIfStatement 等）由递归特性隐式处理。
        }

        /**
         * 分析 setColumns lambda 主体内的单个顶级表达式语句。
         * 检查以下不允许的模式：
         * 1. 语句仅为 `row.property()`
         * 2. 语句为 `row.property().disallowedMethod(...)` (方法不是 set*, increment, decrement)
         */
        private void analyzeSetColumnsExpression(PsiExpression expression, String rowParamName) {
            if (!(expression instanceof PsiMethodCallExpression)) {
                // 顶级表达式不是方法调用，当前检查不适用
                return;
            }
            PsiMethodCallExpression topCall = (PsiMethodCallExpression) expression;
            PsiReferenceExpression methodExpression = topCall.getMethodExpression();
            PsiExpression qualifier = methodExpression.getQualifierExpression();
            String methodName = methodExpression.getReferenceName();

            // 新增检查：是否为 lambdaParam.column(...) 模式 (来自 SQLColumnOnlySelector)
            if (qualifier instanceof PsiReferenceExpression && Objects.equals(qualifier.getText(), rowParamName)) {
                PsiMethod resolvedMethod = topCall.resolveMethod();
                if (resolvedMethod != null) {
                    PsiClass containingClass = resolvedMethod.getContainingClass();
                    // 检查方法的所属类是否是 SQLColumnOnlySelector 或其子类/实现类
                    // 使用缓存的 sqlColumnSelectorClass
                    if (sqlColumnSelectorClass != null && containingClass != null &&
                        InheritanceUtil.isInheritorOrSelf(containingClass, sqlColumnSelectorClass, true)) {
                        // 这是有效的选择器调用，例如 o.column(Entity::getProperty)，跳过后续检查
                        return;
                    }
                }
                // 如果不是 SQLColumnOnlySelector 的方法，则可能是无效的独立调用，继续进行后续检查 (情况 1)
                // 结构是 row.method()，并且该方法不是上面允许的 SQLColumnOnlySelector 方法
                // 这通常是错误的，比如仅调用 getter
                LocalQuickFix suppressFix = new SuppressWarningQuickFix();
                holder.registerProblem(topCall, // 在方法调用上报告问题
                        INSPECTION_PREFIX + "SetColumns 中不允许仅调用属性访问器或选择器方法，请使用 'set', 'increment', 'decrement' 等更新方法或使用FETCHER属性选择器", // 调整了警告信息
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
                        registerDisallowedMethodProblem(topCall, qualifier, methodName);
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
        private void registerDisallowedMethodProblem(PsiMethodCallExpression invalidCall, PsiExpression propertyChainExpression, String disallowedMethodName) {
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
                    INSPECTION_PREFIX + "SetColumns 中应使用以 'set' 开头、'increment' 或 'decrement' 的方法更新字段或使用FETCHER属性选择器，而不是 '" + disallowedMethodName + "'",
                    ProblemHighlightType.WARNING,
                    changeToSetFix, suppressFix);
        }
    }
}
