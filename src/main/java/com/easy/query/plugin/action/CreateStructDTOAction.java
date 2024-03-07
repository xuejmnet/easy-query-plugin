package com.easy.query.plugin.action;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.ClassNode;
import com.easy.query.plugin.core.entity.ClassNodeCirculateChecker;
import com.easy.query.plugin.core.entity.ClassNodePropPath;
import com.easy.query.plugin.core.entity.struct.StructDTOContext;
import com.easy.query.plugin.core.util.ClassUtil;
import com.easy.query.plugin.core.util.MyModuleUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.PsiUtil;
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
                    Map<String, PsiClass> entityWithClass = entityClass.stream().collect(Collectors.toMap(o -> o.getName(), o -> o));
                    List<ClassNode> classNodes = new ArrayList<>();
                    LinkedHashSet<String> imports = new LinkedHashSet<>();
                    //循环嵌套的检测
                    parseClassList(project, entityWithClass, classNodes,imports);
                    Module[] modules = MyModuleUtil.getModules(project);
                    Module module = Arrays.stream(modules).filter(o -> {
                        String modulePath = MyModuleUtil.getModulePath(o, JavaModuleSourceRootTypes.SOURCES);
                        return StringUtils.isNotBlank(modulePath)&&path.startsWith(modulePath);
                    }).findFirst().orElse(null);
                    if(module==null){
                        Messages.showErrorDialog(project, "无法找到对应模块", "错误提示");
                        return;
                    }
                    StructDTOContext structDTOContext = new StructDTOContext(project,path, packageName, module);
                    structDTOContext.getImports().addAll(imports);
//        new TestCaseConfigUI().showUI(project);
                    StructDTODialog structDTODialog = new StructDTODialog(structDTOContext,classNodes);
//                    try (AccessToken accessToken = ThreadContext.resetThreadContext()) {
                        structDTODialog.setVisible(true);
//                    }
                }
            }
        }
//        String fullClassName = "org.example.entity.CityEntity";
//        PsiClass psiClass = ClassUtil.findClass(project,fullClassName,true);
//        String entityName = psiClass.getName();
//        String entityFullName = psiClass.getQualifiedName();
    }

    private void parseClassList(Project project, Map<String, PsiClass> entityWithClass, List<ClassNode> classNodeList, Set<String>imports) {
        for (Map.Entry<String, PsiClass> rootClassMap : entityWithClass.entrySet()) {
            String key = rootClassMap.getKey();
            PsiClass value = rootClassMap.getValue();
            ClassNode classNode = new ClassNode(key, null, 0, false,true,value.getName());
            classNodeList.add(classNode);

            addClassProps(project, value, classNode, entityWithClass, null,imports);
        }
    }

    private void addClassProps(Project project, PsiClass psiClass, ClassNode classNode, Map<String, PsiClass> entityWithClass, ClassNodeCirculateChecker classNodeCirculateChecker,Set<String> imports) {
        //是否是数据库对象
        PsiAnnotation entityTable = psiClass.getAnnotation("com.easy.query.core.annotation.Table");
        //获取对应的忽略属性
        Set<String> tableIgnoreProperties = PsiUtil.getPsiAnnotationValues(entityTable, "ignoreProperties", new HashSet<>());
        int sort = classNode.getSort()+1;

        PsiField[] fields = psiClass.getAllFields();
        String qualifiedName = psiClass.getQualifiedName();
        String entityName = psiClass.getName();
        for (PsiField field : fields) {
            PsiAnnotation columnIgnore = field.getAnnotation("com.easy.query.core.annotation.ColumnIgnore");
            if (columnIgnore != null) {
                continue;
            }
            String name = field.getName();
            //是否存在忽略属性
            if (!tableIgnoreProperties.isEmpty() && tableIgnoreProperties.contains(name)) {
                continue;
            }
            boolean isBeanProperty = ClassUtil.hasGetterAndSetter(psiClass, name);
            if (!isBeanProperty) {
                continue;
            }
//            for (PsiAnnotation annotation : field.getAnnotations()) {
//                String string = annotation.toString();
//                //((PsiAnnotationStubImpl)((PsiAnnotationImpl) annotation).mySubstrateRef.getStub()).getText()
//                System.out.println(string);
//            }
            String psiFieldComment = PsiUtil.getPsiFieldOnlyComment(field);

            PsiAnnotation column = field.getAnnotation("com.easy.query.core.annotation.Column");
            boolean isPrimary = false;
            if (column != null) {
                String primary = PsiUtil.getPsiAnnotationValue(column, "primary", "");
                isPrimary = Objects.equals("true", primary);
                imports.add("com.easy.query.core.annotation.Column");
            }


            PsiAnnotation navigate = field.getAnnotation("com.easy.query.core.annotation.Navigate");
            PsiAnnotation valueObject = field.getAnnotation("com.easy.query.core.annotation.ValueObject");
            boolean isValueObject = valueObject != null;
            if (isValueObject) {
                continue;
            }

            boolean includeProperty = navigate != null;
            if (!includeProperty) {
                ClassNode navClass = new ClassNode(name, entityName, sort++, isPrimary,false,null);
                navClass.setPropText(field.getText());
                navClass.setComment(psiFieldComment);
                classNode.addChild(navClass);
            } else {
                imports.add("com.easy.query.core.annotation.Navigate");
                imports.add("com.easy.query.core.enums.RelationTypeEnum");
                ClassNodeCirculateChecker circulateChecker = classNodeCirculateChecker == null ? new ClassNodeCirculateChecker(qualifiedName) : classNodeCirculateChecker;

                String selfProperty = PsiUtil.getPsiAnnotationValue(navigate, "selfProperty", "");
                String selfNavigateId=null;
                if(StrUtil.isNotBlank(selfProperty)){
                    selfNavigateId=selfProperty;
                }

                String targetNavigateId=null;
                String targetProperty = PsiUtil.getPsiAnnotationValue(navigate, "targetProperty", "");
                if(StrUtil.isNotBlank(targetProperty)){
                    targetNavigateId=targetProperty;
                }

                String propertyType = PsiUtil.getPsiFieldPropertyType(field, true);
                PsiClass propClass = entityWithClass.get(propertyType);
                if (propClass != null) {
                    if (circulateChecker.pathRepeat(new ClassNodePropPath(qualifiedName, propertyType, name))) {
                        continue;
                    }
                    ClassNode navClass = new ClassNode(name, entityName, sort++, isPrimary,true,propClass.getName());
                    navClass.setSelfNavigateId(selfNavigateId);
                    navClass.setTargetNavigateId(targetNavigateId);
                    navClass.setPropText(field.getText());
                    navClass.setComment(psiFieldComment);
//                    String sub = StrUtil.subAfter(propertyType, ".", true);
                    classNode.addChild(navClass);
                    addClassProps(project, propClass, navClass, entityWithClass, classNodeCirculateChecker,imports);
                } else {
                    PsiClass propertyClass = findClass(project, propertyType);
                    if (propertyClass != null) {
                        if (circulateChecker.pathRepeat(new ClassNodePropPath(qualifiedName, propertyType, name))) {
                            continue;
                        }
                        ClassNode navClass = new ClassNode(name, entityName, sort++, isPrimary,true,propertyClass.getName());
                        navClass.setSelfNavigateId(selfProperty);
                        navClass.setTargetNavigateId(targetNavigateId);
                        navClass.setPropText(field.getText());
                        navClass.setComment(psiFieldComment);
                        classNode.addChild(navClass);
                        addClassProps(project, propertyClass, navClass, entityWithClass, circulateChecker,imports);
                    }
                }

            }

        }
    }


    private PsiClass findClass(Project project, String fullClassName) {
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        PsiClass newClass = javaPsiFacade.findClass(fullClassName, GlobalSearchScope.projectScope(project));
        if (newClass == null) {
            newClass = javaPsiFacade.findClass(fullClassName, GlobalSearchScope.allScope(project));
        }
        return newClass;
    }
}
