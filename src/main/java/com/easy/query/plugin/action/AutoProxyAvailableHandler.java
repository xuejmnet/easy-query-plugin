//package com.easy.query.plugin.action;
//
//import com.easy.query.plugin.core.util.KtFileUtil;
//import com.easy.query.plugin.core.util.PsiJavaFileUtil;
//import com.easy.query.plugin.core.util.PsiUtil;
//import com.intellij.codeInsight.CodeInsightActionHandler;
//import com.intellij.codeInsight.generation.OverrideImplementUtil;
//import com.intellij.openapi.command.WriteCommandAction;
//import com.intellij.openapi.command.undo.UndoUtil;
//import com.intellij.openapi.editor.Editor;
//import com.intellij.openapi.project.Project;
//import com.intellij.psi.JavaPsiFacade;
//import com.intellij.psi.PsiAnnotation;
//import com.intellij.psi.PsiClass;
//import com.intellij.psi.PsiElementFactory;
//import com.intellij.psi.PsiFile;
//import com.intellij.psi.PsiImportList;
//import com.intellij.psi.PsiImportStatement;
//import com.intellij.psi.PsiJavaCodeReferenceElement;
//import com.intellij.psi.PsiJavaFile;
//import com.intellij.psi.PsiMethod;
//import com.intellij.psi.search.GlobalSearchScope;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.kotlin.psi.KtFile;
//
//import java.util.HashSet;
//import java.util.Set;
//
///**
// * create time 2024/5/15 11:11
// * 文件说明
// *
// * @author xuejiaming
// */
//public class AutoProxyAvailableHandler implements CodeInsightActionHandler {
//    @Override
//    public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {
//
//        if (psiFile.isWritable()) {
//            PsiClass psiClass = OverrideImplementUtil.getContextClass(project, editor, psiFile, false);
//            if (null != psiClass) {
//                processClass(project, psiClass);
//
//                UndoUtil.markPsiFileForUndo(psiFile);
//            }
//        }
//    }
//
//    protected void processClass(@NotNull Project project, @NotNull PsiClass psiClass) {
//
//        PsiAnnotation entityProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityProxy");
//        PsiAnnotation entityFileProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityFileProxy");
//        if (entityProxy == null && entityFileProxy == null) {
//            return;
//        }
//        boolean implementInterface = isImplementInterface(psiClass);
//        if (!implementInterface) {//没有注解或者没实现
//
//            PsiFile psiFile = psiClass.getContainingFile();
//            Set<String> importSet = new HashSet<>();
//            if (psiFile instanceof KtFile) {
//                KtFile ktFile = (KtFile) psiFile;
//                importSet = KtFileUtil.getImportSet(ktFile);
//            }
//            if (psiFile instanceof PsiJavaFile) {
//                PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
//                importSet = PsiJavaFileUtil.getQualifiedNameImportSet(psiJavaFile);
//            }
//            String entityName = psiClass.getName();
//            String entityProxyName = PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "value", entityName + "Proxy");
////            return importSet.contains("com.easy.query.core.annotation.EntityProxy" );
//            String qualifiedName = psiClass.getQualifiedName().substring(0, psiClass.getQualifiedName().lastIndexOf(".")) + ".proxy." + entityProxyName;
//
//            JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
//
//
//            PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
//            PsiImportStatement importProxyAvailableStatement = getImportStatement(true, javaPsiFacade, elementFactory, "com.easy.query.core.proxy.ProxyEntityAvailable", project);
//            PsiJavaCodeReferenceElement referenceFromText = elementFactory.createReferenceFromText(String.format("ProxyEntityAvailable<%s , %s>", entityName, entityProxyName), psiClass);
//            PsiMethod method = elementFactory.createMethodFromText(String.format("public Class<%s> proxyTableClass() {return %s.class;}", entityProxyName, entityProxyName), psiClass);
//            method.getModifierList().addAnnotation("Override");
//            if (psiFile instanceof PsiJavaFile) {
//                if (importProxyAvailableStatement != null) {
//
//                    PsiImportList importList = ((PsiJavaFile) psiClass.getContainingFile()).getImportList();
//                    if (importList != null) {
//                        importList.add(importProxyAvailableStatement);
//                    }
//                }
////                        KtPsiFactory psiFactory = new KtPsiFactory(project);
////                        KtSuperTypeEntry superTypeEntry = psiFactory.createSuperTypeEntry(String.format("ProxyEntityAvailable<%s , %s>", entityName, entityProxyName));
////                        KtSuperTypeList superTypeList = ktClass.getSuperTypeList();
////                        superTypeList.add(navigationElement);
//                if (psiClass.getImplementsList() != null) {
//                    psiClass.getImplementsList().add(referenceFromText);
//                }
//                psiClass.add(method);
////                WriteCommandAction.runWriteCommandAction(project, () -> {
////
////                    if (importProxyAvailableStatement != null) {
////
////                        PsiImportList importList = ((PsiJavaFile) psiClass.getContainingFile()).getImportList();
////                        if (importList != null) {
////                            importList.add(importProxyAvailableStatement);
////                        }
////                    }
//////                        KtPsiFactory psiFactory = new KtPsiFactory(project);
//////                        KtSuperTypeEntry superTypeEntry = psiFactory.createSuperTypeEntry(String.format("ProxyEntityAvailable<%s , %s>", entityName, entityProxyName));
//////                        KtSuperTypeList superTypeList = ktClass.getSuperTypeList();
//////                        superTypeList.add(navigationElement);
////                    if (psiClass.getImplementsList() != null) {
////                        psiClass.getImplementsList().add(referenceFromText);
////                    }
////                    psiClass.add(method);
//////                    if (importProxyAvailableStatement != null) {
//////                        PsiImportList importList = ((PsiJavaFile)containingFile).getImportList();
//////                        if (importList != null) {
//////                            importList.add(importProxyAvailableStatement);
//////                        }
//////                    }
////                });
//            }
//        }
//    }
//
//    private PsiImportStatement getImportStatement(boolean invoke, JavaPsiFacade javaPsiFacade, PsiElementFactory elementFactory, String qualifiedName, Project project) {
//        if (invoke) {
//            PsiClass entityProxyClass = javaPsiFacade.findClass(qualifiedName, GlobalSearchScope.allScope(project));
//            if (entityProxyClass != null) {
//                return elementFactory.createImportStatement(entityProxyClass);
//            }
//        }
//        return null;
//    }
//
//    private boolean isImplementInterface(PsiClass psiClass) {
//        for (PsiClass anInterface : psiClass.getInterfaces()) {
//            if ("com.easy.query.core.proxy.ProxyEntityAvailable".equals(anInterface.getQualifiedName())) {
//                return true;
//            }
//        }
//        return false;
//    }
//}
