package com.easy.query.plugin.core.util;

import com.easy.query.plugin.core.enums.FileTypeEnum;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiArrayType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.javadoc.PsiDocComment;
import org.jetbrains.kotlin.psi.KtFile;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * create time 2023/9/16 12:17
 * 文件说明
 *
 * @author xuejiaming
 */
public class PsiUtil {
    public static FileTypeEnum getFileType(PsiClassOwner psiFile){
        if(psiFile instanceof PsiJavaFile){
            return FileTypeEnum.Java;
        }
        if(psiFile instanceof KtFile){
            return FileTypeEnum.Kotlin;
        }
        return FileTypeEnum.Unknown;
    }
    public static Set<String> getPsiAnnotationValues(PsiAnnotation annotation, String attr,Set<String> values){
        String psiAnnotationValue = getPsiAnnotationValueIfEmpty(annotation, attr, null);
        if(Objects.nonNull(psiAnnotationValue)){
            if(psiAnnotationValue.startsWith("{")&&psiAnnotationValue.endsWith("}")){
                psiAnnotationValue=psiAnnotationValue.substring(1,psiAnnotationValue.length()-1);
            }
            String[] split = psiAnnotationValue.split(",");
            Collections.addAll(values, split);
        }
        return values;
    }
    public static String getPsiAnnotationValueIfEmpty(PsiAnnotation annotation, String attr, String def){
        String psiAnnotationValue = getPsiAnnotationValue(annotation, attr, "");
        if(StrUtil.isBlank(psiAnnotationValue)){
            return def;
        }
        return psiAnnotationValue;
    }
    public static String getPsiAnnotationValue(PsiAnnotation annotation, String attr, String def){
        if(!Objects.isNull(annotation)){
            PsiAnnotationMemberValue value = annotation.findAttributeValue(attr);
            if(Objects.nonNull(value)){
                String text = value.getText();
                if(Objects.nonNull(text)){
                    return text.replace("\"", "");
                }
            }
        }

        return def;
    }
    private static String removeStarsAndTrim(String text) {
        // 定义正则表达式来匹配星号字符和首尾空白字符
        String regex = "(?s)/\\*\\*|\\*|\\s*(\\*?/|$)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        // 使用替换移除匹配的字符
        return matcher.replaceAll("").trim();
    }
    public static String getPsiFieldClearComment(PsiField field){

        String psiFieldComment = getPsiFieldComment(field, null);
        if(Objects.isNull(psiFieldComment)){
            return "/**";
        }
        return "/**\n" +
               "     * "+removeStarsAndTrim(psiFieldComment);
    }

    public static String getPsiFieldComment(PsiField field, String def){
        // 获取该字段的注释
        PsiElement[] children = field.getChildren();
        for (PsiElement child : children) {
            // 检查是否是注释
            if (child instanceof PsiComment) {
                PsiComment comment = (PsiComment) child;

                // 如果是 JavaDoc 注释，你可以使用以下方式获取其文本内容
                if (comment instanceof PsiDocComment) {
                    PsiDocComment docComment = (PsiDocComment) comment;
                    return docComment.getText();
                } else {
                    // 如果是普通注释，你可以使用以下方式获取其文本内容

                    return comment.getText();
                }
            }
        }
        return def;
    }
    public static String getPsiFieldPropertyType(PsiField field,boolean isInclude){
        // 获取属性类型
        PsiType fieldType = field.getType();
//        if(fieldType instanceof PsiArrayType){
//            return getPsiArrayFieldPropertyType((PsiArrayType) fieldType,isInclude);
//        }else
            if(fieldType instanceof  PsiClassType){
            return getPsiClassFieldPropertyType((PsiClassType)fieldType,isInclude);
        }
        return fieldType.getCanonicalText();
    }
    public static String getPsiClassFieldPropertyType(PsiClassType fieldType,boolean isInclude){
        if(fieldType.resolve() instanceof PsiTypeParameter){
            return "java.lang.Object";
        }
        String canonicalText = fieldType.getCanonicalText();
        if(isInclude){
            return parseGenericType(canonicalText);
        }
//        if (canonicalText.contains("<") && canonicalText.contains(">")){
//            return "java.lang.Object";
//        }
        return canonicalText;
    }
    public static String getPsiArrayFieldPropertyType(PsiArrayType fieldType,boolean isInclude){
        System.out.println("1");
        return "java.lang.Object";
    }
    public static String parseGenericType(String genericTypeString) {
        if(genericTypeString.contains(",")){
            return genericTypeString;
        }
        // 正则表达式用于匹配泛型类型字符串
        String regex = "<(.+?)>$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(genericTypeString);

        // 如果匹配成功，返回内部类型字符串；否则返回空字符串
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return genericTypeString;
        }
    }


    public static Collection<PsiField> getAllFields(PsiClass psiClass) {
        LinkedHashMap<String, PsiField> fields = getAllFields0(psiClass);
        return fields.values();
    }
    private static LinkedHashMap<String,PsiField> getAllFields0(PsiClass psiClass) {
        LinkedHashMap<String,PsiField> fields = new LinkedHashMap<>();
        // 递归获取父类的所有Field
        PsiClass superClass = psiClass.getSuperClass();
        if (superClass != null) {
            LinkedHashMap<String, PsiField> allFields0 = getAllFields0(superClass);
            fields.putAll(allFields0);
        }
        // 获取当前类的所有Field
        for (PsiField declaredField : psiClass.getAllFields()) {
            fields.put(declaredField.getName(),declaredField);
        }
        return fields;
    }
}
