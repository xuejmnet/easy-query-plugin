package com.easy.query.plugin.core.completion;

import cn.hutool.core.lang.Pair;
import com.easy.query.plugin.action.navgen.NavMappingGUI;
import com.easy.query.plugin.action.navgen.NavMappingRelation;
import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.IdeaUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtAnnotationEntry;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtClassBody;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtFunction;
import org.jetbrains.kotlin.psi.KtParameter;
import org.jetbrains.kotlin.psi.KtProperty;
import org.jetbrains.kotlin.psi.KtTypeReference;
import org.jetbrains.kotlin.psi.KtUserType;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 导航2映射自动补全 - Kotlin 版本
 * 支持普通 class 和 data class
 *
 * @author xuejiaming
 */
public class TableNavMappingCompletionKotlin extends CompletionContributor {

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {

        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }

        PsiElement position = parameters.getPosition();
        
        // 检查是否在 Kotlin 文件中
        KtFile ktFile = PsiTreeUtil.getTopmostParentOfType(position, KtFile.class);
        if (ktFile == null) {
            return;
        }

        // 获取当前所在的类
        KtClass ktClass = PsiTreeUtil.getParentOfType(position, KtClass.class);
        if (ktClass == null) {
            return;
        }

        // 检查是否在方法体内，如果是则不处理（避免在方法内部触发）
        if (PsiTreeUtil.getParentOfType(position, KtFunction.class) != null) {
            return;
        }
        
        // 检查是否在代码块内（如 init 块），如果是则不处理
        if (PsiTreeUtil.getParentOfType(position, KtBlockExpression.class) != null) {
            return;
        }

        // 检查是否有 @Table 注解
        if (!hasTableAnnotation(ktClass)) {
            return;
        }

        String currentClassQualifiedName = getClassQualifiedName(ktClass, ktFile);
        if (currentClassQualifiedName == null) {
            return;
        }

        // 获取当前类的所有字段（包括主构造函数参数和普通属性）
        List<Pair<String, String>> currentClassFields = getClassFields(ktClass);

        LookupElement lookupElementWithEq = PrioritizedLookupElement.withPriority(
            LookupElementBuilder.create("nav2 easyquery @Navigate生成")
                .withInsertHandler((context, item) -> {
                    Project project = context.getProject();

                    // 将 final 变量移到外面，以便 lambda 访问
                    final String finalCurrentClassQualifiedName = currentClassQualifiedName;
                    final List<Pair<String, String>> finalCurrentClassFields = currentClassFields;

                    Consumer<NavMappingRelation> callback = (mappingRelation) -> {
                        ApplicationManager.getApplication().runWriteAction(() -> {
                            String navText = generateNavPropertyCode(project, mappingRelation);
                            context.getDocument().replaceString(context.getStartOffset() - 1, // 需要往后回退一位, 将@ 符号也一起替换掉
                                context.getTailOffset(), navText);
                        });
                    };

                    SwingUtilities.invokeLater(() -> {
                        // 使用 ReadAction.compute() 在读取操作中获取 PSI 数据
                        Map<String, Object> psiData = ReadAction.compute(() -> {
                            Map<String, Object> data = new HashMap<>();
                            Collection<PsiClass> entityClasses = PsiJavaFileUtil.getAnnotationPsiClass(project,
                                "com.easy.query.core.annotation.Table");

                            List<Pair<String, String>> entities = entityClasses.stream()
                                .map(clazz -> Pair.of(clazz.getQualifiedName(), getCommentFromPsiClass(clazz)))
                                .collect(Collectors.toList());
                            data.put("entities", entities);

                            Map<String, List<Pair<String, String>>> entityAttributesMap = new HashMap<>();

                            for (PsiClass psiClass : entityClasses) {
                                List<Pair<String, String>> fields = Arrays.stream(psiClass.getAllFields())
                                    .filter(field -> !ignoreField(field))
                                    .filter(field -> !hasNavigateAnnotation(field))
                                    .map(field -> Pair.of(field.getName(), getCommentFromPsiField(field)))
                                    .collect(Collectors.toList());
                                entityAttributesMap.put(psiClass.getQualifiedName(), fields);
                            }

                            // 添加当前 Kotlin 类的字段
                            entityAttributesMap.put(finalCurrentClassQualifiedName, finalCurrentClassFields);
                            data.put("entityAttributesMap", entityAttributesMap);
                            return data;
                        });

                        // 从 ReadAction 返回的数据中获取
                        @SuppressWarnings("unchecked")
                        List<Pair<String, String>> entities = (List<Pair<String, String>>) psiData.get("entities");
                        @SuppressWarnings("unchecked")
                        Map<String, List<Pair<String, String>>> entityAttributesMap = (Map<String, List<Pair<String, String>>>) psiData.get("entityAttributesMap");

                        NavMappingGUI gui = new NavMappingGUI(entities, finalCurrentClassQualifiedName,
                            null, entityAttributesMap, callback);
                        gui.setVisible(true);
                    });
                })
                .withIcon(Icons.EQ),
            400d);
        result.addElement(lookupElementWithEq);

    }

    /**
     * 检查 KtClass 是否有 @Table 注解
     */
    private boolean hasTableAnnotation(KtClass ktClass) {
        for (KtAnnotationEntry annotation : ktClass.getAnnotationEntries()) {
            String shortName = annotation.getShortName() != null ? annotation.getShortName().asString() : null;
            if ("Table".equals(shortName)) {
                return true;
            }
            // 尝试解析全限定名
            String fqName = getAnnotationFqName(annotation);
            if ("com.easy.query.core.annotation.Table".equals(fqName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取注解的全限定名
     */
    private String getAnnotationFqName(KtAnnotationEntry annotation) {
        try {
            // 尝试从类型引用获取
            KtTypeReference typeRef = annotation.getTypeReference();
            if (typeRef != null) {
                KtUserType userType = typeRef.getTypeElement() instanceof KtUserType ? 
                    (KtUserType) typeRef.getTypeElement() : null;
                if (userType != null) {
                    // 尝试解析引用
                    String text = userType.getReferencedName();
                    if (text != null && text.contains(".")) {
                        return text;
                    }
                }
            }
            // 尝试通过短名称推断
            String shortName = annotation.getShortName() != null ? 
                annotation.getShortName().asString() : null;
            if ("Table".equals(shortName)) {
                return "com.easy.query.core.annotation.Table";
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return null;
    }

    /**
     * 获取类的全限定名
     */
    private String getClassQualifiedName(KtClass ktClass, KtFile ktFile) {
        try {
            // 从 package 和类名构建全限定名
            String packageName = ktFile.getPackageFqName() != null ? 
                ktFile.getPackageFqName().asString() : "";
            String className = ktClass.getName();
            if (className == null) {
                return null;
            }
            if (packageName.isEmpty()) {
                return className;
            }
            return packageName + "." + className;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取类的所有字段（包括主构造函数参数和普通属性）
     */
    private List<Pair<String, String>> getClassFields(KtClass ktClass) {
        List<Pair<String, String>> fields = new java.util.ArrayList<>();

        // 1. 获取主构造函数参数（data class 和普通 class 都可能有）
        if (ktClass.getPrimaryConstructor() != null) {
            for (KtParameter param : ktClass.getPrimaryConstructor().getValueParameters()) {
                if (!ignoreKtParameter(param) && !hasNavigateAnnotation(param)) {
                    fields.add(Pair.of(param.getName(), getCommentFromKtParameter(param)));
                }
            }
        }

        // 2. 获取类体内的普通属性
        KtClassBody classBody = ktClass.getBody();
        if (classBody != null) {
            for (KtDeclaration declaration : classBody.getDeclarations()) {
                if (declaration instanceof KtProperty) {
                    KtProperty property = (KtProperty) declaration;
                    if (!ignoreKtProperty(property) && !hasNavigateAnnotation(property)) {
                        fields.add(Pair.of(property.getName(), getCommentFromKtProperty(property)));
                    }
                }
            }
        }

        return fields;
    }

    /**
     * 判断是否需要忽略该参数
     */
    private boolean ignoreKtParameter(KtParameter param) {
        // 类似 Java 的忽略逻辑：静态字段、transient 等
        // Kotlin 中通常不需要特别处理，可以根据需要扩展
        return false;
    }

    /**
     * 判断是否需要忽略该属性
     */
    private boolean ignoreKtProperty(KtProperty property) {
        // 类似 Java 的忽略逻辑
        return false;
    }

    /**
     * 检查 KtParameter 是否有 @Navigate 注解
     */
    private boolean hasNavigateAnnotation(KtParameter param) {
        for (KtAnnotationEntry annotation : param.getAnnotationEntries()) {
            String shortName = annotation.getShortName() != null ? annotation.getShortName().asString() : null;
            if ("Navigate".equals(shortName) || "NavigateFlat".equals(shortName) || "NavigateJoin".equals(shortName)) {
                return true;
            }
            String fqName = getAnnotationFqName(annotation);
            if (fqName != null && (fqName.contains("Navigate"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查 KtProperty 是否有 @Navigate 注解
     */
    private boolean hasNavigateAnnotation(KtProperty property) {
        for (KtAnnotationEntry annotation : property.getAnnotationEntries()) {
            String shortName = annotation.getShortName() != null ? annotation.getShortName().asString() : null;
            if ("Navigate".equals(shortName) || "NavigateFlat".equals(shortName) || "NavigateJoin".equals(shortName)) {
                return true;
            }
            String fqName = getAnnotationFqName(annotation);
            if (fqName != null && (fqName.contains("Navigate"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从 KtParameter 获取注释
     */
    private String getCommentFromKtParameter(KtParameter param) {
        // Kotlin 参数通常没有 KDoc，可以返回空或尝试从其他位置获取
        return "";
    }

    /**
     * 从 KtProperty 获取注释
     */
    private String getCommentFromKtProperty(KtProperty property) {
        // 尝试获取 KDoc
        if (property.getDocComment() != null) {
            return property.getDocComment().getText();
        }
        return "";
    }

    /**
     * 从 PsiClass 获取注释（用于兼容 Java 实体类）
     */
    private String getCommentFromPsiClass(PsiClass psiClass) {
        if (psiClass.getDocComment() != null) {
            return psiClass.getDocComment().getText();
        }
        return "";
    }

    /**
     * 从 PsiField 获取注释（用于兼容 Java 实体类）
     */
    private String getCommentFromPsiField(com.intellij.psi.PsiField field) {
        if (field.getDocComment() != null) {
            return field.getDocComment().getText();
        }
        return "";
    }

    /**
     * 判断是否需要忽略该字段（Java 兼容）
     */
    private boolean ignoreField(com.intellij.psi.PsiField field) {
        // 复用 Java 版本的逻辑
        return com.easy.query.plugin.core.util.PsiJavaFieldUtil.ignoreField(field);
    }

    /**
     * 检查 Java 字段是否有 Navigate 注解（兼容）
     */
    private boolean hasNavigateAnnotation(com.intellij.psi.PsiField field) {
        return com.easy.query.plugin.core.util.EasyQueryElementUtil.hasNavigateAnnotation(field);
    }

    public static String generateNavPropertyCode(Project project, NavMappingRelation relation) {
        String sourceEntityFullName = relation.getSourceEntity();
        String targetEntityFullName = relation.getTargetEntity();
        String middleEntityFullName = relation.getMappingClass();

        // 找到对应实体类
        PsiClass sourceEntityClass = PsiJavaFileUtil.getPsiClass(project, sourceEntityFullName);
        String sourceEntitySimpleName = sourceEntityClass != null ? sourceEntityClass.getName() : getSimpleName(sourceEntityFullName);

        boolean noLombok = sourceEntityClass == null || 
            (sourceEntityClass.getAnnotation("lombok.experimental.FieldNameConstants") == null
            && findStaticInnerClass(sourceEntityClass, "Fields") == null);
        
        String sourceFieldPrefix;
        if (noLombok) {
            sourceFieldPrefix = (sourceEntitySimpleName + "Proxy");
        } else {
            sourceFieldPrefix = sourceEntitySimpleName;
        }

        PsiClass targetEntityClass = PsiJavaFileUtil.getPsiClass(project, targetEntityFullName);
        String targetEntitySimpleName = targetEntityClass != null ? targetEntityClass.getName() : getSimpleName(targetEntityFullName);

        PsiAnnotation targetEntityClassAnnotation = targetEntityClass != null ? 
            targetEntityClass.getAnnotation("lombok.experimental.FieldNameConstants") : null;
        String targetFieldPrefix;
        if (targetEntityClassAnnotation == null) {
            targetFieldPrefix = (targetEntitySimpleName + "Proxy");
        } else {
            targetFieldPrefix = targetEntitySimpleName;
        }

        String middleEntitySimpleName;
        String middleFieldPrefix;
        PsiClass middleEntityClass = null;
        if (StrUtil.isNotBlank(middleEntityFullName)) {
            middleEntityClass = PsiJavaFileUtil.getPsiClass(project, middleEntityFullName);
            middleEntitySimpleName = middleEntityClass != null ? middleEntityClass.getName() : getSimpleName(middleEntityFullName);

            PsiAnnotation middleEntityClassAnnotation = middleEntityClass != null ?
                middleEntityClass.getAnnotation("lombok.experimental.FieldNameConstants") : null;
            if (middleEntityClassAnnotation == null) {
                middleFieldPrefix = (middleEntitySimpleName + "Proxy");
            } else {
                middleFieldPrefix = middleEntitySimpleName;
            }
        } else {
            middleEntitySimpleName = null;
            middleFieldPrefix = null;
        }

        StringBuilder code = new StringBuilder();

        // 需要获取当前的 project
        String newLine = IdeaUtil.lineSeparator();

        code.append("/**");
        code.append(newLine);
        code.append(" *");
        code.append(newLine);
        code.append(" */");
        code.append(newLine);

        // 生成注解 - Kotlin 语法
        code.append("@Navigate(");

        // 添加关系类型
        code.append("value = RelationTypeEnum.").append(relation.getRelationType());

        // 添加 selfProperty - Kotlin 使用 [ ] 数组语法
        if (relation.getSourceFields() != null && relation.getSourceFields().length > 0) {
            code.append(", selfProperty = [");
            List<NavigateField> navigateFields = mappingFieldToConstField(relation.getSourceFields(), sourceEntityClass, sourceFieldPrefix);
            code.append(navigateFields.stream().map(s -> s.prefix + ".Fields." + s.field).collect(Collectors.joining(", ")));
            code.append("]");
        }

        // 添加 selfMappingProperty
        if (relation.getSelfMappingFields() != null && relation.getSelfMappingFields().length > 0) {
            code.append(", selfMappingProperty = [");
            if (middleEntityClass == null) {
                code.append(Arrays.stream(relation.getSelfMappingFields()).map(s -> middleEntityFullName + ".Fields." + s).collect(Collectors.joining(", ")));
            } else {
                List<NavigateField> navigateFields = mappingFieldToConstField(relation.getSelfMappingFields(), middleEntityClass, middleFieldPrefix);
                code.append(navigateFields.stream().map(s -> s.prefix + ".Fields." + s.field).collect(Collectors.joining(", ")));
            }
            code.append("]");
        }

        // 添加 mappingClass - Kotlin 使用 ::class
        if (middleEntityFullName != null && !middleEntityFullName.isEmpty()) {
            code.append(", mappingClass = ").append(middleEntitySimpleName).append("::class");
        }

        // 添加 targetProperty
        if (relation.getTargetFields() != null && relation.getTargetFields().length > 0) {
            code.append(", targetProperty = [");
            List<NavigateField> navigateFields = mappingFieldToConstField(relation.getTargetFields(), targetEntityClass, targetFieldPrefix);
            code.append(navigateFields.stream().map(s -> s.prefix + ".Fields." + s.field).collect(Collectors.joining(", ")));
            code.append("]");
        }

        // 添加 targetMappingProperty
        if (relation.getTargetMappingFields() != null && relation.getTargetMappingFields().length > 0) {
            code.append(", targetMappingProperty = [");
            if (middleEntityClass == null) {
                code.append(Arrays.stream(relation.getTargetMappingFields()).map(s -> middleEntityFullName + ".Fields." + s).collect(Collectors.joining(", ")));
            } else {
                List<NavigateField> navigateFields = mappingFieldToConstField(relation.getTargetMappingFields(), middleEntityClass, middleFieldPrefix);
                code.append(navigateFields.stream().map(s -> s.prefix + ".Fields." + s.field).collect(Collectors.joining(", ")));
            }
            code.append("]");
        }

        code.append(")");
        code.append(newLine);
        
        // Kotlin 属性声明语法
        boolean toMany = StrUtil.endWith(relation.getRelationType(), "ToMany");
        String propertyName;
        if (StrUtil.isBlank(targetEntitySimpleName)) {
            propertyName = toMany ? "itemList" : "item";
        } else {
            propertyName = StrUtil.toLowerCaseFirstOne(targetEntitySimpleName) + (toMany ? "List" : "");
        }
        
        code.append("var ").append(propertyName).append(":");
        if (toMany) {
            code.append("MutableList<");
        }
        code.append(targetEntitySimpleName);
        if (toMany) {
            code.append(">");
        }
        code.append("? = null");

        return code.toString();
    }

    public static PsiClass findStaticInnerClass(PsiClass psiClass, String innerClassName) {
        if (psiClass == null) return null;
        for (PsiClass innerClass : psiClass.getInnerClasses()) {
            if (innerClass.getName() != null
                && innerClass.getName().equals(innerClassName)
                && innerClass.hasModifierProperty(PsiModifier.STATIC)) {
                return innerClass;
            }
        }
        return null;
    }

    private static String getSimpleName(String fullName) {
        if (fullName == null) return "";
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(lastDot + 1) : fullName;
    }

    /**
     * 处理lombok的@FieldNameConstants
     */
    private static List<NavigateField> mappingFieldToConstField(String[] mappingFields, PsiClass psiClass, String defaultPrefix) {
        if (psiClass == null) {
            // 如果类找不到，使用默认前缀
            return Arrays.stream(mappingFields).map(f -> {
                NavigateField nf = new NavigateField();
                nf.prefix = defaultPrefix;
                nf.field = f;
                return nf;
            }).collect(Collectors.toList());
        }

        String psiClassName = psiClass.getName();
        Map<String, com.intellij.psi.PsiField> fieldMap = Arrays.stream(psiClass.getAllFields())
            .collect(Collectors.toMap(com.intellij.psi.PsiField::getName, s -> s, (v1, v2) -> v2));

        PsiAnnotation psiClassAnnotation = psiClass.getAnnotation("lombok.experimental.FieldNameConstants");
        boolean lombokField = psiClassAnnotation != null;
        String prefix = lombokField ? psiClassName : (psiClassName + "Proxy");

        return Arrays.stream(mappingFields).map(f -> {
            NavigateField navigateField = new NavigateField();
            navigateField.prefix = prefix;
            navigateField.field = f;
            com.intellij.psi.PsiField psiField = fieldMap.get(f);
            if (lombokField && psiField != null && psiField.getParent() instanceof PsiClass) {
                PsiClass parent = (PsiClass) psiField.getParent();
                PsiAnnotation parentAnnotation = parent.getAnnotation("lombok.experimental.FieldNameConstants");
                if (parentAnnotation != null) {
                    navigateField.prefix = parent.getName();
                } else {
                    navigateField.prefix = (psiClassName + "Proxy");
                }
            }
            return navigateField;
        }).collect(Collectors.toList());
    }

    public static class NavigateField {
        public String prefix;
        public String field;
    }
}
