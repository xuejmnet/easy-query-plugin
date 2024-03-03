//package com.easy.query.plugin.action;
//
//import com.easy.query.plugin.core.entity.AptPropertyInfo;
//import com.easy.query.plugin.core.entity.AptSelectPropertyInfo;
//import com.easy.query.plugin.core.entity.AptValueObjectInfo;
//import com.easy.query.plugin.core.entity.PropertyColumn;
//import com.easy.query.plugin.core.util.ClassUtil;
//import com.easy.query.plugin.core.util.PsiUtil;
//import com.intellij.openapi.actionSystem.AnAction;
//import com.intellij.openapi.actionSystem.AnActionEvent;
//import com.intellij.openapi.actionSystem.CommonDataKeys;
//import com.intellij.openapi.project.Project;
//import com.intellij.psi.JavaPsiFacade;
//import com.intellij.psi.PsiAnnotation;
//import com.intellij.psi.PsiClass;
//import com.intellij.psi.PsiClassType;
//import com.intellij.psi.PsiElement;
//import com.intellij.psi.PsiField;
//import com.intellij.psi.PsiType;
//import com.intellij.psi.search.GlobalSearchScope;
//
//import java.util.HashSet;
//import java.util.Objects;
//import java.util.Set;
//
///**
// * create time 2024/2/29 16:44
// * 文件说明
// *
// * @author xuejiaming
// */
//public class CreateStructDTOAction extends AnAction {
//
//    @Override
//    public void actionPerformed(AnActionEvent e) {
//        // TODO: insert action logic here
//        Project project = e.getProject();
//        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);
//        String fullClassName = "org.example.entity.CityEntity";
//        PsiClass psiClass = ClassUtil.findClass(project,fullClassName,true);
//        String entityName = psiClass.getName();
//        String entityFullName = psiClass.getQualifiedName();
//        //是否是数据库对象
//        PsiAnnotation entityTable = psiClass.getAnnotation("com.easy.query.core.annotation.Table");
//        //获取对应的忽略属性
//        Set<String> tableIgnoreProperties = PsiUtil.getPsiAnnotationValues(entityTable, "ignoreProperties", new HashSet<>());
//
//        PsiField[] fields = psiClass.getAllFields();
//        for (PsiField field : fields) {
//            PsiAnnotation columnIgnore = field.getAnnotation("com.easy.query.core.annotation.ColumnIgnore");
//            if (columnIgnore != null) {
//                continue;
//            }
//            String name = field.getName();
//            //是否存在忽略属性
//            if (!tableIgnoreProperties.isEmpty() && tableIgnoreProperties.contains(name)) {
//                continue;
//            }
//            boolean isBeanProperty = ClassUtil.hasGetterAndSetter(psiClass, name);
//            if (!isBeanProperty) {
//                continue;
//            }
//            PsiAnnotation navigate = field.getAnnotation("com.easy.query.core.annotation.Navigate");
//            String psiFieldPropertyType = PsiUtil.getPsiFieldPropertyType(field, navigate != null);
//            String psiFieldComment = PsiUtil.getPsiFieldClearComment(field);
//            PsiAnnotation valueObject = field.getAnnotation("com.easy.query.core.annotation.ValueObject");
//            boolean isValueObject = valueObject != null;
//            String fieldName = isValueObject ? psiFieldPropertyType.substring(psiFieldPropertyType.lastIndexOf(".") + 1) : entityName;
//
//            PsiAnnotation proxyProperty = field.getAnnotation("com.easy.query.core.annotation.ProxyProperty");
//            String proxyPropertyName = PsiUtil.getPsiAnnotationValue(proxyProperty, "value", null);
//
//            PropertyColumn propertyColumn = getPropertyColumn(psiFieldPropertyType);
//
//            boolean includeProperty = navigate != null;
//            boolean includeManyProperty = false;
//            if (!includeProperty) {
//                aptFileCompiler.getSelectorInfo().addProperties(new AptSelectPropertyInfo(name, psiFieldComment, proxyPropertyName));
//            } else {
//                aptFileCompiler.addImports("com.easy.query.core.proxy.columns.SQLNavigateColumn");
//                String propertyType = propertyColumn.getPropertyType();
//
//                String propIsProxy = PsiUtil.getPsiAnnotationValue(navigate, "propIsProxy", "true");
//                String navigatePropertyProxyFullName = getNavigatePropertyProxyFullName(project,propertyType,!Objects.equals("false",propIsProxy));
//                if (navigatePropertyProxyFullName != null) {
//                    propertyColumn.setNavigateProxyName(navigatePropertyProxyFullName);
//                }else{
//                    psiFieldComment+="\n//插件提示无法获取导航属性代理:"+propertyType;
//                }
//                String psiAnnotationValue = PsiUtil.getPsiAnnotationValue(navigate, "value", "");
//                if (psiAnnotationValue.endsWith("ToMany")) {
//                    includeManyProperty = true;
//                    aptFileCompiler.addImports("com.easy.query.core.proxy.columns.SQLQueryable");
//                }
//            }
//            aptValueObjectInfo.addProperties(new AptPropertyInfo(name, propertyColumn, psiFieldComment, fieldName, isValueObject, entityName, includeProperty,includeManyProperty, proxyPropertyName));
//            aptFileCompiler.addImports(propertyColumn.getImport());
//
//
//        }
//        System.out.println(e);
//    }
//
//
//    private PsiClass findClass(Project project, String fullClassName) {
//        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
//        PsiClass newClass = javaPsiFacade.findClass(fullClassName, GlobalSearchScope.projectScope(project));
//        if (newClass == null) {
//            newClass = javaPsiFacade.findClass(fullClassName, GlobalSearchScope.allScope(project));
//        }
//        return newClass;
//    }
//}
