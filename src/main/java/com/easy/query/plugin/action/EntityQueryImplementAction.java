package com.easy.query.plugin.action;

import com.easy.query.plugin.core.util.KtFileUtil;
import com.easy.query.plugin.core.util.ProjectUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.PsiUtil;
import com.easy.query.plugin.core.util.VirtualFileUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.HashSet;
import java.util.Set;

public class EntityQueryImplementAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getProject();
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
        ProjectUtils.setCurrentProject(project);

        try {
            implement(project, virtualFile);
            PsiJavaFileUtil.createAptCurrentFile(virtualFile, project);
            importProxy(project, virtualFile);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void implement(Project project, VirtualFile virtualFile) {
        PsiClassOwner psiFile = (PsiClassOwner) VirtualFileUtils.getPsiFile(project, virtualFile);
        DumbService.getInstance(project).runWhenSmart(() -> {

            if (psiFile.getClasses().length > 0) {
                PsiClass psiClass = psiFile.getClasses()[0];
                PsiAnnotation entityProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityProxy");
                if (entityProxy == null) {
                    return;
                }
                boolean implementInterface = isImplementInterface(psiClass);
                if (!implementInterface) {//没有注解或者没实现
                    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);


                    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                    String entityName = psiClass.getName();

                    //获取对应的代理对象名称
                    String entityProxyName = PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "value", entityName + "Proxy");
                    PsiImportStatement importProxyAvailableStatement = getImportStatement(true, javaPsiFacade, elementFactory, "com.easy.query.core.proxy.ProxyEntityAvailable", project);


                    PsiJavaCodeReferenceElement referenceFromText = elementFactory.createReferenceFromText(String.format("ProxyEntityAvailable<%s , %s>", entityName, entityProxyName), psiClass);
                    PsiMethod method = elementFactory.createMethodFromText(String.format("public Class<%s> proxyTableClass() {return %s.class;}", entityProxyName, entityProxyName), psiClass);
                    method.getModifierList().addAnnotation("Override");
                    WriteCommandAction.runWriteCommandAction(project, () -> {

                        if (importProxyAvailableStatement != null) {
                            PsiImportList importList = ((PsiJavaFile) psiClass.getContainingFile()).getImportList();
                            if (importList != null) {
                                importList.add(importProxyAvailableStatement);
                            }
                        }
                        if (psiClass.getImplementsList() != null) {

                            psiClass.getImplementsList().add(referenceFromText);
                        }
                        psiClass.add(method);
                    });
                }
            }
        });
    }

    private void importProxy(Project project, VirtualFile virtualFile) {
        DumbService.getInstance(project).runWhenSmart(() -> {

            PsiManager psiManager = PsiManager.getInstance(project);
            PsiFile psiFile = psiManager.findFile(virtualFile);
            // 支持java和kotlin
            if (!(psiFile instanceof PsiJavaFile) && !(psiFile instanceof KtFile)) {
                return;
            }
            Set<String> importSet = new HashSet<>();
            if (psiFile instanceof KtFile) {
                KtFile ktFile = (KtFile) psiFile;
                importSet = KtFileUtil.getImportSet(ktFile);
            }
            if (psiFile instanceof PsiJavaFile) {
                PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
                importSet = PsiJavaFileUtil.getQualifiedNameImportSet(psiJavaFile);
            }
            PsiClassOwner psiClassOwnerFile = (PsiClassOwner) VirtualFileUtils.getPsiFile(project, virtualFile);
            PsiClass psiClass = psiClassOwnerFile.getClasses()[0];
            String entityName = psiClass.getName();
            PsiAnnotation entityProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityProxy");
            if(entityProxy == null){
                return;
            }
            String entityProxyName =PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "value", entityName + "Proxy");
//            return importSet.contains("com.easy.query.core.annotation.EntityProxy" );
            String qualifiedName = psiClass.getQualifiedName().substring(0, psiClass.getQualifiedName().lastIndexOf(".")) + ".proxy." + entityProxyName;
            if (!importSet.contains(qualifiedName)) {
                JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);


                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                PsiImportStatement importEntityProxyStatement = getImportStatement(true, javaPsiFacade, elementFactory, qualifiedName, project);
                WriteCommandAction.runWriteCommandAction(project, () -> {

                    if (importEntityProxyStatement != null) {
                        PsiImportList importList = ((PsiJavaFile) psiClass.getContainingFile()).getImportList();
                        if (importList != null) {
                            importList.add(importEntityProxyStatement);

                        }
                    }
                });
            }
        });
    }

    private PsiImportStatement getImportStatement(boolean invoke, JavaPsiFacade javaPsiFacade, PsiElementFactory elementFactory, String qualifiedName, Project project) {
        if (invoke) {
            PsiClass entityProxyClass = javaPsiFacade.findClass(qualifiedName, GlobalSearchScope.allScope(project));
            if (entityProxyClass != null) {
                return elementFactory.createImportStatement(entityProxyClass);
            }
        }
        return null;
    }

    private boolean isImplementInterface(PsiClass psiClass) {
        for (PsiClass anInterface : psiClass.getInterfaces()) {
            if ("com.easy.query.core.proxy.ProxyEntityAvailable".equals(anInterface.getQualifiedName())) {
                return true;
            }
        }
        return false;
    }
}
