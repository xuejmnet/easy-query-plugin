package com.easy.query.plugin.action;

import com.easy.query.plugin.core.util.MyStringUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiImportList;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiReferenceExpression;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * create time 2024/6/10 13:41
 * 文件说明
 *
 * @author xuejiaming
 */
public class NavigatePathAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getProject();

        try {


            PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
            if (psiFile != null) {
                if (psiFile instanceof KtFile) {
                    return;
                }
                PsiClassOwner psiClassOwner = (PsiClassOwner) psiFile;
                DataContext dataContext = e.getDataContext();
                Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
                if (editor != null) {
                    int offset = editor.getCaretModel().getOffset();
                    PsiElement elementAt = psiFile.findElementAt(offset);
                    if (elementAt == null) {
                        return;
                    }
                    //如果是字段的话
                    if (ELEMENT_FIELD.accepts(elementAt)) {
                        PsiIdentifier field = (PsiIdentifier) elementAt;
                        String fieldName = MyStringUtil.toUpperUnderlined(field.getText());

//                        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(project);
                        PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(project);
                        PsiClass ownerClass = psiClassOwner.getClasses()[0];
                        String className = ownerClass.getName();
                        PsiDocComment docComment = ownerClass.getDocComment();
                        String text = docComment != null ? docComment.getText() : "";
                        String referenceClassName = getReferenceClassName(className, text);
                        PsiField psiField = elementFactory.createFieldFromText(String.format("private static final MappingPath %s_PATH =%s.TABLE", fieldName, referenceClassName + "Proxy"), ownerClass);
                        PsiAnnotation annotationFromText = elementFactory.createAnnotationFromText(String.format("@NavigateFlat(pathAlias=\"%s_PATH\")", fieldName), psiField);
                        PsiElement prevSibling = elementAt.getPrevSibling().getPrevSibling().getPrevSibling().getPrevSibling();
                        PsiModifierList psiModifierList = prevSibling instanceof PsiModifierList ? (PsiModifierList) prevSibling : null;
                        WriteCommandAction.runWriteCommandAction(project, () -> {

                            if (psiModifierList != null) {
                                psiFile.addBefore(annotationFromText, psiModifierList);
                            } else {
                                psiFile.addBefore(annotationFromText, elementAt.getPrevSibling().getPrevSibling().getPrevSibling().getPrevSibling().getPrevSibling());
                            }
                            psiFile.addBefore(psiField, elementAt.getParent());
                        });
                    }
                    System.out.println("123");
                }

            }


        } catch (Exception ex) {
            Messages.showErrorDialog(e.getProject(), "请按规定：将光标移动到对应的Class的属性上:" + ex.getMessage(), "错误提示");
        }
    }

//    private static final String LINK_SEE_CLASS_REGEX = "@see\\s+([\\w\\.]+)|\\{@link\\s+([\\w\\.]+)\\}";
    private static final String SEE_CLASS_REGEX = "@see\\s+([\\w\\.]+)";

    private String getReferenceClassName(String className, String docText) {
        String result = getReferenceClassName0(className, docText).trim();
        if (result.contains(".")) {
            return StrUtil.subAfter(result, ".", true);
        } else {
            return result;
        }
    }

    private String getReferenceClassName0(String className, String docText) {
//        docText = docText.replaceAll("\n", "");
        if (StrUtil.isNotBlank(docText)) {
            String newDocText = docText.replaceAll("@link", "@see");
            Pattern pattern = Pattern.compile(SEE_CLASS_REGEX);

            // 创建匹配器
            Matcher matcher = pattern.matcher(newDocText);
            if (matcher.find()) {
                String result = matcher.group(1);
                if (StrUtil.isNotBlank(result)) {
                    return result;
                } else {
                    String resultLink = matcher.group(2);
                    if (StrUtil.isNotBlank(resultLink)) {
                        return resultLink;
                    }
                }
            }
        }
        return className;
    }

    private static final PsiElementPattern.Capture<PsiElement> ELEMENT_FIELD = PlatformPatterns.psiElement().withParent(PsiField.class);
}
