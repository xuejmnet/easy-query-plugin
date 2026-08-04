package com.easy.query.plugin.action;

import com.easy.query.plugin.core.util.KtFileUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.ProjectUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.PsiUtil;
import com.easy.query.plugin.core.util.VirtualFileUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.PsiImportHolder;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiJavaCodeReferenceElement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassBody;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtImportDirective;
import org.jetbrains.kotlin.psi.KtImportList;
import org.jetbrains.kotlin.psi.KtNamedFunction;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.jetbrains.kotlin.psi.KtSuperTypeEntry;
import org.jetbrains.kotlin.psi.KtSuperTypeList;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.resolve.jvm.KotlinJavaPsiFacade;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class EntityQueryImplementAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getProject();
        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);

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
        if (psiFile instanceof KtFile) {
            implementKotlin(project, (KtFile) psiFile);
        } else {
            implementJava(project, psiFile);
        }
//        KtClass ktClass = Arrays.stream(psiFile.getChildren()).filter(o -> o instanceof KtClass).map(o -> (KtClass) o).findFirst().orElse(null);
//        DumbService.getInstance(project).runWhenSmart(() -> {
//
//            if (psiFile.getClasses().length > 0) {
//                PsiClass psiClass = psiFile.getClasses()[0];
//                PsiAnnotation entityProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityProxy" );
//                PsiAnnotation entityFileProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityFileProxy" );
//                if (entityProxy == null && entityFileProxy == null) {
//                    return;
//                }
//                boolean implementInterface = isImplementInterface(psiClass);
//                if (!implementInterface) {//没有注解或者没实现
//                    JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
//                    PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
//                    String entityName = psiClass.getName();
//                    //获取对应的代理对象名称
//                    String entityProxyName = PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "value", entityName + "Proxy" );
//                    PsiImportStatement importProxyAvailableStatement = getImportStatement(true, javaPsiFacade, elementFactory, "com.easy.query.core.proxy.ProxyEntityAvailable", project);
//                    PsiJavaCodeReferenceElement referenceFromText = elementFactory.createReferenceFromText(String.format("ProxyEntityAvailable<%s , %s>", entityName, entityProxyName), psiClass);
////                    PsiElement navigationElement = referenceFromText.getNavigationElement();
////                    PsiElement navigationElement = referenceFromText.getNavigationElement();
//                    PsiMethod method = elementFactory.createMethodFromText(String.format("public Class<%s> proxyTableClass() {return %s.class;}", entityProxyName, entityProxyName), psiClass);
//                    method.getModifierList().addAnnotation("Override" );
//                    WriteCommandAction.runWriteCommandAction(project, () -> {
//
//                        if (importProxyAvailableStatement != null) {
//                            PsiImportList importList = ((PsiJavaFile) psiClass.getContainingFile()).getImportList();
//                            if (importList != null) {
//                                importList.add(importProxyAvailableStatement);
//                            }
//                        }
//                        KtPsiFactory psiFactory = new KtPsiFactory(project);
//                        KtSuperTypeEntry superTypeEntry = psiFactory.createSuperTypeEntry(String.format("ProxyEntityAvailable<%s , %s>", entityName, entityProxyName));
//                        ktClass.addSuperTypeListEntry(superTypeEntry);
////                        KtSuperTypeList superTypeList = ktClass.getSuperTypeList();
////                        superTypeList.add(navigationElement);
////                        if (psiClass.getImplementsList() != null) {
////                            psiClass.getImplementsList().add(referenceFromText);
////                        }
////                        psiClass.add(method);
//                    });
//                }
//            }
//        });
    }

    private void implementJava(Project project, PsiFile psiFile) {
        PsiClassOwner psiClassOwner = (PsiClassOwner) psiFile;
        DumbService.getInstance(project).runWhenSmart(() -> {
            // 2026.2 起后台线程 PSI 读取需 read action；读取与写分离，write command 置 read action 外
            final PsiClass[] psiClassHolder = new PsiClass[1];
            final PsiImportStatement[] importStmtHolder = new PsiImportStatement[1];
            final PsiJavaCodeReferenceElement[] refHolder = new PsiJavaCodeReferenceElement[1];
            final boolean[] shouldWrite = {false};
            ReadAction.run(() -> {
                PsiClass psiClass = psiClassOwner.getClasses()[0];
                PsiAnnotation entityProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityProxy");
                PsiAnnotation entityFileProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityFileProxy");
                if (entityProxy == null && entityFileProxy == null) {
                    return;
                }
                if (isImplementInterface(psiClass)) {
                    return;
                }
                JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
                PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                String entityName = psiClass.getName();
                String entityProxyName = PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "value", entityName + "Proxy");
                psiClassHolder[0] = psiClass;
                importStmtHolder[0] = getImportStatement(true, javaPsiFacade, elementFactory, "com.easy.query.core.proxy.ProxyEntityAvailable", project);
                refHolder[0] = elementFactory.createReferenceFromText(String.format("ProxyEntityAvailable<%s , %s>", entityName, entityProxyName), psiClass);
                shouldWrite[0] = true;
            });
            if (!shouldWrite[0]) {
                return;
            }
            PsiClass psiClass = psiClassHolder[0];
            PsiImportStatement importProxyAvailableStatement = importStmtHolder[0];
            PsiJavaCodeReferenceElement referenceFromText = refHolder[0];
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
            });
        });
    }

    private void implementKotlin(Project project, KtFile ktFile) {
        KtClass ktClass = Arrays.stream(ktFile.getChildren()).filter(o -> o instanceof KtClass).map(o -> (KtClass) o).findFirst().orElse(null);
        if (ktClass != null) {

            DumbService.getInstance(project).runWhenSmart(() -> {
                // 2026.2 起后台线程 PSI 读取需 read action；读取与写分离
                final PsiImportStatement[] importStmtHolder = new PsiImportStatement[1];
                final KtSuperTypeEntry[] superTypeHolder = new KtSuperTypeEntry[1];
                final boolean[] shouldWrite = {false};
                ReadAction.run(() -> {
                    for (PsiClass psiClass : ktFile.getClasses()) {
                        PsiAnnotation entityProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityProxy");
                        PsiAnnotation entityFileProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityFileProxy");
                        if (entityProxy == null && entityFileProxy == null) {
                            break;
                        }
                        if (isImplementInterface(psiClass)) {
                            break;
                        }
                        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
                        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                        String entityName = psiClass.getName();
                        String entityProxyName = PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "value", entityName + "Proxy");
                        importStmtHolder[0] = getImportStatement(true, javaPsiFacade, elementFactory, "com.easy.query.core.proxy.ProxyEntityAvailable", project);
                        KtPsiFactory psiFactory = new KtPsiFactory(project);
                        superTypeHolder[0] = psiFactory.createSuperTypeEntry(String.format("ProxyEntityAvailable<%s , %s>", entityName, entityProxyName));
                        shouldWrite[0] = true;
                        break;
                    }
                });
                if (!shouldWrite[0]) {
                    return;
                }
                PsiImportStatement importProxyAvailableStatement = importStmtHolder[0];
                KtSuperTypeEntry referenceSuperType = superTypeHolder[0];
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    if (importProxyAvailableStatement != null) {
                        KtImportList importList = ktFile.getImportList();
                        if (importList != null) {
                            ktFile.addAfter(importProxyAvailableStatement, importList);
                        }
                    }
                    ktClass.addSuperTypeListEntry(referenceSuperType);
                });
            });
        }
    }

    private void importProxy(Project project, VirtualFile virtualFile) {
        DumbService.getInstance(project).runWhenSmart(() -> {
            // 2026.2 起后台线程 PSI 读取需 read action；读取决策与写分离
            final PsiFile[] psiFileHolder = new PsiFile[1];
            final PsiImportStatement[] importStmtHolder = new PsiImportStatement[1];
            final boolean[] needImportKotlin = {false};
            final boolean[] needImportJava = {false};
            ReadAction.run(() -> {
                PsiManager psiManager = PsiManager.getInstance(project);
                PsiFile psiFile = psiManager.findFile(virtualFile);
                // 支持java和kotlin
                if (!(psiFile instanceof PsiJavaFile) && !(psiFile instanceof KtFile)) {
                    return;
                }
                psiFileHolder[0] = psiFile;
                Set<String> importSet = new HashSet<>();
                if (psiFile instanceof KtFile) {
                    importSet = KtFileUtil.getImportSet((KtFile) psiFile);
                }
                if (psiFile instanceof PsiJavaFile) {
                    importSet = PsiJavaFileUtil.getQualifiedNameImportSet((PsiJavaFile) psiFile);
                }
                PsiClassOwner psiClassOwnerFile = (PsiClassOwner) VirtualFileUtils.getPsiFile(project, virtualFile);
                for (PsiClass psiClass : psiClassOwnerFile.getClasses()) {
                    String entityName = psiClass.getName();
                    PsiAnnotation entityProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityProxy");
                    PsiAnnotation entityFileProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityFileProxy");
                    if (entityProxy == null && entityFileProxy == null) {
                        break;
                    }
                    String entityProxyName = PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "value", entityName + "Proxy");
                    String psiClassQualifiedName = psiClass.getQualifiedName();
                    String qualifiedName = psiClassQualifiedName == null ? "" : psiClassQualifiedName.substring(0, psiClassQualifiedName.lastIndexOf(".")) + ".proxy." + entityProxyName;
                    if (!importSet.contains(qualifiedName)) {
                        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
                        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                        importStmtHolder[0] = getImportStatement(true, javaPsiFacade, elementFactory, qualifiedName, project);
                        needImportKotlin[0] = !(psiFile instanceof PsiJavaFile);
                        needImportJava[0] = psiFile instanceof PsiJavaFile;
                    }
                    break;
                }
            });
            PsiFile psiFile = psiFileHolder[0];
            PsiImportStatement importEntityProxyStatement = importStmtHolder[0];
            if (needImportKotlin[0] && psiFile instanceof KtFile) {
                importProxyKotlin(project, importEntityProxyStatement, (KtFile) psiFile);
            } else if (needImportJava[0] && psiFile instanceof PsiJavaFile) {
                importProxyJava(project, importEntityProxyStatement, (PsiJavaFile) psiFile);
            }
        });
    }

    private void importProxyJava(Project project, PsiImportStatement importEntityProxyStatement, PsiJavaFile psiFile) {

        WriteCommandAction.runWriteCommandAction(project, () -> {

            if (importEntityProxyStatement != null) {
                PsiImportList importList = psiFile.getImportList();
                if (importList != null) {
                    importList.add(importEntityProxyStatement);

                }
            }
        });
    }

    private void importProxyKotlin(Project project, PsiImportStatement importEntityProxyStatement, KtFile ktFile) {

        WriteCommandAction.runWriteCommandAction(project, () -> {

            if (importEntityProxyStatement != null) {
                KtImportList importList = ktFile.getImportList();
                if (importList != null) {
                    ktFile.addAfter(importEntityProxyStatement, importList);

                }
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
