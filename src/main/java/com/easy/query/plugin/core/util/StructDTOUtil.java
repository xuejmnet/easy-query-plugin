package com.easy.query.plugin.core.util;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.ClassNode;
import com.easy.query.plugin.core.entity.ClassNodeCirculateChecker;
import com.easy.query.plugin.core.entity.ClassNodePropPath;
import com.easy.query.plugin.core.enums.BeanPropTypeEnum;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.GlobalSearchScope;

import java.util.*;

/**
 * create time 2024/3/8 15:23
 * 文件说明
 *
 * @author xuejiaming
 */
public class StructDTOUtil {

    public static void parseClassList(int maxDeep, Project project, String entityName, PsiClass entityClass, Map<String, PsiClass> entityWithClass, Map<String, Map<String, ClassNode>> entityProps, List<ClassNode> classNodeList, Set<String> imports, Set<String> ignoreColumns) {

        ClassNode classNode = new ClassNode(entityName, null, 0, false, true, entityClass.getName(), null, null, entityClass.getQualifiedName(), BeanPropTypeEnum.GET);
        classNode.setPsiClass(entityClass);
        classNodeList.add(classNode);

        addClassProps(maxDeep, project, entityClass, null, classNode, entityWithClass, entityProps, null, imports, ignoreColumns, 0);
    }

    private static void addClassProps(int maxDeep, Project project, PsiClass psiClass, String ownerPropertyName,
                                      ClassNode classNode, Map<String, PsiClass> entityWithClass, Map<String, Map<String, ClassNode>> entityProps,
                                      ClassNodeCirculateChecker classNodeCirculateChecker, Set<String> imports, Set<String> ignoreColumns, int deep) {
        //是否是数据库对象
        PsiAnnotation entityTable = psiClass.getAnnotation("com.easy.query.core.annotation.Table");
        //获取对应的忽略属性
        Set<String> tableIgnoreProperties = PsiUtil.getPsiAnnotationValues(entityTable, "ignoreProperties", new HashSet<>());
        tableIgnoreProperties.addAll(ignoreColumns);
        int sort = classNode.getSort() + 1;

        PsiField[] fields = psiClass.getAllFields();
        String qualifiedName = psiClass.getQualifiedName();
        String entityName = psiClass.getName();
        entityProps.putIfAbsent(entityName, new HashMap<>());
        Map<String, ClassNode> classNodes = entityProps.get(entityName);
        for (PsiField psiField : fields) {

            PsiAnnotation psiAnnoColumnIgnore = psiField.getAnnotation("com.easy.query.core.annotation.ColumnIgnore");
            PsiAnnotation psiAnnoColumn = psiField.getAnnotation("com.easy.query.core.annotation.Column");
            PsiAnnotation psiAnnoNavigate = psiField.getAnnotation("com.easy.query.core.annotation.Navigate");
            PsiAnnotation psiAnnoNavigateFlat = psiField.getAnnotation("com.easy.query.core.annotation.NavigateFlat");

            // 判断字段上是否有 @ColumnIgnore 注解, 如果有的话, 说明是忽略字段, 直接跳过

            if (psiAnnoColumnIgnore != null) {
                continue;
            }

            String fieldName = psiField.getName();
            //是否存在忽略属性
            if (!tableIgnoreProperties.isEmpty() && tableIgnoreProperties.contains(fieldName)) {
                continue;
            }

            // 字段不包含 getter 和 setter 直接跳过
            BeanPropTypeEnum beanPropType = ClassUtil.hasGetterAndSetter(psiClass, fieldName);
            if (beanPropType == BeanPropTypeEnum.NOT) {
                continue;
            }

            String psiFieldComment = PsiUtil.getPsiFieldOnlyComment(psiField);


            boolean isPrimary = false;
            String conversion = null;
            String columnValue = null;
            String complexPropType = null;
            if (psiAnnoColumn != null) {
                // 处理 @Column 注解
                // 先把 Column 注解的类导入进来
                imports.add("com.easy.query.core.annotation.Column");

                // primary 和 primaryKey 两个属性都是用来标识是否是主键的
                String primary = PsiUtil.getPsiAnnotationValue(psiAnnoColumn, "primary", "");
                String primaryKey = PsiUtil.getPsiAnnotationValue(psiAnnoColumn, "primaryKey", "");
                isPrimary = Objects.equals("true", primary) || Objects.equals("true", primaryKey);

                // conversion 值转换器 在内存中通过java代码进行转换
                conversion = PsiUtil.getPsiAnnotationValue(psiAnnoColumn, "conversion", "");
                if (conversion != null && conversion.endsWith("DefaultValueConverter.class")) {
                    conversion = null;
                }

                // complexPropType 复杂类型
                complexPropType = PsiUtil.getPsiAnnotationValue(psiAnnoColumn, "complexPropType", "");
                if (StrUtil.isNotBlank(complexPropType) && StrUtil.endWith(complexPropType, "DefaultComplexPropType.class")) {
                    complexPropType = null;
                }

                columnValue = PsiUtil.getPsiAnnotationValue(psiAnnoColumn, "value", "");
            }


            PsiAnnotation navigatePsiAnno = psiField.getAnnotation("com.easy.query.core.annotation.Navigate");
            PsiAnnotation valueObjectPsiAnno = psiField.getAnnotation("com.easy.query.core.annotation.ValueObject");
            boolean isValueObject = valueObjectPsiAnno != null;
            if (isValueObject) {
                continue;
            }

            boolean includeProperty = navigatePsiAnno != null;
            if (!includeProperty) {
                ClassNode navClass = new ClassNode(fieldName, entityName, sort++, isPrimary, false, null, ownerPropertyName, null, null, beanPropType);
                navClass.setPropText(psiField.getText());
                navClass.setComment(psiFieldComment);
                navClass.setConversion(conversion);
                navClass.setColumnValue(columnValue);
                navClass.setComplexPropType(complexPropType);
                navClass.setPsiClass(psiClass);
                navClass.setPsiField(psiField);
                navClass.setPsiAnnoColumn(psiAnnoColumn);
                navClass.setPsiAnnoNavigate(psiAnnoNavigate);
                navClass.setPsiAnnoNavigateFlat(psiAnnoNavigateFlat);

                classNode.addChild(navClass);
                classNodes.putIfAbsent(fieldName, navClass);
            } else {
                imports.add("com.easy.query.core.annotation.Navigate");
                imports.add("com.easy.query.core.enums.RelationTypeEnum");
                ClassNodeCirculateChecker circulateChecker = classNodeCirculateChecker == null ? new ClassNodeCirculateChecker(qualifiedName,maxDeep) : classNodeCirculateChecker;

                String relationType = PsiUtil.getPsiAnnotationValue(navigatePsiAnno, "value", "");
                String selfProperty = PsiUtil.getPsiAnnotationValue(navigatePsiAnno, "selfProperty", "");
                String selfNavigateId = null;
                if (cn.hutool.core.util.StrUtil.isNotBlank(selfProperty)) {
                    selfNavigateId = selfProperty;
                }

                String targetNavigateId = null;
                String targetProperty = PsiUtil.getPsiAnnotationValue(navigatePsiAnno, "targetProperty", "");
                if (StrUtil.isNotBlank(targetProperty)) {
                    targetNavigateId = targetProperty;
                }

                String propertyType = PsiUtil.getPsiFieldPropertyType(psiField, true);
                PsiClass propClass = entityWithClass.get(propertyType);
                if (propClass != null) {

                    if (circulateChecker.pathRepeat(new ClassNodePropPath(ownerPropertyName,qualifiedName, propertyType, fieldName, deep))) {
                        continue;
                    }
                    ClassNode navClass = new ClassNode(fieldName, entityName, sort++, isPrimary, true, propClass.getName(), ownerPropertyName, qualifiedName, propClass.getQualifiedName(), beanPropType);
                    navClass.setSelfNavigateId(selfNavigateId);
                    navClass.setTargetNavigateId(targetNavigateId);
                    navClass.setPropText(psiField.getText());
                    navClass.setComment(psiFieldComment);
                    navClass.setConversion(conversion);
                    navClass.setColumnValue(columnValue);
                    navClass.setComplexPropType(complexPropType);
                    navClass.setPsiClass(psiClass);
                    navClass.setPsiField(psiField);
                    navClass.setPsiAnnoColumn(psiAnnoColumn);
                    navClass.setPsiAnnoNavigate(psiAnnoNavigate);
                    navClass.setPsiAnnoNavigateFlat(psiAnnoNavigateFlat);

                    navClass.setRelationType(relationType);
//                    String sub = StrUtil.subAfter(propertyType, ".", true);
                    classNode.addChild(navClass);
                    classNodes.putIfAbsent(fieldName, navClass);
                    addClassProps(maxDeep,project, propClass, fieldName, navClass, entityWithClass, entityProps, circulateChecker, imports, ignoreColumns, deep + 1);
                } else {
                    PsiClass propertyClass = findClass(project, propertyType);
                    if (propertyClass != null) {
                        if (circulateChecker.pathRepeat(new ClassNodePropPath(ownerPropertyName,qualifiedName, propertyType, fieldName, deep))) {
                            continue;
                        }
                        ClassNode navClass = new ClassNode(fieldName, entityName, sort++, isPrimary, true, propertyClass.getName(), ownerPropertyName, qualifiedName, propertyClass.getQualifiedName(), beanPropType);
                        navClass.setSelfNavigateId(selfProperty);
                        navClass.setTargetNavigateId(targetNavigateId);
                        navClass.setPropText(psiField.getText());
                        navClass.setComment(psiFieldComment);
                        navClass.setConversion(conversion);
                        navClass.setColumnValue(columnValue);
                        navClass.setComplexPropType(complexPropType);
                        navClass.setPsiClass(psiClass);
                        navClass.setPsiField(psiField);
                        navClass.setPsiAnnoColumn(psiAnnoColumn);
                        navClass.setPsiAnnoNavigate(psiAnnoNavigate);
                        navClass.setPsiAnnoNavigateFlat(psiAnnoNavigateFlat);

                        navClass.setRelationType(relationType);
                        classNode.addChild(navClass);
                        classNodes.putIfAbsent(fieldName, navClass);
                        addClassProps(maxDeep,project, propertyClass, fieldName, navClass, entityWithClass, entityProps, circulateChecker, imports, ignoreColumns, deep + 1);
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
