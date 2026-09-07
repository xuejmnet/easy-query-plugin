package com.easy.query.plugin.core.util;

import com.easy.query.plugin.core.enums.BeanPropTypeEnum;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.Nullable;

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
        if (beanProp == BeanPropTypeEnum.NOT) {
            beanProp = propertyIsBeanProp(psiClass, capitalizedPropertyName, "is");
            if (beanProp == BeanPropTypeEnum.NOT) {
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
        if (setMethods.length == 0) {
            return BeanPropTypeEnum.NOT;
        }
        PsiMethod setter = setMethods[0];
        if (setter != null && setter.hasModifierProperty(PsiModifier.PUBLIC)) {
            return beanProp;
        }
        return BeanPropTypeEnum.NOT;
    }

    /**
     * 优先走 Lombok 快速路径（规避对 Lombok 类的 PSI 增强解析，见 {@link LombokBeanPropResolver}），
     * 不确定时回退到按方法名逐一查找的原实现，二者结论一致。
     */
    public static BeanPropTypeEnum hasGetterAndSetter(PsiClass psiClass, PsiField field) {
        BeanPropTypeEnum fastPath = LombokBeanPropResolver.resolveIfDeterministic(psiClass, field);
        if (fastPath != null) {
            return fastPath;
        }
        return hasGetterAndSetter(psiClass, field.getName());
    }

    private static BeanPropTypeEnum propertyIsBeanProp(PsiClass psiClass, String capitalizedPropertyName, String prefix) {
        PsiMethod[] getMethods = psiClass.findMethodsByName(prefix + capitalizedPropertyName, true);
        if (getMethods.length == 0) {
            return BeanPropTypeEnum.NOT;
        }
        PsiMethod getter = getMethods[0];
        if (getter == null || !getter.hasModifierProperty(PsiModifier.PUBLIC)) {
            return BeanPropTypeEnum.NOT;
        }
        return Objects.equals("is", prefix) ? BeanPropTypeEnum.IS : BeanPropTypeEnum.GET;
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
        if (seachAllScope) {
            if (newClass == null) {
                newClass = javaPsiFacade.findClass(fullClassName, GlobalSearchScope.allScope(project));
            }
        }
        return newClass;
    }
}
