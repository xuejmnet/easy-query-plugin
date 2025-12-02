package com.easy.query.plugin.action;

import com.easy.query.plugin.core.util.PsiUtil;
import com.easy.query.plugin.core.util.VirtualFileUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.rename.RenameHandler;
import com.intellij.refactoring.rename.RenameHandlerRegistry;
import com.intellij.refactoring.rename.RenameProcessor;
import org.jetbrains.annotations.NotNull;

public class EasyQueryRenameAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
        DataContext dataContext = e.getDataContext();

        if (project == null || editor == null || file == null || element == null) {
            return;
        }

        // 暂时只处理实体类字段的 rename
        if (!(element instanceof PsiField)) {
            return;
        }

        PsiField entityField = (PsiField) element;
        // 获取 IDEA 自带的重命名处理器
        RenameHandler handler = RenameHandlerRegistry.getInstance().getRenameHandler(dataContext);
        if (handler == null) {
            return;
        }


        String oldName = entityField.getName();
        // 从用户输入获取新名字（弹原生 rename 对话框）
        // 这一步会阻塞，用户输入 newName 后才继续执行
        String newName = getUserInputNewName(project, oldName);
        if (newName == null) {
            return;
        }

        if (newName.equalsIgnoreCase(oldName)) {
            return;
        }

        // 找到 Proxy 类
        PsiClass proxyClass = findProxyClass(entityField);
        if (proxyClass == null) {
            // 只有实体字段，正常 rename
            new RenameProcessor(project, entityField, newName, false, false).run();
            return;
        }

        // 找到 proxy 方法
        PsiMethod proxyMethod = findProxyMethod(proxyClass, oldName);
        if (proxyMethod == null) {
            // 没有 proxy方法，正常 rename 实体字段
            new RenameProcessor(project, entityField, newName, false, false).run();
            return;
        }

        // 找 Fields 静态类字段
        PsiField proxyConstField = findFieldsConst(proxyClass, oldName);

        // 创建一个批量 rename（Proxy + 实体字段一起重构）
        RenameProcessor processor = new RenameProcessor(project, proxyMethod, newName, false, false);
        if (proxyConstField != null) {
            processor.addElement(proxyConstField, newName);
        }

        // 由于要先修改Proxy类,但是Proxy类又没用字段做为主元素,因此只能手动接管setter/getter
        // 这里使用lombok的处理getter/setter方式来找对应方法
        // 添加 setter 方法重命名
        PsiMethod setterMethod = findSetterMethod(entityField);
        if (setterMethod != null) {
            String newSetterName = getLombokSetterName(entityField, newName);
            processor.addElement(setterMethod, newSetterName);
        }
        // 添加 getter 方法重命名
        PsiMethod getterMethod = findGetterMethod(entityField);
        if (getterMethod != null) {
            String newGetterName = getLombokGetterName(entityField, newName);
            processor.addElement(getterMethod, newGetterName);
        }


        processor.addElement(entityField, newName);
        processor.run();

    }


    /**
     * 用户输入新名字（完整弹原生对话框）
     */
    private String getUserInputNewName(Project project, String oldName) {
        return com.intellij.openapi.ui.Messages.showInputDialog(
                project,
                "Enter new name",
                "Rename",
                null,
                oldName,
                null
        );
    }


    private PsiClass findProxyClass(PsiField field) {

        PsiClass entityClass = field.getContainingClass();
        if (entityClass == null) return null;

        Project project = entityClass.getProject();


        // 2. 获取 packageName（generatePackage）
        PsiFile containingFile = entityClass.getContainingFile();
        if (!(containingFile instanceof PsiClassOwner)) {
            return null;
        }
        PsiClassOwner psiFile = (PsiClassOwner) containingFile;

        // Copy from APTVersion2_9.java !!! 老版本暂时未处理
        // 1: 检查是否有注解
        PsiAnnotation entityProxy = entityClass.getAnnotation("com.easy.query.core.annotation.EntityProxy");
        if (entityProxy == null) {
            return null;
        }
        // 2: 获取 Proxy 类名
        String proxyClassName = PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "value", entityClass.getName() + "Proxy");
        if (proxyClassName == null) {
            return null;
        }
        String oldPackage = psiFile.getPackageName() + ".proxy";
        String packageName = PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "generatePackage", oldPackage);
        // 3. 在 Project 内查找 Proxy 类
        GlobalSearchScope scope = GlobalSearchScope.projectScope(project);
        JavaPsiFacade facade = JavaPsiFacade.getInstance(project);

        return facade.findClass(packageName + "." + proxyClassName, scope);
    }


    private PsiMethod findProxyMethod(PsiClass proxyClass, String oldName) {
        // 字段名对应的方法，例如 user -> user()
        String methodName = oldName;
        PsiMethod[] methods = proxyClass.getMethods();
        for (PsiMethod m : methods) {
            if (methodName.equals(m.getName()) && m.getParameterList().getParametersCount() == 0) {
                return m;
            }
        }

        return null;
    }


    private PsiField findFieldsConst(PsiClass proxyClass, String oldName) {
        PsiClass fields = proxyClass.findInnerClassByName("Fields", false);
        if (fields == null) {
            return null;
        }
        return fields.findFieldByName(oldName, false);
    }

    private PsiMethod findSetterMethod(PsiField field) {
        PsiClass entityClass = field.getContainingClass();
        if (entityClass == null) return null;

        String name = field.getName();
        String setterName;

        // boolean 类型字段以 is 开头特殊处理
        if (field.getType().getCanonicalText().equalsIgnoreCase("boolean")
                && name.startsWith("is") && name.length() > 2
                && Character.isUpperCase(name.charAt(2))) {
            // 例如 isActive -> setActive
            setterName = "set" + name.substring(2);
        } else {
            setterName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        for (PsiMethod m : entityClass.getMethods()) {
            if (setterName.equals(m.getName()) && m.getParameterList().getParametersCount() == 1) {
                return m;
            }
        }
        return null;
    }


    private PsiMethod findGetterMethod(PsiField field) {
        PsiClass entityClass = field.getContainingClass();
        if (entityClass == null) return null;

        String name = field.getName();
        String getterName;

        if (field.getType().getCanonicalText().equals("boolean")) {
            // boolean 类型字段
            if (name.startsWith("is") && name.length() > 2 && Character.isUpperCase(name.charAt(2))) {
                // isActive -> isActive()
                getterName = name;
            } else {
                getterName = "is" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
            }
        } else {
            // 非 boolean
            getterName = "get" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }

        for (PsiMethod m : entityClass.getMethods()) {
            if (getterName.equals(m.getName()) && m.getParameterList().getParametersCount() == 0) {
                return m;
            }
        }
        return null;
    }


    /**
     * 根据 Lombok 规则生成 setter 方法名
     */
    private String getLombokSetterName(PsiField field, String newName) {
        if (field.getType().getCanonicalText().equals("boolean") &&
                newName.startsWith("is") && newName.length() > 2 &&
                Character.isUpperCase(newName.charAt(2))) {
            return "set" + newName.substring(2);
        }
        return "set" + Character.toUpperCase(newName.charAt(0)) + newName.substring(1);
    }

    /**
     * 根据 Lombok 规则生成 getter 方法名
     */
    private String getLombokGetterName(PsiField field, String newName) {
        if (field.getType().getCanonicalText().equals("boolean")) {
            if (newName.startsWith("is") && newName.length() > 2 && Character.isUpperCase(newName.charAt(2))) {
                return newName; // boolean isXxx -> getter: isXxx
            } else {
                return "is" + Character.toUpperCase(newName.charAt(0)) + newName.substring(1);
            }
        }
        return "get" + Character.toUpperCase(newName.charAt(0)) + newName.substring(1);
    }

}
