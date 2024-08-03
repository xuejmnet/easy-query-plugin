//package com.easy.query.plugin.action;
//
//import com.easy.query.plugin.core.config.CustomConfig;
//import com.easy.query.plugin.core.config.EasyQueryConfig;
//import com.easy.query.plugin.core.persistent.EasyQueryQueryPluginConfigData;
//import com.easy.query.plugin.core.util.NotificationUtils;
//import com.easy.query.plugin.core.util.StrUtil;
//import com.easy.query.plugin.core.util.VirtualFileUtils;
//import com.easy.query.plugin.core.validator.InputAnyValidatorImpl;
//import com.intellij.openapi.actionSystem.ActionUpdateThread;
//import com.intellij.openapi.actionSystem.AnAction;
//import com.intellij.openapi.actionSystem.AnActionEvent;
//import com.intellij.openapi.actionSystem.CommonDataKeys;
//import com.intellij.openapi.actionSystem.DataContext;
//import com.intellij.openapi.command.WriteCommandAction;
//import com.intellij.openapi.editor.Editor;
//import com.intellij.openapi.module.Module;
//import com.intellij.openapi.module.ModuleUtil;
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.ui.Messages;
//import com.intellij.psi.PsiClassOwner;
//import com.intellij.psi.PsiElementFactory;
//import com.intellij.psi.PsiField;
//import com.intellij.psi.PsiFile;
//import com.intellij.psi.PsiIdentifier;
//import com.intellij.psi.PsiJavaFile;
//import com.intellij.psi.PsiReference;
//import com.intellij.psi.search.GlobalSearchScope;
//import com.intellij.psi.search.searches.ReferencesSearch;
//import org.jetbrains.kotlin.psi.KtFile;
//
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.function.Function;
//import java.util.stream.Collectors;
//
///**
// * create time 2024/7/18 17:22
// * 文件说明
// *
// * @author xuejiaming
// */
//public class RenameAptAction  extends AnAction {
//    @Override
//    public void actionPerformed(AnActionEvent e) {
//        // TODO: insert action logic here
//        Project project = e.getProject();
//        if(project==null){
//            return;
//        }
//        String input = Messages.showInputDialog(project, "请输入新的字段名", "重命名", Messages.getQuestionIcon());
//        if (StrUtil.isEmpty(input)) {
//            Messages.showWarningDialog(project, "请输入新的字段名", "警告");
//            return;
//        }
//        DataContext dataContext = e.getDataContext();
//        Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
//        if(editor==null){
//            NotificationUtils.notifyError("未找到文档编辑Editor", "提示", project);
//            return;
//        }
//
//        PsiFile psiFile = VirtualFileUtils.getPsiFile(project,editor.getDocument());
//        if (!(psiFile instanceof PsiJavaFile) && !(psiFile instanceof KtFile)) {
//            NotificationUtils.notifyError("请选择kt或者java文件", "提示", project);
//            return;
//        }
//        Map<String, PsiClassOwner> tableDefMap =  TableDefUtils.getDependenciesTableDef(psiFile.getVirtualFile());
//        PsiClassOwner classOwner = (PsiClassOwner) psiFile;
//        String key = classOwner.getPackageName() + "." + classOwner.getClasses()[0].getName();
//        PsiClassOwner javaFile = tableDefMap.get(key);
//
//        Map<String, PsiField> fieldMap = Arrays.stream(javaFile.getClasses()[0].getFields())
//                .collect(Collectors.toMap(PsiField::getName, Function.identity()));
//        Module moduleForFile = ModuleUtil.findModuleForFile(psiFile.getVirtualFile(), project);
//        CustomConfig config = Modules.moduleConfig(moduleForFile);
//
//        String selectedText = MybatisFlexUtil.getSelectedText(e);
//        PsiField element = fieldMap.get(getDefInstanceName(config, selectedText, false));
//        String newInstanceName = getDefInstanceName(config, input, false);
//        PsiElementFactory instance = PsiElementFactory.getInstance(ProjectUtils.getCurrentProject());
//        replace(classOwner, input, selectedText, instance);
//        if (element != null) {
//            // 使用 ReferencesSearch 来查找引用
//            ReferencesSearch.SearchParameters searchParameters = new ReferencesSearch.SearchParameters(element, GlobalSearchScope.allScope(project), false);
//            PsiReference[] references = ReferencesSearch.search(searchParameters).findAll().toArray(PsiReference.EMPTY_ARRAY);
//            PsiIdentifier identifier = instance.createIdentifier(newInstanceName);
//            WriteCommandAction.runWriteCommandAction(project, () -> {
//                element.replace(identifier);
//                for (PsiReference reference : references) {
//                    reference.getElement().getLastChild().replace(identifier);
//                }
//            });
//        } else {
//            Messages.showMessageDialog(project, "No usages found for " + input, "Usages of " + input, null);
//        }
//    }
//
//    /**
//     * 替换当前 class 中的属性名
//     *
//     * @param classOwner
//     * @param newField
//     * @param instance
//     */
//    public void replace(PsiClassOwner classOwner, String newField, String oldField, PsiElementFactory instance) {
//        PsiIdentifier identifier = instance.createIdentifier(newField);
//        Arrays.stream(classOwner.getClasses()[0].getFields())
//                .filter(el -> oldField.equals(el.getName()))
//                .findAny()
//                .ifPresent(el -> {
//                    WriteCommandAction.runWriteCommandAction(ProjectUtils.getCurrentProject(), () -> {
//                        el.getNameIdentifier().replace(identifier);
//                    });
//                });
//    }
//
//    @Override
//    public @NotNull ActionUpdateThread getActionUpdateThread() {
//        return ActionUpdateThread.BGT;
//    }
//    @Override
//    public void update(AnActionEvent e) {
//        e.getPresentation().setEnabled(PsiJavaFileUtil.isFlexProject());
//    }
//}