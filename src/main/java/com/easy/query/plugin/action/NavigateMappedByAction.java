package com.easy.query.plugin.action;

import com.easy.query.plugin.core.persistent.EasyQueryQueryPluginConfigData;
import com.easy.query.plugin.core.util.MyStringUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.PsiUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.easy.query.plugin.core.validator.InputAnyValidatorImpl;
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
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * create time 2024/6/10 13:41
 * 文件说明
 *
 * @author xuejiaming
 */
public class NavigateMappedByAction extends AnAction {

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
                        //获取当前类
                        PsiClass ownerClass = psiClassOwner.getClasses()[0];
                        String className = ownerClass.getName();

                        PsiField currentField = PsiTreeUtil.getParentOfType(elementAt, PsiField.class);
                        if (currentField == null) {
                            Messages.showErrorDialog(project, "请选择对应的字段", "错误提示");
                            return;
                        }

                        PsiAnnotation navigate = currentField.getAnnotation("com.easy.query.core.annotation.Navigate");
                        if (navigate == null) {
                            Messages.showErrorDialog(project, "请选择对应的导航字段", "错误提示");
                            return;
                        }
                        String psiFieldPropertyType = PsiUtil.getPsiFieldPropertyType(currentField, true);
                        PsiClass targetClass = JavaPsiFacade.getInstance(project).findClass(psiFieldPropertyType, GlobalSearchScope.allScope(project));
                        if (targetClass == null) {
                            Messages.showErrorDialog(project, "请选择对应的导航目标类:" + psiFieldPropertyType, "错误提示");
                            return;
                        }
                        PsiFile targetFile = targetClass.getContainingFile();

                        String navigateValue = PsiUtil.getPsiAnnotationValue(navigate, "value", "");
                        if (StrUtil.isBlank(navigateValue)) {
                            Messages.showErrorDialog(project, "无法获取字段Navigate.value值", "错误提示");
                            return;
                        }
                        String selfProperty = PsiUtil.getPsiAnnotationValue(navigate, "selfProperty", "");
                        String targetProperty = PsiUtil.getPsiAnnotationValue(navigate, "targetProperty", "");
                        String mappingClass = PsiUtil.getPsiAnnotationValue(navigate, "mappingClass", "");
                        String selfMappingProperty = PsiUtil.getPsiAnnotationValue(navigate, "selfMappingProperty", "");
                        String targetMappingProperty = PsiUtil.getPsiAnnotationValue(navigate, "targetMappingProperty", "");

                        String navigateAnnotation = getNavigateAnnotation(navigateValue, selfProperty, targetProperty, mappingClass, selfMappingProperty, targetMappingProperty, className);


                        Messages.InputDialog dialog = new Messages.InputDialog("请输入字段名空则使用默认字段名", "提示名称", Messages.getQuestionIcon(), "", new InputAnyValidatorImpl());
                        dialog.show();

                        String myFieldName = "";
                        if (dialog.isOK()) {
                            myFieldName = dialog.getInputString();
                        }

                        String navigateField = getNavigateField(navigateValue, className, myFieldName);
                        PsiField psiField = elementFactory.createFieldFromText(navigateField, targetClass);
                        PsiAnnotation annotationFromText = elementFactory.createAnnotationFromText(navigateAnnotation, targetClass);
                        PsiModifierList modifierList = psiField.getModifierList();
                        if (modifierList == null) {
                            Messages.showErrorDialog(project, "无法获取字段PsiModifierList值", "错误提示");
                            return;
                        }
                        // 3. 把注解添加到字段
                        psiField.getModifierList().addBefore(annotationFromText, psiField.getModifierList().getFirstChild());

                        WriteCommandAction.runWriteCommandAction(project, () -> {


                            // 4. 添加字段到类中
                            targetClass.add(psiField);
                        });
                    }
                }

            }


        } catch (Exception ex) {
            Messages.showErrorDialog(e.getProject(), "请按规定：将光标移动到对应的Class的属性上:" + ex.getMessage(), "错误提示");
        }
    }

    private String getNavigateField(String value, String className, String myFieldName) {

        StringBuilder sb = new StringBuilder("private ");
        if (Objects.equals("RelationTypeEnum.OneToOne", value)) {
            if (StrUtil.isNotBlank(myFieldName)) {
                sb.append(className).append(" ").append(myFieldName).append(";");
            } else {
                sb.append(className).append(" item;");
            }
        } else if (Objects.equals("RelationTypeEnum.OneToMany", value)) {
            if (StrUtil.isNotBlank(myFieldName)) {
                sb.append(className).append(" ").append(myFieldName).append(";");
            } else {
                sb.append(className).append(" item;");
            }
        } else if (Objects.equals("RelationTypeEnum.ManyToOne", value)) {
            if (StrUtil.isNotBlank(myFieldName)) {
                sb.append(String.format("List<%s> %s;", className, myFieldName));
            } else {
                sb.append(String.format("List<%s> items;", className));
            }
        } else if (Objects.equals("RelationTypeEnum.ManyToMany", value)) {
            if (StrUtil.isNotBlank(myFieldName)) {
                sb.append(String.format("List<%s> %s;", className, myFieldName));
            } else {
                sb.append(String.format("List<%s> items;", className));
            }
        }
        return sb.toString();
    }

    private String getNavigateAnnotation(String value, String selfProperty, String targetProperty, String mappingClass, String selfMappingProperty, String targetMappingProperty, String className) {

        StringBuilder sb = new StringBuilder("@Navigate(value = RelationTypeEnum.");
        if (Objects.equals("RelationTypeEnum.OneToOne", value)) {
            sb.append("OneToOne");
        } else if (Objects.equals("RelationTypeEnum.OneToMany", value)) {
            sb.append("ManyToOne");
        } else if (Objects.equals("RelationTypeEnum.ManyToOne", value)) {
            sb.append("OneToMany");
        } else if (Objects.equals("RelationTypeEnum.ManyToMany", value)) {
            sb.append("ManyToMany");
        }
        if (StrUtil.isNotBlank(targetProperty) && !Objects.equals("{}", targetProperty)) {
            sb.append(",selfProperty=").append(targetProperty);
        }
        if (Objects.equals("RelationTypeEnum.ManyToMany", value)) {
            if (StrUtil.isNotBlank(targetMappingProperty) && !Objects.equals("{}", targetMappingProperty)) {
                sb.append(",selfMappingProperty=").append(targetMappingProperty);
            }
            if (StrUtil.isNotBlank(mappingClass) && !Objects.equals("Object.class", mappingClass)) {
                sb.append(",mappingClass=").append(mappingClass);
            }
            if (StrUtil.isNotBlank(selfMappingProperty) && !Objects.equals("{}", selfMappingProperty)) {
                sb.append(",targetMappingProperty=").append(selfMappingProperty);
            }
        }
        if (StrUtil.isNotBlank(selfProperty) && !Objects.equals("{}", selfProperty)) {
            sb.append(",targetProperty=").append(selfProperty);
        }
        sb.append(")");
        String field = sb.toString();
        field = field.replace(".Fields.", ".#Fields#.");
        field = field.replace("Fields.", className + ".Fields.");
        field = field.replace(".#Fields#.", ".Fields.");
        return field;
    }


    private static final PsiElementPattern.Capture<PsiElement> ELEMENT_FIELD = PlatformPatterns.psiElement().withParent(PsiField.class);
}
