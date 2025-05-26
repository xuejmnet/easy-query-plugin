package com.easy.query.plugin.core.reference;

import cn.hutool.core.util.ArrayUtil;
import com.easy.query.plugin.core.ResultWithError;
import com.easy.query.plugin.core.util.PsiJavaClassUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.lang.jvm.types.JvmReferenceType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.PsiClassReferenceType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PathAliasUtils {

    /**
     * 查找单个段落的目标元素的逻辑。
     *
     * @param project        当前项目
     * @param currentElement 当前元素
     * @param segments       当前段落数组
     * @return 目标 PsiElement
     */
    @NotNull
    public static ResultWithError<PsiElement> findSegmentTargetElement(Project project, PsiElement currentElement, String[] segments) {
        // 获取当前元素最近的 PsiClass
        PsiClass javaClass = PsiTreeUtil.getParentOfType(currentElement, PsiClass.class);
        if (javaClass == null) {
            return new ResultWithError<>(null, "No PsiClass found");
        }

        // 获取 javaClass 的 link docTag 中的链接类
        PsiClass topLinkClass = PsiJavaClassUtil.getLinkPsiClass(javaClass);
        if (topLinkClass == null) {
            return new ResultWithError<>(null, "No link docTag found");
        }


        // 从链接类中找到第 0 个 segments 的字段
        PsiField field = topLinkClass.findFieldByName(segments[0], true);
        if (field == null) {
            if (segments.length == 1) {
                // 尝试从当前类获取一下
                return new ResultWithError<>(javaClass.findFieldByName(segments[0], true),null);
            }
            return new ResultWithError<>(null, "No field found");
        }
        if (segments.length == 1) {
            return new ResultWithError<>(field,null);
        }


        for (int i = 1; i < segments.length; i++) {

            PsiType fieldType = field.getType();
            // 看看这个类型是否包含泛型
            if (!(fieldType instanceof PsiClassReferenceType)) {
                return new ResultWithError<>(null, "No fieldType found");
            }
            // 应该都是这个类型
            PsiJavaCodeReferenceElement fieldTypeRef = ((PsiClassReferenceType) fieldType).getReference();
            String fieldTypeRefQualifiedName = fieldTypeRef.getQualifiedName();

            PsiClass psiClass = PsiJavaFileUtil.getPsiClass(project, fieldTypeRefQualifiedName);
            if (ArrayUtil.isNotEmpty(psiClass.getInterfaceTypes())) {
                // 实现了接口， 看看接口里面是否有集合
                for (JvmReferenceType interfaceType : psiClass.getInterfaceTypes()) {
                    if (interfaceType instanceof PsiType) {
                        String canonicalText = ((PsiType) interfaceType).getCanonicalText();
                        if (StrUtil.startWithAny(canonicalText, "java.util.Collection")) {
                            PsiType[] parameters = ((PsiClassType) fieldType).getParameters();
                            if (parameters.length > 0) {
                                fieldType = parameters[0]; // 获取集合的泛型实际类型
                                fieldTypeRefQualifiedName = fieldType.getCanonicalText();
                            }
                            break;
                        }
                    }
                }
            }

            // 判断 fieldTypeRef 是不是数组类型
            if (fieldType instanceof PsiArrayType) {
                fieldType = ((PsiArrayType) fieldType).getComponentType(); // 获取数组的组件类型
                fieldTypeRefQualifiedName = fieldType.getCanonicalText();
            }

            // 看看是否是最后一段
            if (i == segments.length - 1) {
                // 如果是最后一段， 且是String  Date LocalDate 等常见类型， 则返回
                if (StrUtil.startWithAny(fieldTypeRefQualifiedName, "java.lang", "java.util")) {
                    return new ResultWithError<>(field,null);
                }
                // 如果是枚举类型， 则返回
                if (fieldTypeRefQualifiedName.equals("java.lang.Enum")) {
                    return new ResultWithError<>(field,null);
                }
            }

            // 不是最后一段， 继续解析
            psiClass = PsiJavaFileUtil.getPsiClass(project, fieldType.getCanonicalText());
            if (psiClass == null) {
                // 没有对应的类，视为解析失败
                return new ResultWithError<>(field,  "No class found");
            }

            PsiField currentLevelField = psiClass.findFieldByName(segments[i], true);
            if (currentLevelField == null) {
                return new ResultWithError<>(null, "No currentLevelField found");
            }

            field = currentLevelField;

            if (i == segments.length - 1) {
                return new ResultWithError<>(field,null);
            }
        }


        return new ResultWithError<>(null,"end not found any element");
    }
}
