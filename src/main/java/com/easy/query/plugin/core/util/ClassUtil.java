package com.easy.query.plugin.core.util;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;

/**
 * create time 2023/9/16 12:46
 * 文件说明
 *
 * @author xuejiaming
 */
public class ClassUtil {


    public static boolean hasGetterAndSetter(PsiClass psiClass, String propertyName) {
        String capitalizedPropertyName = capitalize(propertyName);

        // 检查是否有公共的 getter 方法
        PsiMethod[] getMethods = psiClass.findMethodsByName("get" + capitalizedPropertyName, false);
        if(getMethods.length==0){
            return false;
        }
        PsiMethod getter = getMethods[0];
        if (getter == null || !getter.hasModifierProperty(PsiModifier.PUBLIC)) {
            return false;
        }

        // 检查是否有公共的 setter 方法
        PsiMethod[] setMethods = psiClass.findMethodsByName("set" + capitalizedPropertyName, false);
        if(setMethods.length==0){
            return false;
        }
        PsiMethod setter = setMethods[0];
        return setter != null && setter.hasModifierProperty(PsiModifier.PUBLIC);
    }

    private static String capitalize(String s) {
        if (s.length() == 0) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
