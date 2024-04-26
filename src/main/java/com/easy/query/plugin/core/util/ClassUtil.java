package com.easy.query.plugin.core.util;

import com.easy.query.plugin.core.enums.BeanPropTypeEnum;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nullable;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Objects;

/**
 * create time 2023/9/16 12:46
 * 文件说明
 *
 * @author xuejiaming
 */
public class ClassUtil {


    public static BeanPropTypeEnum hasGetterAndSetter(PsiClass psiClass, String propertyName) {
        String capitalizedPropertyName = capitalize(propertyName);

        // 检查是否有公共的 getter 方法
        BeanPropTypeEnum beanProp = propertyIsBeanProp(psiClass, capitalizedPropertyName, "get");
        if(beanProp==BeanPropTypeEnum.NOT){
            beanProp = propertyIsBeanProp(psiClass, capitalizedPropertyName, "is");
            if(beanProp==BeanPropTypeEnum.NOT){
                return beanProp;
            }
        }
//        PsiMethod[] getMethods = psiClass.findMethodsByName("get" + capitalizedPropertyName, true);
//        if(getMethods.length==0){
//            return false;
//        }
//        PsiMethod getter = getMethods[0];
//        if (getter == null || !getter.hasModifierProperty(PsiModifier.PUBLIC)) {
//            return false;
//        }

        // 检查是否有公共的 setter 方法
        PsiMethod[] setMethods = psiClass.findMethodsByName("set" + capitalizedPropertyName, true);
        if(setMethods.length==0){
            return BeanPropTypeEnum.NOT;
        }
        PsiMethod setter = setMethods[0];
        if(setter != null && setter.hasModifierProperty(PsiModifier.PUBLIC)){
            return beanProp;
        }
        return BeanPropTypeEnum.NOT;
    }

    private static BeanPropTypeEnum propertyIsBeanProp(PsiClass psiClass,String capitalizedPropertyName,String prefix){
        PsiMethod[] getMethods = psiClass.findMethodsByName(prefix + capitalizedPropertyName, true);
        if(getMethods.length==0){
            return BeanPropTypeEnum.NOT;
        }
        PsiMethod getter = getMethods[0];
        if (getter == null || !getter.hasModifierProperty(PsiModifier.PUBLIC)) {
            return BeanPropTypeEnum.NOT;
        }
        return Objects.equals("is",prefix)?BeanPropTypeEnum.IS:BeanPropTypeEnum.GET;
    }

    private static String capitalize(String s) {
        if (s.length() == 0) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
    public static @Nullable PsiClass findClass(Project project, String fullClassName, boolean seachAllScope) {
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
        PsiClass newClass = javaPsiFacade.findClass(fullClassName, GlobalSearchScope.projectScope(project));
        if(seachAllScope){
            if (newClass == null) {
                newClass = javaPsiFacade.findClass(fullClassName, GlobalSearchScope.allScope(project));
            }
        }
        return newClass;
    }
}
