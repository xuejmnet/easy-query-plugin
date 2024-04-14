package com.easy.query.plugin.core.util;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.ClassNode;
import com.easy.query.plugin.core.entity.ClassNodeCirculateChecker;
import com.easy.query.plugin.core.entity.ClassNodePropPath;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * create time 2024/3/8 15:23
 * 文件说明
 *
 * @author xuejiaming
 */
public class StructDTOUtil {

    public static void parseClassList(Project project, String entityName,PsiClass entityClass,Map<String, PsiClass> entityWithClass, Map<String, Map<String, ClassNode>> entityProps, List<ClassNode> classNodeList, Set<String> imports,Set<String> ignoreColumns) {

        ClassNode classNode = new ClassNode(entityName, null, 0, false,true, entityClass.getName(), null,null,entityClass.getQualifiedName());
        classNodeList.add(classNode);

        addClassProps(project, entityClass,null, classNode, entityWithClass,entityProps, null,imports,ignoreColumns);
    }

    private static void addClassProps(Project project, PsiClass psiClass, String ownerPropertyName, ClassNode classNode, Map<String, PsiClass> entityWithClass, Map<String,Map<String,ClassNode>> entityProps, ClassNodeCirculateChecker classNodeCirculateChecker, Set<String> imports,Set<String> ignoreColumns) {
        //是否是数据库对象
        PsiAnnotation entityTable = psiClass.getAnnotation("com.easy.query.core.annotation.Table");
        //获取对应的忽略属性
        Set<String> tableIgnoreProperties = PsiUtil.getPsiAnnotationValues(entityTable, "ignoreProperties", new HashSet<>());
        tableIgnoreProperties.addAll(ignoreColumns);
        int sort = classNode.getSort()+1;

        PsiField[] fields = psiClass.getAllFields();
        String qualifiedName = psiClass.getQualifiedName();
        String entityName = psiClass.getName();
        entityProps.putIfAbsent(entityName,new HashMap<>());
        Map<String,ClassNode> classNodes = entityProps.get(entityName);
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
            String conversion = null;
            String columnValue = null;
            if (column != null) {
                String primary = PsiUtil.getPsiAnnotationValue(column, "primary", "");
                isPrimary = Objects.equals("true", primary);
                imports.add("com.easy.query.core.annotation.Column");
                conversion = PsiUtil.getPsiAnnotationValue(column, "conversion", "");
                if(conversion!=null&&conversion.endsWith("DefaultValueConverter.class")){
                    conversion=null;
                }
                columnValue=PsiUtil.getPsiAnnotationValue(column, "value", "");
            }


            PsiAnnotation navigate = field.getAnnotation("com.easy.query.core.annotation.Navigate");
            PsiAnnotation valueObject = field.getAnnotation("com.easy.query.core.annotation.ValueObject");
            boolean isValueObject = valueObject != null;
            if (isValueObject) {
                continue;
            }

            boolean includeProperty = navigate != null;
            if (!includeProperty) {
                ClassNode navClass = new ClassNode(name, entityName, sort++, isPrimary,false,null,ownerPropertyName,null,null);
                navClass.setPropText(field.getText());
                navClass.setComment(psiFieldComment);
                navClass.setConversion(conversion);
                navClass.setColumnValue(columnValue);
                classNode.addChild(navClass);
                classNodes.putIfAbsent(name,navClass);
            } else {
                imports.add("com.easy.query.core.annotation.Navigate");
                imports.add("com.easy.query.core.enums.RelationTypeEnum");
                ClassNodeCirculateChecker circulateChecker = classNodeCirculateChecker == null ? new ClassNodeCirculateChecker(qualifiedName) : classNodeCirculateChecker;

                String relationType = PsiUtil.getPsiAnnotationValue(navigate, "value", "");
                String selfProperty = PsiUtil.getPsiAnnotationValue(navigate, "selfProperty", "");
                String selfNavigateId=null;
                if(cn.hutool.core.util.StrUtil.isNotBlank(selfProperty)){
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
                    ClassNode navClass = new ClassNode(name, entityName, sort++, isPrimary,true,propClass.getName(),ownerPropertyName,qualifiedName,propClass.getQualifiedName());
                    navClass.setSelfNavigateId(selfNavigateId);
                    navClass.setTargetNavigateId(targetNavigateId);
                    navClass.setPropText(field.getText());
                    navClass.setComment(psiFieldComment);
                    navClass.setConversion(conversion);
                    navClass.setColumnValue(columnValue);
                    navClass.setRelationType(relationType);
//                    String sub = StrUtil.subAfter(propertyType, ".", true);
                    classNode.addChild(navClass);
                    classNodes.putIfAbsent(name,navClass);
                    addClassProps(project, propClass,name, navClass, entityWithClass,entityProps, circulateChecker,imports,ignoreColumns);
                } else {
                    PsiClass propertyClass = findClass(project, propertyType);
                    if (propertyClass != null) {
                        if (circulateChecker.pathRepeat(new ClassNodePropPath(qualifiedName, propertyType, name))) {
                            continue;
                        }
                        ClassNode navClass = new ClassNode(name, entityName, sort++, isPrimary,true,propertyClass.getName(),ownerPropertyName,qualifiedName,propertyClass.getQualifiedName());
                        navClass.setSelfNavigateId(selfProperty);
                        navClass.setTargetNavigateId(targetNavigateId);
                        navClass.setPropText(field.getText());
                        navClass.setComment(psiFieldComment);
                        navClass.setConversion(conversion);
                        navClass.setColumnValue(columnValue);
                        navClass.setRelationType(relationType);
                        classNode.addChild(navClass);
                        classNodes.putIfAbsent(name,navClass);
                        addClassProps(project, propertyClass,name, navClass, entityWithClass,entityProps, circulateChecker,imports,ignoreColumns);
                    }
                }

            }

        }
    }


    private static PsiClass findClass(Project project, String fullClassName) {
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        PsiClass newClass = javaPsiFacade.findClass(fullClassName, GlobalSearchScope.projectScope(project));
        if (newClass == null) {
            newClass = javaPsiFacade.findClass(fullClassName, GlobalSearchScope.allScope(project));
        }
        return newClass;
    }
}
