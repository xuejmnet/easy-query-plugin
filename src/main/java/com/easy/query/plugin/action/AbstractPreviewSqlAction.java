package com.easy.query.plugin.action;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.easy.query.plugin.core.util.MyModuleUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.easy.query.plugin.windows.SQLPreviewDialog;
import com.google.common.collect.Sets;
import com.intellij.execution.*;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.*;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.compiler.CompileContext;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.sql.dialects.mysql.MysqlDialect;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 代码预览SQL入口
 * @author link2fun
 */
@Slf4j
public abstract class AbstractPreviewSqlAction extends AnAction {


    /** 运行模式, auto or manual
     * 参数的设置模式, auto 下 进行弹窗展示, manual 下 进行代码展示
     * */
    private final String runMode;

    public AbstractPreviewSqlAction(String runMode) {
        this.runMode = runMode;
    }


    @Override
    public void update(@NotNull AnActionEvent e) {
        // 获取当前项目
        Project project = e.getProject();
        // 获取当前选中的文件
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        // 设置菜单项是否可见
        e.getPresentation().setEnabledAndVisible(
                project != null &&
                        psiFile != null &&
                        psiFile.getFileType().equals(JavaFileType.INSTANCE));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {

        Editor editor = event.getData(CommonDataKeys.EDITOR);
        if (editor == null) {
            return;
        }
        SelectionModel selectionModel = editor.getSelectionModel();
        String selectedText = selectionModel.getSelectedText();
        if (StrUtil.isBlank(selectedText)) {
            // 没有选择文字
            return;
        }

        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);

        if (!(psiFile instanceof PsiJavaFile)) {
            return;
        }

        PsiClass psiClassSource = ((PsiJavaFile) psiFile).getClasses()[0];

        PsiJavaFile psiJavaFileSource = (PsiJavaFile) psiFile;

        PsiElement selectedElementRaw = PsiTreeUtil.findElementOfClassAtRange(psiJavaFileSource,
                selectionModel.getSelectionStart(), selectionModel.getSelectionEnd(), PsiElement.class);

        // 很可能没选全, 这里获取最外围的方法调用
        // PsiElement selectedElementsWhole =
        // PsiTreeUtil.getTopmostParentOfType(selectedElementRaw,
        // PsiMethodCallExpression.class);
        PsiElement selectedElements = selectedElementRaw;

        // 移除最后一个方法调用, 最后一个一般是转换结果的

        if (selectedElements == null || selectedElements.getChildren().length == 0
                || selectedElements.getChildren()[0].getChildren().length == 0) {
            return;
        }

        selectedText = selectedElements.getChildren()[0].getChildren()[0].getText();

        List<PsiReferenceExpression> refOrVarList = PsiTreeUtil
                .findChildrenOfType(selectedElements, PsiReferenceExpression.class).stream()
                .filter(ref -> {
                    PsiElement resolved = ref.resolve();
                    if (resolved instanceof PsiLocalVariable || resolved instanceof PsiParameter) {
                        // 如果 resolved 的位置, 在选中的内部, 说明是内部定义的, 不是外部引用的
                        if (selectionModel.getSelectionStart() <= resolved.getTextOffset()
                                && resolved.getTextOffset() <= selectionModel.getSelectionEnd()) {
                            return false;
                        }

                        // 看看是否有父类, 如果父类是 easy-query 包下面的, 那么可能是自动生成的
                        if (resolved instanceof PsiParameter) {
                            String canonicalText = ((PsiParameter) resolved).getType().getCanonicalText();
                            if (canonicalText.endsWith("Proxy") && canonicalText.contains(".proxy.")) {
                                return false;
                            }
                        }

                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());

        updateGitignore(psiFile.getProject());

        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiFile.getProject());
        // 需要把这些外部变量给定义了
        List<String> varList = constructSearchReq(refOrVarList, elementFactory, psiClassSource);

        Module currentModule = MyModuleUtil.getModuleForFile(psiFile.getProject(), psiFile.getVirtualFile());

        // 拷贝当前文件
        PsiJavaFile psiJavaFileCopied = (PsiJavaFile) psiJavaFileSource.copy();
        // 拷贝类文件
        PsiClass psiClassCopied = (PsiClass) psiClassSource.copy();

        // 修改类文件, 移除接口
        PsiReferenceList implementsList = psiClassCopied.getImplementsList();
        for (PsiJavaCodeReferenceElement referenceElement : implementsList.getReferenceElements()) {
            referenceElement.delete();
        }

        // 添加上新的接口 javax.sql.DataSource
        PsiJavaCodeReferenceElement dataSourceInterface = elementFactory.createReferenceFromText("javax.sql.DataSource",
                psiClassCopied);
        implementsList.add(dataSourceInterface);

        // 修改类文件, 移除注解
        for (PsiMethod method : psiClassCopied.getMethods()) {
            method.delete();
        }

        // 移除里面的字段
        PsiField[] fields = psiClassCopied.getFields();
        for (PsiField field : fields) {
            field.delete();
        }

        // 移除类上面的注解
        PsiModifierList modifierList = psiClassCopied.getModifierList();
        if (modifierList != null) {
            PsiAnnotation[] annotations = modifierList.getAnnotations();
            for (PsiAnnotation annotation : annotations) {
                annotation.delete();
            }
        }

        // 构建一个 Main 方法
        Project project = psiClassCopied.getProject();
        PsiMethod mainMethod = JavaPsiFacade.getElementFactory(project)
                .createMethodFromText("public static void main(String[] args) {\n" +
                        "        System.out.println(\"EasyQuery Preview SQL\");\n" +
                        "\n" +
                        "    // 采用控制台输出\n" +
                        "    LogFactory.useStdOutLogging();\n" +
                        "\n" +
                        "    EasyQueryClient queryClient = EasyQueryBootstrapper.defaultBuilderConfiguration()\n" +
                        "      .setDefaultDataSource(new EasyQueryPreviewSqlAction())\n" +
                        "      .optionConfigure(option -> {\n" +
                        "        option.setPrintSql(true); // 输出SQL\n" +
                        "        option.setKeepNativeStyle(true);\n" +
                        "      })\n" +
                        // " .useDatabaseConfigure(new MsSQLDatabaseConfiguration())\n" +
                        "      .useDatabaseConfigure(new com.easy.query.mysql.config.MySQLDatabaseConfiguration())\n" +
                        "      .build();\n" +
                        "\n" +
                        "    EasyEntityQuery entityQuery = new DefaultEasyEntityQuery(queryClient);\n" +

                        String.join("\n", varList) +

                        "String sql = " + selectedText + ".toSQL();" +
                        // TODO 这里的SQL 需要格式化
                        "        System.out.println(sql);\n" +

                        "    }", psiClassCopied);

        // 添加 Main 方法
        psiClassCopied.add(mainMethod);

        // 为了测试方便, 这里需要把DataSource 的一些接口给实现上
        addDataSourceInterfaceImplement(psiClassCopied, elementFactory);

        handleImport(psiJavaFileCopied, elementFactory, project);

        // TODO 还需要引入外部包

        // TODO 方法中可能引用了 其他实体作为参数

        // 修改下 psiClassCopied 的类名
        psiClassCopied.setName("EasyQueryPreviewSqlAction");
        // 修改下文件名
        psiJavaFileCopied.setName("EasyQueryPreviewSqlAction.java");

        String qualifiedName = psiJavaFileCopied.getPackageName() + "." + psiClassCopied.getName();

        // 替换类
        PsiTreeUtil.getStubChildOfType(psiJavaFileCopied, PsiClass.class).replace(psiClassCopied);

        System.out.println(psiClassCopied.getText());
        System.out.println(psiJavaFileCopied.getText());

        PsiDirectory containingDirectory = psiJavaFileSource.getContainingDirectory();

        WriteCommandAction.runWriteCommandAction(event.getProject(), () -> {

            PsiFile file = containingDirectory.findFile(psiJavaFileCopied.getName());
            if (ObjectUtil.isNotNull(file)) {
                file.delete();
            }
            PsiElement psiElementFormated = containingDirectory
                    .add(CodeStyleManager.getInstance(project).reformat(psiJavaFileCopied));
            VirtualFile virtualFile = psiElementFormated.getContainingFile().getVirtualFile();

            // 编辑器打开这个文件
            FileEditorManager.getInstance(project).openFile(virtualFile, true);

            // 编译文件
            CompilerManager compilerManager = CompilerManager.getInstance(project);
            compilerManager.compile(new VirtualFile[] { virtualFile }, new CompileStatusNotification() {
                @Override
                public void finished(boolean aborted, int errors, int warnings,
                                     @NotNull CompileContext compileContext) {
                    if (errors > 0) {
                        System.err.println("编译出错");
                        return;
                    }

                    RunManager runManager = RunManager.getInstance(project);
                    ConfigurationType configurationType = ConfigurationTypeUtil
                            .findConfigurationType(ApplicationConfigurationType.class);
                    ConfigurationFactory configurationFactory = configurationType.getConfigurationFactories()[0];

                    // 获取配置模板
                    RunnerAndConfigurationSettings templateSettings = runManager
                            .getConfigurationTemplate(configurationFactory);

                    RunnerAndConfigurationSettings easyQueryPreviewSqlSettings = runManager
                            .createConfiguration("EasyQuery Preview SQL", templateSettings.getFactory());
                    easyQueryPreviewSqlSettings.setTemporary(true); // 设为临时的
                    ApplicationConfiguration runConfiguration = (ApplicationConfiguration) easyQueryPreviewSqlSettings
                            .getConfiguration();

                    runConfiguration.setMainClassName(qualifiedName);
                    runConfiguration.setModule(currentModule);

                    runManager.addConfiguration(easyQueryPreviewSqlSettings);

                    // 执行配置
                    // 执行配置并获取结果
                    ExecutionEnvironmentBuilder builder;
                    try {
                        builder = ExecutionEnvironmentBuilder.create(DefaultRunExecutor.getRunExecutorInstance(),
                                easyQueryPreviewSqlSettings);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                        return;
                    }
                    ExecutionEnvironment environment = builder.build();

                    if (!StrUtil.equalsAny(runMode, "auto")) {
                        ProgramRunnerUtil.executeConfiguration(environment, true, true);
                        return;
                    }

                    JavaCommandLineState commandLineState = new JavaCommandLineState(environment) {
                        @Override
                        protected JavaParameters createJavaParameters() throws ExecutionException {
                            JavaParameters params = new JavaParameters();
                            params.setMainClass(qualifiedName);
                            params.configureByModule(currentModule, JavaParameters.JDK_AND_CLASSES);
                            return params;
                        }
                    };
                    try {
                        ProgramRunner<?> programRunner = ProgramRunnerUtil.getRunner(DefaultRunExecutor.EXECUTOR_ID,
                                easyQueryPreviewSqlSettings);
                        ExecutionResult executionResult = commandLineState
                                .execute(DefaultRunExecutor.getRunExecutorInstance(), programRunner);
                        ProcessHandler processHandler = executionResult.getProcessHandler();
                        processHandler.addProcessListener(new ProcessAdapter() {
                            @Override
                            public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                                String text = event.getText();
                                if (!StrUtil.startWithAnyIgnoreCase(text, "select", "insert", "update", "delete",
                                        "truncate")) {
                                    System.out.println(text);
                                    return;
                                }

                                CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
                                PsiFileFactory psiFileFactory = PsiFileFactory.getInstance(project);
                                PsiFile sqlFile = psiFileFactory.createFileFromText(
                                        "preview-easy-query.sql", MysqlDialect.INSTANCE, text, false, false);

                                PsiElement formattedElement = codeStyleManager.reformat(sqlFile, false);
                                // 更新到 sqlFile
                                // String formattedSQL = extractFormattedSQL(formattedElement);
                                String formattedText = formattedElement.getText();
                                System.out.println(formattedText);
                                // System.out.println(formattedSQL);
                                SQLPreviewDialog sqlPreviewDialog = new SQLPreviewDialog(formattedText);
                                SwingUtilities.invokeLater(() -> {
                                    sqlPreviewDialog.setVisible(true);
                                });
                            }

                            @Override
                            public void processTerminated(@NotNull ProcessEvent event) {
                                WriteCommandAction.runWriteCommandAction(project, () -> {
                                    try {
                                        virtualFile.delete(this);
                                    } catch (IOException e) {
                                        log.warn("删除文件失败");
                                    }
                                });
                            }
                        });
                        processHandler.startNotify();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }

                }
            });

        });

    }

    private static @NotNull List<String> constructSearchReq(List<PsiReferenceExpression> refOrVarList,
                                                            PsiElementFactory elementFactory, PsiClass psiClassSource) {
        Set<String> varRegistered = Sets.newHashSet();
        List<String> varList = Lists.newArrayList();
        for (PsiReferenceExpression varRef : refOrVarList) {
            PsiElement varEle = varRef.resolve();
            if (varEle instanceof PsiLocalVariable || varEle instanceof PsiParameter) {
                PsiType type = varEle instanceof PsiLocalVariable ? ((PsiLocalVariable) varEle).getType()
                        : ((PsiParameter) varEle).getType();
                String varName = varEle instanceof PsiLocalVariable ? ((PsiLocalVariable) varEle).getName()
                        : ((PsiParameter) varEle).getName();
                String varType = type.getCanonicalText();
                if (varRegistered.contains(varName)) {
                    continue;
                }
                // 判断是否是基本类型或包装类
                boolean isPriType = type instanceof PsiPrimitiveType ||
                        Arrays.asList("java.lang.Integer", "java.lang.Long", "java.lang.Double",
                                        "java.lang.Float", "java.lang.Boolean", "java.lang.Byte",
                                        "java.lang.Short", "java.lang.Character")
                                .contains(varType);

                varRegistered.add(varName);

                // 定义变量
                String varDef;
                if (isPriType) {
                    varDef = varType + " " + varName + " = "+ getMockValue(varType) + ";";
                    varList.add(varDef);
                } else {
                    // 处理复杂类型
                    varDef = varType + " " + varName + " = new " + varType + "();";
                    PsiElement varDefEle = elementFactory.createStatementFromText(varDef, psiClassSource);
                    String varDefEleText = varDefEle.getText();
                    varList.add(varDefEleText);

                    // 获取类型的所有setter方法并设置模拟值
                    PsiClass targetClass = PsiJavaFileUtil.getPsiClass(psiClassSource.getProject(), varType);
                    if (targetClass != null) {
                        PsiMethod[] methods = targetClass.getMethods();
                        for (PsiMethod method : methods) {
                            if (method.getName().startsWith("set")) {
                                PsiParameter[] parameters = method.getParameterList().getParameters();
                                if (parameters.length == 1) {
                                    PsiType paramType = parameters[0].getType();
                                    String paramTypeName = paramType.getCanonicalText();
                                    String mockValue = getMockValue(paramTypeName);
                                    if (mockValue != null) {
                                        varList.add(varName + "." + method.getName() + "(" + mockValue + ");");
                                    }
                                }
                            }
                        }
                    }
                }

            }
        }
        return varList;
    }

    // 新增辅助方法生成模拟值
    private static String getMockValue(String typeName) {
        // 处理泛型集合类型
        if (typeName.startsWith("java.util.List<") || typeName.startsWith("java.util.ArrayList<")) {
            String genericType = extractGenericType(typeName);
            return String.format("java.util.Arrays.asList(%s, %s)", getMockValue(genericType),
                    getMockValue(genericType));
        }

        if (typeName.startsWith("java.util.Set<") || typeName.startsWith("java.util.HashSet<")) {
            String genericType = extractGenericType(typeName);
            return String.format("new java.util.HashSet<>(java.util.Arrays.asList(%s, %s))",
                    getMockValue(genericType), getMockValue(genericType));
        }

        if (typeName.startsWith("java.util.Map<")) {
            String[] genericTypes = extractMapGenericTypes(typeName);
            String keyType = genericTypes[0];
            String valueType = genericTypes[1];
            return String.format("new java.util.HashMap<>() {{ put(%s, %s); put(%s, %s); }}",
                    getMockValue(keyType), getMockValue(valueType),
                    getMockValue(keyType), getMockValue(valueType));
        }

        // 原有的基础类型处理
        switch (typeName) {
            case "java.lang.String":
                return "\"" + RandomUtil.randomString(RandomUtil.randomInt(1, 10)) + "\"";
            case "java.lang.Long":
            case "long":
                return RandomUtil.randomLong() + "L";
            case "java.lang.Integer":
            case "int":
                return String.valueOf(RandomUtil.randomInt());
            case "java.lang.Double":
            case "double":
                return RandomUtil.randomDouble() + "d";
            case "java.lang.Boolean":
            case "boolean":
                return String.valueOf(RandomUtil.randomBoolean());
            case "java.math.BigDecimal":
                return "new java.math.BigDecimal(\"" + RandomUtil.randomDouble() + "\")";
            case "java.time.LocalDate":
                return "java.time.LocalDate.now()";
            case "java.time.LocalDateTime":
                return "java.time.LocalDateTime.now()";
            default:
                return null;
        }
    }

    // 提取泛型类型
    private static String extractGenericType(String typeName) {
        int start = typeName.indexOf('<');
        int end = typeName.lastIndexOf('>');
        if (start != -1 && end != -1) {
            return typeName.substring(start + 1, end).trim();
        }
        return "java.lang.Object";
    }

    // 提取Map的键值泛型类型
    private static String[] extractMapGenericTypes(String typeName) {
        int start = typeName.indexOf('<');
        int end = typeName.lastIndexOf('>');
        if (start != -1 && end != -1) {
            String generics = typeName.substring(start + 1, end).trim();
            String[] types = generics.split(",");
            if (types.length == 2) {
                return new String[] { types[0].trim(), types[1].trim() };
            }
        }
        return new String[] { "java.lang.Object", "java.lang.Object" };
    }

    /**
     * 处理import
     */
    private static void handleImport(PsiJavaFile psiJavaFileCopied, PsiElementFactory elementFactory, Project project) {
        // 添加EQ相关import
        // import com.easy.query.core.logging.LogFactory
        psiJavaFileCopied.getImportList().add(elementFactory
                .createImportStatement(PsiJavaFileUtil.getPsiClass(project, "com.easy.query.core.logging.LogFactory")));
        // import com.easy.query.core.api.client.EasyQueryClient
        psiJavaFileCopied.getImportList().add(elementFactory.createImportStatement(
                PsiJavaFileUtil.getPsiClass(project, "com.easy.query.core.api.client.EasyQueryClient")));
        // import com.easy.query.core.bootstrapper.EasyQueryBootstrapper
        psiJavaFileCopied.getImportList().add(elementFactory.createImportStatement(
                PsiJavaFileUtil.getPsiClass(project, "com.easy.query.core.bootstrapper.EasyQueryBootstrapper")));
        // import com.easy.query.mssql.config.MsSQLDatabaseConfiguration
        psiJavaFileCopied.getImportList().add(elementFactory.createImportStatement(
                PsiJavaFileUtil.getPsiClass(project, "com.easy.query.mssql.config.MsSQLDatabaseConfiguration")));
        // import java.sql.Connection
        psiJavaFileCopied.getImportList()
                .add(elementFactory.createImportStatement(PsiJavaFileUtil.getPsiClass(project, "java.sql.Connection")));
        // import java.sql.SQLException
        psiJavaFileCopied.getImportList().add(
                elementFactory.createImportStatement(PsiJavaFileUtil.getPsiClass(project, "java.sql.SQLException")));
        // import java.io.PrintWriter
        psiJavaFileCopied.getImportList()
                .add(elementFactory.createImportStatement(PsiJavaFileUtil.getPsiClass(project, "java.io.PrintWriter")));
        // import java.util.logging.Logger
        psiJavaFileCopied.getImportList().add(
                elementFactory.createImportStatement(PsiJavaFileUtil.getPsiClass(project, "java.util.logging.Logger")));
        // import java.sql.SQLFeatureNotSupportedException
        psiJavaFileCopied.getImportList().add(elementFactory.createImportStatement(
                PsiJavaFileUtil.getPsiClass(project, "java.sql.SQLFeatureNotSupportedException")));
        // com.easy.query.api.proxy.client.DefaultEasyEntityQuery
        psiJavaFileCopied.getImportList().add(elementFactory.createImportStatement(
                PsiJavaFileUtil.getPsiClass(project, "com.easy.query.api.proxy.client.DefaultEasyEntityQuery")));
    }

    private void addDataSourceInterfaceImplement(PsiClass psiClassCopied, PsiElementFactory elementFactory) {
        PsiMethod getConnectionMethod = elementFactory
                .createMethodFromText("public Connection getConnection() throws SQLException {\n" +
                        "    return null;\n" +
                        "  }", psiClassCopied);
        psiClassCopied.add(getConnectionMethod);

        PsiMethod getConnectionMethod2 = elementFactory.createMethodFromText(
                "public Connection getConnection(String username, String password) throws SQLException {\n" +
                        "    return null;\n" +
                        "  }",
                psiClassCopied);
        psiClassCopied.add(getConnectionMethod2);

        PsiMethod getLogWriterMethod = elementFactory
                .createMethodFromText("public PrintWriter getLogWriter() throws SQLException {\n" +
                        "    return null;\n" +
                        "  }", psiClassCopied);
        psiClassCopied.add(getLogWriterMethod);

        PsiMethod setLogWriterMethod = elementFactory
                .createMethodFromText("public void setLogWriter(PrintWriter out) throws SQLException {\n" +
                        "\n" +
                        "  }", psiClassCopied);
        psiClassCopied.add(setLogWriterMethod);

        PsiMethod setLoginTimeoutMethod = elementFactory
                .createMethodFromText("public void setLoginTimeout(int seconds) throws SQLException {\n" +
                        "\n" +
                        "  }", psiClassCopied);
        psiClassCopied.add(setLoginTimeoutMethod);

        PsiMethod getLoginTimeoutMethod = elementFactory
                .createMethodFromText("public int getLoginTimeout() throws SQLException {\n" +
                        "    return 0;\n" +
                        "  }", psiClassCopied);
        psiClassCopied.add(getLoginTimeoutMethod);

        PsiMethod getParentLoggerMethod = elementFactory
                .createMethodFromText("public Logger getParentLogger() throws SQLFeatureNotSupportedException {\n" +
                        "    return null;\n" +
                        "  }", psiClassCopied);
        psiClassCopied.add(getParentLoggerMethod);

        PsiMethod unwrapMethod = elementFactory
                .createMethodFromText("public <T> T unwrap(Class<T> iface) throws SQLException {\n" +
                        "    return null;\n" +
                        "  }", psiClassCopied);
        psiClassCopied.add(unwrapMethod);

        PsiMethod isWrapperForMethod = elementFactory
                .createMethodFromText("public boolean isWrapperFor(Class<?> iface) throws SQLException {\n" +
                        "    return false;\n" +
                        "  }", psiClassCopied);
        psiClassCopied.add(isWrapperForMethod);

    }

    private void updateGitignore(Project project) {
        VirtualFile baseDir = project.getBaseDir();
        VirtualFile gitignoreFile = baseDir.findChild(".gitignore");

        if (gitignoreFile != null) {
            appendToGitignoreFile(project, gitignoreFile);
        }
    }

    private void appendToGitignoreFile(Project project, VirtualFile gitignoreFile) {
        try {
            String content = new String(gitignoreFile.contentsToByteArray());
            if (!content.contains("EasyQueryPreviewSqlAction.java")) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    try {
                        String newContent = content.endsWith("\n") ? content + "EasyQueryPreviewSqlAction.java\n"
                                : content + "\nEasyQueryPreviewSqlAction.java\n";
                        gitignoreFile.setBinaryContent(newContent.getBytes());
                    } catch (IOException e) {
                        log.error("更新 .gitignore 文件失败", e);
                    }
                });
            }
        } catch (IOException e) {
            log.error("读取 .gitignore 文件失败", e);
        }
    }

    // 在constructSearchReq方法中添加对集合类型的导入处理
    private static void addCollectionImports(PsiJavaFile psiJavaFile, PsiElementFactory elementFactory) {
        psiJavaFile.getImportList().add(elementFactory.createImportStatement(
                PsiJavaFileUtil.getPsiClass(psiJavaFile.getProject(), "java.util.Arrays")));
        psiJavaFile.getImportList().add(elementFactory.createImportStatement(
                PsiJavaFileUtil.getPsiClass(psiJavaFile.getProject(), "java.util.ArrayList")));
        psiJavaFile.getImportList().add(elementFactory.createImportStatement(
                PsiJavaFileUtil.getPsiClass(psiJavaFile.getProject(), "java.util.HashSet")));
        psiJavaFile.getImportList().add(elementFactory.createImportStatement(
                PsiJavaFileUtil.getPsiClass(psiJavaFile.getProject(), "java.util.HashMap")));
    }


}
