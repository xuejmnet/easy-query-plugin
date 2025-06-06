package com.easy.query.plugin.core.inspection;

import com.easy.query.plugin.core.util.EasyQueryElementUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * EasyQuery OrderBy 调用检测
 * <p>
 * 该代码检查工具用于检测 {@code com.easy.query.api.proxy.client.EasyEntityQuery} 相关查询中
 * {@code orderBy} 方法的调用是否正确地以 {@code .asc()} 或 {@code .desc()} 结尾。
 * <p>
 * 它会检查以下情况：
 * <ul>
 *     <li>直接在 {@code orderBy} 参数中调用方法，例如 {@code orderBy(e.field.asc())}</li>
 *     <li>在 Lambda 表达式参数中调用方法，例如 {@code orderBy(e -> e.field.asc())}</li>
 * </ul>
 * 对于不符合规范的调用，会提示警告并提供快速修复选项（添加 {@code .asc()} 或 {@code .desc()}）。
 *
 * @author link2fun
 */
public class EasyQueryOrderByIncorrectInspection extends AbstractBaseJavaLocalInspectionTool {

    private final AddAscQuickFix addAscQuickFix = new AddAscQuickFix();
    private final AddDescQuickFix addDescQuickFix = new AddDescQuickFix();

    /**
     * 检查消息的通用前缀
     */
    private static final String PROBLEM_PREFIX = "[EQ插件检查-ORDER BY] ";

    /**
     * 返回此检查在设置中显示的名称。
     *
     * @return 显示名称
     */
    @Override
    public @Nls(capitalization = Nls.Capitalization.Sentence) @NotNull String getDisplayName() {
        return "EasyQuery OrderBy 检测";
    }

    /**
     * 指示此检查是否需要对整个文件进行分析。
     *
     * @return true 表示需要分析整个文件
     */
    @Override
    public boolean runForWholeFile() {
        return true;
    }


    /**
     * 构建用于访问和检查 Java 文件中元素的访问者。
     *
     * @param holder     用于注册问题的持有者
     * @param isOnTheFly 指示检查是否在编辑器中动态运行
     * @return Java 元素访问者
     */
    @Override
    public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
        return new JavaElementVisitor() {
            @Override
            public void visitClass(@NotNull PsiClass currentClass) {
                // 检查当前类是否注入了 EasyEntityQuery，如果没有则无需检查
                if (Arrays.stream(currentClass.getAllFields()).noneMatch(field -> field.getType().getCanonicalText().equals("com.easy.query.api.proxy.client.EasyEntityQuery"))) {
                    return;
                }

                // 遍历类中的所有标识符，查找 "orderBy"
                Collection<PsiIdentifier> identifierList = PsiTreeUtil.findChildrenOfType(currentClass, PsiIdentifier.class);
                for (PsiIdentifier identifier : identifierList) {
                    if (!identifier.getText().equals("orderBy")) {
                        continue; // 不是 orderBy 标识符，跳过
                    }

                    // 获取标识符的引用并解析
                    PsiReference identifierRef = identifier.getParent().getReference();
                    if (identifierRef == null || !(identifierRef.resolve() instanceof PsiMethod)) {
                        continue; // 无法解析或解析的不是方法，跳过
                    }

                    // 确保解析的是方法
                    PsiMethod resolvedMethod = (PsiMethod) identifierRef.resolve();
                    if (resolvedMethod == null || resolvedMethod.getContainingClass() == null || resolvedMethod.getContainingClass().getQualifiedName() == null) {
                        continue; // 方法或其所属类信息不完整，跳过
                    }

                    // 检查方法是否属于 EasyQuery 的 Queryable 扩展
                    if (!resolvedMethod.getContainingClass().getQualifiedName().startsWith("com.easy.query.api.proxy.entity.select.extension.queryable")) {
                        continue; // 不是 EasyQuery 的 orderBy 方法，跳过
                    }

                    // 获取 orderBy 方法调用的参数列表
                    PsiElement argumentsListElement = identifier.getParent().getNextSibling();
                    if (argumentsListElement instanceof PsiExpressionList) {
                        // 如果是表达式列表，则检查参数
                        checkOrderByArguments((PsiExpressionList) argumentsListElement, holder);
                    } else {
                        // 如果 orderBy 后面没有参数列表（可能是不完整的代码），直接报告问题
                        holder.registerProblem(identifier, PROBLEM_PREFIX + "OrderBy语句需要以 .asc() / .desc() 方法调用作为结尾", ProblemHighlightType.WARNING, addAscQuickFix, addDescQuickFix);
                    }
                }
            }
        };
    }

    /**
     * 检查 orderBy 方法的参数列表，确定调用是否规范。
     * <p>
     * 会分别处理普通表达式参数和 Lambda 表达式参数。
     * 如果参数列表为空或包含无法识别的结构（通常是直接传递字段引用或不完整的 Lambda），也会报告问题。
     *
     * @param argumentsList orderBy 方法的参数列表
     * @param holder        用于注册问题的持有者
     */
    private void checkOrderByArguments(PsiExpressionList argumentsList, ProblemsHolder holder) {
        // 查找参数列表中的普通表达式语句
        List<PsiMethodCallExpression> methodCallExpressionList = EasyQueryElementUtil.getDirectChildOfType(argumentsList, PsiMethodCallExpression.class);

        for (PsiMethodCallExpression methodCallExpression : methodCallExpressionList) {
            checkMethodCall(methodCallExpression, holder);
        }

    }

    private void checkMethodCall(PsiMethodCallExpression expression, ProblemsHolder holder) {
        // 检查表达式是否已经是 .asc() 或 .desc() 调用
        // PsiTreeUtil.findChildrenOfType(expression,PsiMethodCallExpression.class).stream().collect(Collectors.toList()).get(1).resolveMethod()
        // com.easy.query.core.proxy.AbstractProxyEntity#expression
        // 看看 表达式里面是否有 expression ，如果有的话，则不进行校验， 因为表达式能实现的太多了
        if (EasyQueryElementUtil.hasAbstractExpressionMethodCall(expression)) {
            return;
        }

        PsiMethod resolvedMethod = expression.resolveMethod();
        String resolvedMethodName = Optional.ofNullable(resolvedMethod).map(PsiMethod::getName).orElse(StrUtil.EMPTY);
        String methodClassQualifiedName = Optional.ofNullable(resolvedMethod).map(PsiMember::getContainingClass).map(PsiClass::getQualifiedName).orElse(StrUtil.EMPTY);

        // 如果已经是来自 SQLSelectExpression 的 asc 或 desc 调用，则认为是合法的
        if (methodClassQualifiedName.startsWith("com.easy.query.core.proxy.SQLSelectExpression")) {
            return; // 合法调用，无需报告
        }
        if (resolvedMethod == null) {
            return;
        }
        if (resolvedMethod.getReturnType() == null) {
            // 没有返回类型， 可能是正常的？先不处理
            return;
        }
        // 如果不是以 .asc() 或 .desc() 结尾的方法调用，报告问题
        if (resolvedMethod.getReturnType().getCanonicalText().startsWith("com.easy.query.core.proxy.columns.types")) {
            // 目前针对性的检查 实体字段调用
            holder.registerProblem(expression, PROBLEM_PREFIX + "OrderBy语句需要以 .asc() / .desc() / .orderBy() / .expression() 方法调用作为结尾", ProblemHighlightType.WARNING, addAscQuickFix, addDescQuickFix);
        }

    }


    /**
     * 提供快速修复，在检测到的元素后添加 {@code .asc()} 或 {@code .desc()} 后缀的抽象基类。
     */
    private static abstract class BaseAddOrderFix implements LocalQuickFix {
        private final String suffix; // 要添加的后缀, e.g., ".asc()"
        private final String suffixName; // 后缀的名称, e.g., "asc"

        /**
         * 构造函数。
         *
         * @param suffixName 后缀的名称 ("asc" 或 "desc")
         */
        protected BaseAddOrderFix(String suffixName) {
            this.suffixName = suffixName;
            this.suffix = "." + suffixName + "()";
        }

        @Nls(capitalization = Nls.Capitalization.Sentence)
        @NotNull
        @Override
        public String getName() {
            // 根据后缀名动态生成 QuickFix 在菜单中显示的名称
            return "添加 ." + suffixName + "()";
        }

        @Nls(capitalization = Nls.Capitalization.Sentence)
        @NotNull
        @Override
        public String getFamilyName() {
            // 通常 familyName 和 name 相同即可
            return getName();
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            PsiElementFactory factory = JavaPsiFacade.getElementFactory(project);

            // 根据检测到的元素类型应用修复
            if (element instanceof PsiExpressionStatement) {
                // 处理普通表达式语句: e.g., statement containing `x.field`
                PsiExpression expression = ((PsiExpressionStatement) element).getExpression();
                PsiExpression newExpression = factory.createExpressionFromText(expression.getText() + this.suffix, element);
                expression.replace(newExpression);
            } else if (element instanceof PsiLambdaExpression) {
                // 处理 Lambda 表达式: e.g., `e -> e.field`
                PsiLambdaExpression lambdaExpression = (PsiLambdaExpression) element;
                PsiElement lambdaBody = lambdaExpression.getBody();
                if (lambdaBody instanceof PsiExpression) { // 确保 Lambda 体是表达式
                    String text = lambdaBody.getText();
                    PsiExpression newBody = factory.createExpressionFromText(text + this.suffix, lambdaBody);
                    lambdaBody.replace(newBody);
                } else if (lambdaBody instanceof PsiCodeBlock) {
                    // 如果是代码块，尝试找到最后的 return 语句或表达式语句
                    // 这里简化处理，可能需要更复杂的逻辑来处理所有代码块情况
                    // 暂时只对简单返回表达式的Lambda有效，与原逻辑保持一致
                }
            } else if (element instanceof PsiExpressionList) {
                // 处理空的或无效的参数列表: e.g., `orderBy()`
                PsiElement parent = element.getParent(); // parent is the PsiMethodCallExpression `orderBy(...)`
                if (parent instanceof PsiMethodCallExpression) {
                    // 这种情况修复可能不理想，因为不知道应该对什么调用 .asc() / .desc()
                    // String text = parent.getText();
                    // PsiExpression newExpression = factory.createExpressionFromText(text + this.suffix, parent);
                    // parent.replace(newExpression);
                    // 注释掉此修复: 对空参数列表或无效参数应用后缀通常没有意义，修复目标不明确，可能产生错误代码，建议用户手动修改。
                }
            } else if (element instanceof PsiIdentifier && element.getText().equals("orderBy")) {
                // 处理直接在 orderBy 标识符上报告的问题 (通常是缺少参数列表)
                PsiElement parent = element.getParent(); // PsiReferenceExpression `orderBy`
                if (parent instanceof PsiReferenceExpression) {
                    PsiElement grandParent = parent.getParent(); // PsiMethodCallExpression `orderBy`
                    if (grandParent instanceof PsiMethodCallExpression && ((PsiMethodCallExpression) grandParent).getArgumentList().getExpressionCount() == 0) {
                        // 同样，对此情况应用修复可能不理想
                        // String text = grandParent.getText();
                        // PsiExpression newExpression = factory.createExpressionFromText(text + this.suffix, grandParent);
                        // grandParent.replace(newExpression);
                        // 注释掉此修复: 对缺少参数的 orderBy(...) 调用应用后缀没有意义，修复目标不明确，建议用户手动修改。
                    }
                }
            } else if (element instanceof PsiMethodCallExpression) {
                // 处理直接在方法调用上报告的问题 (通常是 Lambda 体内或普通表达式参数)
                // e.g., `x.field` within `orderBy(e -> x.field)` or `orderBy(x.field)`
                String text = element.getText();
                PsiExpression newExpression = factory.createExpressionFromText(text + this.suffix, element);
                element.replace(newExpression);
            } else if (element instanceof PsiReferenceExpression || element instanceof PsiIdentifier) {
                // 处理 Lambda 体内直接返回字段的情况 (e.g., e -> e.id)
                String text = element.getText();
                PsiExpression newExpression = factory.createExpressionFromText(text + this.suffix, element);
                element.replace(newExpression);
            }
        }
    }

    /**
     * 提供快速修复，在检测到的元素后添加 ".asc()"。
     */
    private static class AddAscQuickFix extends BaseAddOrderFix {
        protected AddAscQuickFix() {
            super("asc");
        }
    }

    /**
     * 提供快速修复，在检测到的元素后添加 ".desc()"。
     */
    private static class AddDescQuickFix extends BaseAddOrderFix {
        protected AddDescQuickFix() {
            super("desc");
        }
    }
}
