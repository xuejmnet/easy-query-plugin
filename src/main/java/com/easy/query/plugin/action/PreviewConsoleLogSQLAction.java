package com.easy.query.plugin.action;

import com.easy.query.plugin.core.expression.SimpleFunction;
import com.easy.query.plugin.windows.SQLPreviewDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;

/**
 * 预览控制台日志的SQL
 */
public class PreviewConsoleLogSQLAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        // 如果是从项目视图中右键点击的进来的则创建新的类
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor != null) {
            String selectedText = editor.getSelectionModel().getSelectedText();
            preview(selectedText, () -> {
            });
        }
    }


    //预览
    public void preview(String selectedText, SimpleFunction function) {
        if (StringUtils.isBlank(selectedText)) {
            return;
        }
//        &&StringUtils.containsIgnoreCase(selectedText,"Preparing")
//                &&StringUtils.containsIgnoreCase(selectedText,"Parameters")
        try {
            SQLPreviewDialog sqlPreviewDialog = new SQLPreviewDialog(selectedText);
            SwingUtilities.invokeLater(() -> {
                sqlPreviewDialog.setVisible(true);
            });
//            System.out.println("预览选择文字:"+selectedText);
//            String[] multiLogs = selectedText.split("\n");
//            if (selectedText.startsWith("QueryWrapper")) {
//                selectedText = String.format(SYSTEM_OUT_PRINTLN_TO_SQL, selectedText);
////            } else {
////                // 处理链式调用
////                selectedText = chain(selectedText, psiFile);
//            }
//            createFile(project,psiFile, StrUtil.format(CLASS_TEMPLATE, selectedText), psiFile.getPackageName());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // new File("MybatisFlexSqlPreview").delete();
            function.apply();
        }
    }

//    private void createFile(Project project,PsiJavaFile psiFile, String text, String packageName) {
//        PsiJavaFile psiJavaFile = (PsiJavaFile) PsiFileFactory.getInstance(project).createFileFromText("EasyQuerySqlPreview.java", JavaFileType.INSTANCE, text);
//        psiJavaFile.setPackageName(psiFile.getPackageName());
//        PsiImportList importList = psiFile.getImportList();
//        PsiDirectory containingDirectory = psiFile.getContainingDirectory();
//        WriteCommandAction.runWriteCommandAction(project, () -> {
//            PsiImportList psiJavaFileImportList = psiJavaFile.getImportList();
//            if (ObjectUtil.isNotNull(importList)) {
//                Arrays.stream(importList.getAllImportStatements())
//                        .forEach(el -> {
//                            assert psiJavaFileImportList != null;
//                            psiJavaFileImportList.add(el);
//                        });
//            }
//            if (CollUtil.isNotEmpty(list)) {
//                for (String impor : list) {
//                    psiJavaFileImportList.add(instance.createImportStatement(PsiJavaFileUtil.getPsiClass(impor)));
//                }
//            }
//            try {
//                PsiFile file = containingDirectory.findFile(psiJavaFile.getName());
//                if (ObjectUtil.isNotNull(file)) {
//                    file.delete();
//                }
//
//                PsiElement element = containingDirectory.add(CodeReformat.reformat(psiJavaFile));
//                VirtualFile virtualFile = element.getContainingFile().getVirtualFile();
//                showSql(project, packageName, virtualFile);
//                // compileJavaCode(psiJavaFile.getText());
//                // showSql(project, packageName, null);
//
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//    }
//
//    private void showSql(Project project, String packageName, VirtualFile virtualFile) {
//        ArrayList<VirtualFile> virtualFiles = new ArrayList<VirtualFile>();
//        if (ObjectUtil.isNotNull(entityClass)) {
//            virtualFiles.add(entityClass.getContainingFile().getVirtualFile());
//        }
//        virtualFiles.add(virtualFile);
//        CompilerManagerUtil.compile(virtualFiles.toArray(new VirtualFile[0]), (b, i, i1, compileContext) -> {
//            try {
//                ApplicationManager.getApplication().invokeLater(() -> {
//                    WriteCommandAction.runWriteCommandAction(project, () -> {
//                        try {
//                            if (!MybatisFlexSettingDialog.insideSchemaFlag) {
//                                virtualFile.delete(this);
//                            }
//                            if (ObjectUtil.isNotNull(entityClass)) {
//                                removeNoArgsConstructor(entityClass);
//                            }
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    });
//                }, ModalityState.defaultModalityState());
//                // 执行配置
//                ProgramRunner runner = ProgramRunner.PROGRAM_RUNNER_EP.getExtensions()[0];
//                Executor instance = MyBatisLogExecutor.getInstance();
//                RunManagerEx runManager = (RunManagerEx) RunManager.getInstance(project);
//                RunnerAndConfigurationSettings defaultSettings = runManager.getSelectedConfiguration();
//                ExecutionEnvironment environment = new ExecutionEnvironment(instance, runner, defaultSettings, project);
//                // 创建 Java 执行配置
//                JavaCommandLineState commandLineState = new JavaCommandLineState(environment) {
//                    @Override
//                    protected JavaParameters createJavaParameters() throws ExecutionException {
//                        JavaParameters params = new JavaParameters();
//                        Sdk projectSdk = ProjectRootManager.getInstance(project).getProjectSdk();
//                        params.configureByProject(project, JavaParameters.JDK_AND_CLASSES_AND_TESTS, projectSdk);
//                        // params.setMainClass(packageName + ".MybatisFlexSqlPreview");
//                        params.setMainClass(packageName + ".EasyQuerySqlPreview");
//                        return params;
//                    }
//                };
//                ExecutionResult executionResult = commandLineState.execute(instance, runner);
//                if (ObjectUtil.isNotNull(executionResult)) {
//                    ProcessHandler processHandler = executionResult.getProcessHandler();
//                    processHandler.addProcessListener(new ProcessAdapter() {
//                        @Override
//                        public void onTextAvailable(ProcessEvent event1, Key outputType) {
//                            if (ProcessOutputTypes.STDOUT.equals(outputType)) {
//                                if (StrUtil.startWithAnyIgnoreCase(event1.getText(), "SELECT", "INSERT", "UPDATE", "DELETE", "CREATE", "DROP", "ALTER", "TRUNCATE")) {
//                                    new SQLPreviewDialog(event1.getText()).setVisible(true);
//                                }
//                            } else if (ProcessOutputTypes.STDERR.equals(outputType)) {
//                                String text = event1.getText();
//                                System.out.println(text);
//                                if (text.startsWith("Exception in")) {
//                                    NotificationUtils.notifyError((StrUtil.subAfter(text, ":", true)), "Mybatis-Flex system tips");
//                                }
//                            }
//                        }
//                    });
//                    processHandler.startNotify();
//                }
//            } catch (
//                    Exception e) {
//                Messages.showErrorDialog("Error executing code:\n" + e.getMessage(), "Code Execution Error");
//            }
//        });
//    }
}
