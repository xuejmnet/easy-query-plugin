package com.easy.query.plugin.action;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.struct.StructDTOEntityContext;
import com.easy.query.plugin.core.util.MyModuleUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.windows.EntitySelectDialog;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl;

import javax.swing.*;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * create time 2024/2/29 16:44
 * 文件说明
 *
 * @author xuejiaming
 */
public class CreateStructDTOAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getProject();
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
        if (psiElement instanceof PsiJavaDirectoryImpl) {
            PsiJavaDirectoryImpl psiJavaDirectory = (PsiJavaDirectoryImpl) psiElement;
            String path = psiJavaDirectory.getVirtualFile().getPath();
            if (StrUtil.isNotBlank(path)) {
                if (path.contains("src/main/java/")) {
                    String comPath = StrUtil.subAfter(path, "src/main/java/", true);
                    String packageName = comPath.replaceAll("/", ".");

                    Collection<PsiClass> entityClass = PsiJavaFileUtil.getAnnotationPsiClass(project, "com.easy.query.core.annotation.Table");
                    Map<String, PsiClass> entityWithClass = entityClass.stream().collect(Collectors.toMap(o -> o.getQualifiedName(), o -> o));
//                    Map<String,Map<String,ClassNode>> entityProps=new HashMap<>();
//                    List<ClassNode> classNodes = new ArrayList<>();
//                    LinkedHashSet<String> imports = new LinkedHashSet<>();
//                    //循环嵌套的检测
//                    parseClassList(project, entityWithClass,entityProps, classNodes,imports);
                    Module module = MyModuleUtil.getModuleForFile(project, psiJavaDirectory.getVirtualFile());
                    if (module == null) {
                        Messages.showErrorDialog(project, "无法找到对应模块", "错误提示");
                        return;
                    }


                    StructDTOEntityContext structDTOEntityContext = new StructDTOEntityContext(project, path, packageName, module, entityWithClass);
                    EntitySelectDialog entitySelectDialog = new EntitySelectDialog(structDTOEntityContext);
                    SwingUtilities.invokeLater(() -> {
                        entitySelectDialog.setVisible(true);
                    });

                }
            }
        }else{
            Messages.showErrorDialog(project, "请选择对应的包而不是类或者其他地方右键", "错误提示");
        }
    }
}
