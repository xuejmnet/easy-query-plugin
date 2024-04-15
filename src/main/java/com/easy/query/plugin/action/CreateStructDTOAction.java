package com.easy.query.plugin.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.ClassNode;
import com.easy.query.plugin.core.entity.ClassNodeCirculateChecker;
import com.easy.query.plugin.core.entity.ClassNodePropPath;
import com.easy.query.plugin.core.entity.struct.StructDTOContext;
import com.easy.query.plugin.core.entity.struct.StructDTOEntityContext;
import com.easy.query.plugin.core.util.ClassUtil;
import com.easy.query.plugin.core.util.MyModuleUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.PsiUtil;
import com.easy.query.plugin.windows.EntitySelectDialog;
import com.easy.query.plugin.windows.StructDTODialog;
import com.intellij.concurrency.ThreadContext;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.impl.file.PsiJavaDirectoryImpl;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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
        if(psiElement instanceof PsiJavaDirectoryImpl){
            PsiJavaDirectoryImpl psiJavaDirectory = (PsiJavaDirectoryImpl) psiElement;
            String path = psiJavaDirectory.getVirtualFile().getPath();
            if(StrUtil.isNotBlank(path)){
                if(path.contains("src/main/java/")){
                    String comPath = StrUtil.subAfter(path, "src/main/java/", true);
                    String packageName = comPath.replaceAll("/", ".");

                    Collection<PsiClass> entityClass = PsiJavaFileUtil.getAnnotationPsiClass(project, "com.easy.query.core.annotation.Table");
                    Map<String, PsiClass> entityWithClass = entityClass.stream().collect(Collectors.toMap(o -> o.getQualifiedName(), o -> o));
//                    Map<String,Map<String,ClassNode>> entityProps=new HashMap<>();
//                    List<ClassNode> classNodes = new ArrayList<>();
//                    LinkedHashSet<String> imports = new LinkedHashSet<>();
//                    //循环嵌套的检测
//                    parseClassList(project, entityWithClass,entityProps, classNodes,imports);
                    Module[] modules = MyModuleUtil.getModules(project);
                    Module module = Arrays.stream(modules).filter(o -> {
                        String modulePath = MyModuleUtil.getModulePath(o, JavaModuleSourceRootTypes.SOURCES);
                        return StringUtils.isNotBlank(modulePath)&&path.startsWith(modulePath);
                    }).findFirst().orElse(null);
                    if(module==null){
                        Messages.showErrorDialog(project, "无法找到对应模块", "错误提示");
                        return;
                    }


                    StructDTOEntityContext structDTOEntityContext = new StructDTOEntityContext(project, path, packageName, module, entityWithClass);
                    EntitySelectDialog entitySelectDialog = new EntitySelectDialog(structDTOEntityContext);
                    SwingUtilities.invokeLater(() -> {
                        entitySelectDialog.setVisible(true);
                    });

//                    StructDTOContext structDTOContext = new StructDTOContext(project,path, packageName, module,entityProps);
//                    structDTOContext.getImports().addAll(imports);
////        new TestCaseConfigUI().showUI(project);
//                    StructDTODialog structDTODialog = new StructDTODialog(structDTOContext,classNodes);
////                    try (AccessToken accessToken = ThreadContext.resetThreadContext()) {
//                        structDTODialog.setVisible(true);
//                    }
                }
            }
        }
//        String fullClassName = "org.example.entity.CityEntity";
//        PsiClass psiClass = ClassUtil.findClass(project,fullClassName,true);
//        String entityName = psiClass.getName();
//        String entityFullName = psiClass.getQualifiedName();
    }
}
