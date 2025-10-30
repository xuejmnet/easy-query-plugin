package com.easy.query.plugin.core.completion;

import cn.hutool.core.lang.Pair;
import com.easy.query.plugin.action.navgen.NavMappingGUI;
import com.easy.query.plugin.action.navgen.NavMappingRelation;
import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.EasyQueryElementUtil;
import com.easy.query.plugin.core.util.IdeaUtil;
import com.easy.query.plugin.core.util.PsiCommentUtil;
import com.easy.query.plugin.core.util.PsiJavaClassUtil;
import com.easy.query.plugin.core.util.PsiJavaFieldUtil;
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
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

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
 * 导航2映射自动补全
 *
 * @author xuejiaming
 */
public class TableNavMappingCompletion extends CompletionContributor {

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {

        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }

        PsiElement position = parameters.getPosition();
//        PsiClass topLevelDtoClass = PsiTreeUtil.getTopmostParentOfType(position, PsiClass.class);
//        if (!PsiJavaClassUtil.isElementRelatedToClass(position)) {
//            // 只处理类下面的直接元素, 方法内的不处理
//            return;
//        }
        PsiClass currentPsiClass = PsiTreeUtil.getParentOfType(position, PsiClass.class);
        if (Objects.isNull(currentPsiClass)) {
            return;
        }
//        if (PsiTreeUtil.getParentOfType(position, PsiAnnotation.class) == null) {
//            // 当前输入的不是注解
//            System.out.println("456456745566");
//            return;
//        }
        //是否是数据库对象
        PsiAnnotation entityTable = currentPsiClass.getAnnotation("com.easy.query.core.annotation.Table");
        if (entityTable == null) {
            return;
        }


        String currentClassQualifiedName = currentPsiClass.getQualifiedName();
        List<Pair<String, String>> currentClassFields = Arrays.stream(currentPsiClass.getAllFields())
            .filter(field -> !PsiJavaFieldUtil.ignoreField(field))
            .filter(field -> !EasyQueryElementUtil.hasNavigateAnnotation(field))
            .map(field -> Pair.of(field.getName(), PsiCommentUtil.getCommentDataStr(field.getDocComment())))
            .collect(Collectors.toList());

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

//                                List<String> entityClassQualifiedNameList = entityClasses.stream().map(PsiClass::getQualifiedName).collect(Collectors.toList());


                            List<Pair<String, String>> entities = entityClasses.stream()
                                .map(clazz -> Pair.of(clazz.getQualifiedName(), PsiCommentUtil.getCommentDataStr(clazz.getDocComment())))
                                .collect(Collectors.toList());
                            data.put("entities", entities);

                            Map<String, List<Pair<String, String>>> entityAttributesMap = new HashMap<>();


                            for (PsiClass psiClass : entityClasses) {
                                PsiField[] allFields = psiClass.getAllFields();
                                //((PsiClass)allFields[13].getParent()).getName()
                                entityAttributesMap.put(psiClass.getQualifiedName(), Arrays.stream(allFields)
                                    .filter(field -> !PsiJavaFieldUtil.ignoreField(field))
                                    .filter(field -> !EasyQueryElementUtil.hasNavigateAnnotation(field))
                                    .map(field -> Pair.of(field.getName(), PsiCommentUtil.getCommentDataStr(field.getDocComment())))
                                    .collect(Collectors.toList()));
                            }

                            // 当前可能是DTO
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

    public static PsiClass findStaticInnerClass(PsiClass psiClass, String innerClassName) {
        for (PsiClass innerClass : psiClass.getInnerClasses()) {
            if (innerClass.getName() != null
                && innerClass.getName().equals(innerClassName)
                && innerClass.hasModifierProperty(PsiModifier.STATIC)) {
                return innerClass;
            }
        }
        return null;
    }

    public static String generateNavPropertyCode(Project project, NavMappingRelation relation) {
        String sourceEntityFullName = relation.getSourceEntity();
        String targetEntityFullName = relation.getTargetEntity();
        String middleEntityFullName = relation.getMappingClass();

        // 找到对应实体类
        PsiClass sourceEntityClass = PsiJavaFileUtil.getPsiClass(project, sourceEntityFullName);
        String sourceEntitySimpleName = sourceEntityClass.getName();

        boolean noLombok = sourceEntityClass.getAnnotation("lombok.experimental.FieldNameConstants") == null//没有注解
            && findStaticInnerClass(sourceEntityClass, "Fields") == null;//也没有静态内部类 lombok可能是已经编译完了
        String sourceFieldPrefix;
        if (noLombok) {
            sourceFieldPrefix = (sourceEntitySimpleName + "Proxy");
        } else {
            sourceFieldPrefix = sourceEntitySimpleName;
        }
//        PsiJavaFile currentClassFile = (PsiJavaFile) sourceEntityClass.getContainingFile();

//        PsiImportList currentClassFileImportList = currentClassFile.getImportList();

        PsiClass targetEntityClass = PsiJavaFileUtil.getPsiClass(project, targetEntityFullName);
        String targetEntitySimpleName = targetEntityClass.getName();


        PsiAnnotation targetEntityClassAnnotation = targetEntityClass.getAnnotation("lombok.experimental.FieldNameConstants");
        String targetFieldPrefix;
        if (targetEntityClassAnnotation == null) {
            targetFieldPrefix = (targetEntitySimpleName + "Proxy");
        } else {
            targetFieldPrefix = targetEntitySimpleName;
        }

        // 添加TargetEntity的引入
//        currentClassFileImportList.add(PsiJavaFileUtil.createImportStatement(project, targetEntityClass));

        String middleEntitySimpleName;
        String middleFieldPrefix;
        PsiClass middleEntityClass;
        if (StrUtil.isNotBlank(middleEntityFullName)) {
            middleEntityClass = PsiJavaFileUtil.getPsiClass(project, middleEntityFullName);

            // 当前类需要导入包
//            currentClassFileImportList.add(PsiJavaFileUtil.createImportStatement(project, middleEntityClass));

            middleEntitySimpleName = middleEntityClass.getName();

            PsiAnnotation middleEntityClassAnnotation = middleEntityClass.getAnnotation("lombok.experimental.FieldNameConstants");
            if (middleEntityClassAnnotation == null) {
                middleFieldPrefix = (middleEntitySimpleName + "Proxy");
            } else {
                middleFieldPrefix = middleEntitySimpleName;
            }
        } else {
            middleEntitySimpleName = null;
            middleFieldPrefix = null;
            middleEntityClass = null;
        }


        StringBuilder code = new StringBuilder();

        // 需要获取当前的 project
        String newLine = IdeaUtil.lineSeparator();

        code.append("/**");
        code.append(newLine);
        code.append("*");
        code.append(newLine);
        code.append("**/");
        code.append(newLine);

        // 生成注解
        code.append("@Navigate(");

        // 添加关系类型
        code.append("value = RelationTypeEnum.").append(relation.getRelationType());

        // 添加 selfProperty
        if (relation.getSourceFields() != null && relation.getSourceFields().length > 0) {
            code.append(", selfProperty = {");
            List<NavigateField> navigateFields = mappingFieldToConstField(relation.getSourceFields(), sourceEntityClass);
            code.append(navigateFields.stream().map(s -> s.prefix + ".Fields." + s.field).collect(Collectors.joining(", ")));
            code.append("}");
        }

        // 添加 selfMappingProperty
        if (relation.getSelfMappingFields() != null && relation.getSelfMappingFields().length > 0) {
            code.append(", selfMappingProperty = {");
            if (middleEntityClass == null) {
                code.append(Arrays.stream(relation.getSelfMappingFields()).map(s -> middleEntityFullName + ".Fields." + s).collect(Collectors.joining(", ")));
            } else {
                List<NavigateField> navigateFields = mappingFieldToConstField(relation.getSelfMappingFields(), middleEntityClass);
                code.append(navigateFields.stream().map(s -> s.prefix + ".Fields." + s.field).collect(Collectors.joining(", ")));
            }
            code.append("}");
        }

        // 添加 mappingClass
        if (middleEntityFullName != null && !middleEntityFullName.isEmpty()) {
            code.append(", mappingClass = ").append(middleEntitySimpleName).append(".class");
        }

        // 添加 targetProperty
        if (relation.getTargetFields() != null && relation.getTargetFields().length > 0) {
            code.append(", targetProperty = {");
            List<NavigateField> navigateFields = mappingFieldToConstField(relation.getTargetFields(), targetEntityClass);
            code.append(navigateFields.stream().map(s -> s.prefix + ".Fields." + s.field).collect(Collectors.joining(", ")));
            code.append("}");
        }

        // 添加 targetMappingProperty
        if (relation.getTargetMappingFields() != null && relation.getTargetMappingFields().length > 0) {
            code.append(", targetMappingProperty = {");
            if (middleEntityClass == null) {
                code.append(Arrays.stream(relation.getTargetMappingFields()).map(s -> middleEntityFullName + ".Fields." + s).collect(Collectors.joining(", ")));
            } else {
                List<NavigateField> navigateFields = mappingFieldToConstField(relation.getTargetMappingFields(), middleEntityClass);
                code.append(navigateFields.stream().map(s -> s.prefix + ".Fields." + s.field).collect(Collectors.joining(", ")));
            }
            code.append("}");
        }
//        if ("OneToMany".equals(relation.getRelationType()) || "ManyToMany".equals(relation.getRelationType())) {
//
//            code.append(",subQueryToGroupJoin = true");
//        }
        code.append(")");
        code.append(newLine);
        code.append("private ");
        boolean toMany = StrUtil.endWith(relation.getRelationType(), "ToMany");
        if (toMany) {
            code.append("List<");
        }
        code.append(targetEntitySimpleName);
        if (toMany) {
            code.append(">");
        }
        code.append(" ");
        if (StrUtil.isBlank(targetEntitySimpleName)) {
            if (toMany) {
                code.append("itemList;");
            } else {
                code.append("item;");
            }
        } else {
            if (toMany) {
                code.append(StrUtil.toLowerCaseFirstOne(targetEntitySimpleName)).append("List;");
            } else {
                code.append(StrUtil.toLowerCaseFirstOne(targetEntitySimpleName)).append(";");
            }

        }

        return code.toString();
    }

    /**
     * 处理lombok的@FieldNameConstants
     *
     * @param mappingFields
     * @param psiClass
     * @return
     */
    private static List<NavigateField> mappingFieldToConstField(String[] mappingFields, PsiClass psiClass) {
        String psiClassName = psiClass.getName();
        Map<String, PsiField> fieldMap = Arrays.stream(psiClass.getAllFields()).collect(Collectors.toMap(s -> s.getName(), s -> s, (v1, v2) -> v2));

        PsiAnnotation psiClassAnnotation = psiClass.getAnnotation("lombok.experimental.FieldNameConstants");
        boolean lombokField = psiClassAnnotation != null;
        String prefix = lombokField ? psiClassName : (psiClassName + "Proxy");

        return Arrays.stream(mappingFields).map(f -> {
            NavigateField navigateField = new NavigateField();
            navigateField.prefix = prefix;
            navigateField.field = f;
            PsiField psiField = fieldMap.get(f);
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
