package com.easy.query.plugin.action;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.RenderEasyQueryTemplate;
import com.easy.query.plugin.core.entity.AnonymousParseContext;
import com.easy.query.plugin.core.entity.AnonymousParseResult;
import com.easy.query.plugin.core.util.KtFileUtil;
import com.easy.query.plugin.core.util.MyModuleUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.PsiUtil;
import com.easy.query.plugin.core.util.VirtualFileUtils;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiImportStatement;
import com.intellij.psi.PsiImportStaticStatement;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiLiteralExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * create time 2024/1/29 22:18
 * 文件说明
 *
 * @author xuejiaming
 */
public class AnonymousMapAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        try {
            // 织入代码

            PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
            DataContext dataContext = e.getDataContext();
            Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
            PsiElement psiElement = CommonDataKeys.PSI_ELEMENT.getData(dataContext);
            if (psiFile != null) {

                if (psiElement instanceof PsiMethod) {
                    PsiClass containingClass = ((PsiMethod) psiElement).getContainingClass();
                    if (containingClass != null) {
                        String qualifiedName = containingClass.getQualifiedName();
                        if ("com.easy.query.api.proxy.base.MapProxy".equals(qualifiedName)) {
                            assert editor != null;
                            Document document = editor.getDocument();
                            int offset = editor.getCaretModel().getOffset();
                            PsiElement elementAt = psiFile.findElementAt(offset);
                            if (elementAt instanceof PsiIdentifier) {
                                PsiIdentifier psiIdentifier = (PsiIdentifier) elementAt;
                                PsiElement newExpression = psiIdentifier.getParent().getParent();
                                PsiElement element = psiIdentifier.getParent().getParent().getParent();
                                if (element instanceof PsiReferenceExpression) {
                                    AnonymousParseContext anonymousParseContext = new AnonymousParseContext();

                                    PsiExpressionList argumentList = ((PsiNewExpression) elementAt.getParent().getParent()).getArgumentList();
                                    if (argumentList != null) {
                                        PsiExpression[] expressions = argumentList.getExpressions();
                                        boolean b = parseMapProxyConstructor(e.getProject(), expressions, anonymousParseContext);
                                        if (!b) {
                                            return;
                                        }
                                        parseChainMapProxy(element, anonymousParseContext);
                                        lambdaBodyContent(element, anonymousParseContext);
                                        //element.getParent().getParent().getParent().getParent().getParent().getTextOffset()
//                                            System.out.println(document.getCharsSequence().subSequence(anonymousParseContext.getStart(),anonymousParseContext.getEnd()));
                                        //生成匿名对象
                                        if (!anonymousParseContext.getAnonymousParseResultMap().isEmpty()) {
                                            anonymousParseContext.setProject(e.getProject());
//                                                PackageUtil.selectPackage(e.getProject(),)
                                            RenderEasyQueryTemplate.renderAnonymousType(anonymousParseContext);

                                            PsiClass newPsiClass = JavaPsiFacade.getInstance(e.getProject()).findClass(anonymousParseContext.getModelPackage() + ".proxy." + anonymousParseContext.getAnonymousName() + "Proxy", GlobalSearchScope.projectScope(e.getProject()));
                                            if (newPsiClass == null) {
                                                return;
                                            }

                                            WriteCommandAction.runWriteCommandAction(e.getProject(), () -> {
                                                if (psiFile instanceof PsiJavaFile) {
                                                    PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
                                                    javaImport(e.getProject(), psiJavaFile, newPsiClass, anonymousParseContext);
                                                }
//                                                    else if (psiFile instanceof KtFile) {
//                                                        KtFile ktFile = (KtFile) file;
//                                                        ktImport(ktFile, psiClass, entry.getKey(), document);
//                                                    }
                                            });
                                            String anonymousLambdaTemplate = RenderEasyQueryTemplate.getAnonymousLambdaTemplate(anonymousParseContext);
                                            WriteCommandAction.runWriteCommandAction(e.getProject(), () -> {
                                                document.replaceString(anonymousParseContext.getStart(),
                                                        anonymousParseContext.getEnd(), new StringBuffer(anonymousLambdaTemplate));
                                            });

                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Messages.showErrorDialog(e.getProject(), "请按规定：将光标移动到MapProxy或者对应的Class上:" + ex.getMessage(), "错误提示");
        }
//        Project project = e.getProject();
////        VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
//        ProjectUtils.setCurrentProject(project);
//        PsiJavaFileUtil.createAptFile(project);
    }

    private boolean parseMapProxyConstructor(Project project, PsiExpression[] expressions, AnonymousParseContext anonymousParseContext) {
        if (expressions.length < 2) {
            Messages.showErrorDialog(project, "错误的MapProxy构造函数", "错误提示");
            return false;
        }
        String anonymousName = trimClearStringValue(expressions[0].getText());
        anonymousParseContext.setAnonymousName(anonymousName);
        PsiType packageClass = expressions[1].getType();
        if (packageClass == null) {
            return false;
        }
        if("java.lang.String".equals(packageClass.getCanonicalText())){
            if(expressions.length<3){
                Messages.showErrorDialog(project, "错误的MapProxy构造函数", "错误提示");
                return false;
            }
            String moduleName = trimClearStringValue(expressions[1].getText());
            String packageName = trimClearStringValue(expressions[2].getText());
            if(StrUtil.isBlank(moduleName) || StrUtil.isBlank(packageName)){
                Messages.showErrorDialog(project, "错误的MapProxy构造函数moduleName:["+moduleName+"],packageName:["+packageName+"]", "错误提示");
                return false;
            }
            anonymousParseContext.setModelPackage(packageName);

            Module moduleForFile = MyModuleUtil.getModule(project,moduleName);
            if (moduleForFile == null) {
                Messages.showErrorDialog(project, "无法找到对应模块:" + moduleName, "错误提示");
                return false;
            }
            anonymousParseContext.setModuleName(moduleForFile.getName());
            if (expressions.length == 4) {
                String text = expressions[3].getText();
                boolean parameterTrue = StrUtil.equals(text, "true", true);
                anonymousParseContext.setEntityProxy(!parameterTrue);
                anonymousParseContext.setEntityFileProxy(parameterTrue);
            } else {
                anonymousParseContext.setEntityProxy(true);
                anonymousParseContext.setEntityFileProxy(false);
            }

        }else{
            String genericType = packageClass.getCanonicalText();
            String packageWithClassName = PsiUtil.parseGenericType(genericType);
            if (StrUtil.endWith(packageWithClassName, anonymousName)) {
                Messages.showErrorDialog(project, "请勿使用相同类名:" + packageWithClassName, "错误提示");
                return false;
            }
            anonymousParseContext.setModelPackage(StrUtil.subBefore(packageWithClassName, ".", true));
            PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(packageWithClassName, GlobalSearchScope.allScope(project));
            if (psiClass == null) {
                return false;
            }
            PsiFile containingFile = psiClass.getContainingFile();
            Module moduleForFile = ModuleUtil.findModuleForFile(containingFile);
            if (moduleForFile == null) {
                Messages.showErrorDialog(project, "无法找到对应模块:" + packageWithClassName, "错误提示");
                return false;
            }
            anonymousParseContext.setModuleName(moduleForFile.getName());
            if (expressions.length == 3) {
                String text = expressions[2].getText();
                boolean parameterTrue = StrUtil.equals(text, "true", true);
                anonymousParseContext.setEntityProxy(!parameterTrue);
                anonymousParseContext.setEntityFileProxy(parameterTrue);
            } else {
                anonymousParseContext.setEntityProxy(true);
                anonymousParseContext.setEntityFileProxy(false);
            }
        }
//                                                ModuleManager.getInstance(e.getProject()).

        return true;
    }

    private String trimClearStringValue(String val){
        if(val==null){
            return null;
        }
        String value = val;
        if (val.startsWith("\"") && val.endsWith("\"")) {
            value = value.substring(1, value.length() - 1); // 移除首尾引号
        }
        return value;
    }

    public void javaImport(Project project, PsiJavaFile psiJavaFile, PsiClass psiClass, AnonymousParseContext anonymousParseContext) {
        Set<String> importSet = PsiJavaFileUtil.getImportSet(psiJavaFile);
        PsiImportStatement importStatement = PsiElementFactory.getInstance(project).createImportStatement(psiClass);
        // 如果已经导入了，就不再导入
        if (importSet.contains(importStatement.getText())) {
            return;
        }
        anonymousParseContext.setStart(anonymousParseContext.getStart() + importStatement.getTextLength());
        anonymousParseContext.setEnd(anonymousParseContext.getEnd() + importStatement.getTextLength());
        psiJavaFile.getImportList().add(importStatement);
    }
//    public void ktImport(Project project,KtFile ktFile, PsiClass psiClass, Document document) {
//        Set<String> importSet = KtFileUtil.getImportSet(ktFile);
//        String importText = Objects.requireNonNull(psiClass.getQualifiedName());
//        PsiImportStatement importStatementOnDemand = PsiElementFactory.getInstance(project).createImportStatementOnDemand(importText);
//        // 如果已经导入了，就不再导入
//        if (importSet.contains(importStatementOnDemand.getText())) {
//            return;
//        }
//        ktFile.getImportList().add(importStatementOnDemand);
//        // 为什么要这样写，因为kotlin 不支持静态导入，要么就是.*导入，但是.*又会导致上面代码提示重复，所以只能这样写
//        String text = ktFile.getText().replace("import " + importText + ".*", "\nimport " + importText + "." + finalImport);
//        document.setText(text);
//    }

    private void lambdaBodyContent(PsiElement element, AnonymousParseContext anonymousParseContext) {
        if (element.getParent() instanceof PsiLambdaExpression) {
            anonymousParseContext.setStart(element.getTextOffset());
            anonymousParseContext.setEnd(anonymousParseContext.getStart() + element.getTextLength());
        } else {
            lambdaBodyContent(element.getParent(), anonymousParseContext);
        }

    }

    private void parseChainMapProxy(PsiElement psiElement, AnonymousParseContext anonymousParseContext) {
        if (psiElement.getLastChild() instanceof PsiIdentifier) {
            PsiIdentifier lastChild = (PsiIdentifier) psiElement.getLastChild();
            if (lastChild.getText().equals("put")) {
                PsiElement element = psiElement.getParent();
                parsePsiMethodCallExpression(element, anonymousParseContext);
                parseChainMapProxy(element.getParent(), anonymousParseContext);
            }
        }

    }

    private void parsePsiMethodCallExpression(PsiElement psiElement, AnonymousParseContext anonymousParseContext) {
        PsiElement firstChild = psiElement.getFirstChild();
        PsiElement lastChild = psiElement.getLastChild();

        if (lastChild instanceof PsiExpressionList) {
            PsiExpressionList psiExpressionList = (PsiExpressionList) lastChild;
            PsiElement putParameterL = psiExpressionList.getFirstChild();
            PsiElement putParameterR = psiExpressionList.getLastChild();

            if ("(".equals(putParameterL.getText()) && ")".equals(putParameterR.getText())) {
                PsiElement nextSiblingL = putParameterL.getNextSibling();
                PsiElement prevSiblingR = putParameterR.getPrevSibling();
                if (nextSiblingL instanceof PsiLiteralExpression && prevSiblingR instanceof PsiMethodCallExpression) {
                    PsiLiteralExpression psiLiteralExpressionL = (PsiLiteralExpression) nextSiblingL;
                    PsiMethodCallExpression psiMethodCallExpressionR = (PsiMethodCallExpression) prevSiblingR;
                    String key = psiLiteralExpressionL.getText();
                    if (key.startsWith("\"") && key.endsWith("\"")) {
                        key = key.substring(1, key.length() - 1); // 移除首尾引号
                    }
//                    PsiExpressionList argumentList = psiMethodCallExpressionR.getArgumentList();
//                    PsiExpression qualifierExpression = psiMethodCallExpressionR.getMethodExpression().getQualifierExpression();
                    PsiElement owner = psiMethodCallExpressionR.getMethodExpression().getFirstChild();
                    PsiType type = ((PsiExpression) owner).getType();
                    if (type instanceof PsiClassType) {
                        PsiClass ownerClass = ((PsiClassType) type).resolve();
                        if (ownerClass != null) {

                            PsiElement psiProp = psiMethodCallExpressionR.getMethodExpression().getLastChild();
                            if (psiProp instanceof PsiIdentifier) {
//                                String prop = psiProp.getText();

                                PsiType returnType1 = ((PsiMethod) psiMethodCallExpressionR.getMethodExpression().resolve()).getReturnType();
                                Map<PsiTypeParameter, PsiType> substitutionMap1 = ((PsiClassReferenceType) returnType1).resolveGenerics().getSubstitutor().getSubstitutionMap();
                                if (substitutionMap1.size() == 2) {
                                    Collection<PsiType> values = substitutionMap1.values();
                                    for (PsiType value : values) {
                                        if (!Objects.equals(value.getInternalCanonicalText(), ownerClass.getQualifiedName())) {
                                            String internalCanonicalText = value.getInternalCanonicalText();
                                            anonymousParseContext.getAnonymousParseResultMap().put(key, new AnonymousParseResult(key, internalCanonicalText, psiMethodCallExpressionR.getText()));
                                            break;
                                        }
                                    }
                                }
//                                PsiMethod psiMethod = Arrays.stream(ownerClass.getAllMethods())
//                                        .filter(o -> {
//                                            return Objects.equals(o.getName(), prop);
//                                        }).findFirst().orElse(null);
//                                if(psiMethod!=null){
//                                    PsiClassType returnType = (PsiClassType) psiMethod.getReturnType();
//                                    if(returnType!=null){
//                                        PsiClass resolve = returnType.resolve();
//                                        if(resolve!=null){
//                                            String name = resolve.getName();
//                                            System.out.println("返回结果");
//                                        }
//                                    }
//                                    //
//                                }
//                                PsiField field = Arrays.stream(ownerClass.getAllFields()).filter(o -> {
//                                    return Objects.equals(o.getName(), prop);
//                                }).findFirst().orElse(null);
//                                System.out.println(field);
                            }
                        }
//                        PsiType referencedClass = getReferencedClass((PsiReferenceExpression) owner);
                    }
                }
            }
        }

    }
}
