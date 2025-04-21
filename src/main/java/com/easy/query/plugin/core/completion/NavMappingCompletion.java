package com.easy.query.plugin.core.completion;

import cn.hutool.core.lang.Pair;
import com.easy.query.plugin.action.navgen.NavMappingGUI;
import com.easy.query.plugin.action.navgen.NavMappingRelation;
import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.*;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.application.ReadAction;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 导航映射自动补全
 *
 * @author link2fun
 */
public class NavMappingCompletion extends CompletionContributor {

    @Override
    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {

        if (parameters.getCompletionType() != CompletionType.BASIC) {
            return;
        }

        PsiElement position = parameters.getPosition();
        PsiClass topLevelDtoClass = PsiTreeUtil.getTopmostParentOfType(position, PsiClass.class);
        if (!PsiJavaClassUtil.isElementRelatedToClass(position)) {
            // 只处理类下面的直接元素, 方法内的不处理
            return;
        }
        PsiClass currentPsiClass = PsiTreeUtil.getParentOfType(position, PsiClass.class);
        if (Objects.isNull(currentPsiClass)) {
            return;
        }
        if (PsiTreeUtil.getParentOfType(position, PsiAnnotation.class) == null) {
            // 当前输入的不是注解
            return;
        }
        // 需要是在字段上的注解
        if (!(PsiTreeUtil.getParentOfType(position, PsiField.class, PsiMethod.class, PsiClass.class) instanceof PsiField)) {
            return;
        }

        PsiField currentField = PsiTreeUtil.getParentOfType(position, PsiField.class);
        // 获取当前字段的类型
        if (currentField == null) {
            return;
        }
        PsiType currentFieldType = currentField.getType();
        String currentFieldTypeQualifiedName = null;
        if (currentFieldType instanceof PsiClassType) {
            PsiClassType classType = (PsiClassType) currentFieldType;
            PsiClass resolvedClass = classType.resolve();
            if (resolvedClass != null) {
                if (resolvedClass.getQualifiedName() != null && (resolvedClass.getQualifiedName().equals("java.util.List") || resolvedClass.getQualifiedName().equals("java.util.Set"))) {
                    PsiType[] genericParameters = classType.getParameters();
                    if (genericParameters.length > 0 && genericParameters[0] instanceof PsiClassType) {
                        PsiClass genericClass = ((PsiClassType) genericParameters[0]).resolve();
                        if (genericClass != null) {
                            currentFieldTypeQualifiedName = genericClass.getQualifiedName();
                        }
                    }
                } else {
                    currentFieldTypeQualifiedName = resolvedClass.getQualifiedName();
                }
            }
        }


        String currentClassQualifiedName = currentPsiClass.getQualifiedName();
        List<Pair<String, String>> currentClassFields = Arrays.stream(currentPsiClass.getAllFields())
                .filter(field -> !PsiJavaFieldUtil.ignoreField(field))
                .filter(field-> !EasyQueryElementUtil.hasNavigateAnnotation(field))
                // 还需要过滤掉当前字段， 毕竟 当前字段是被关联的， 所以不能作为关联属性
                .filter(field-> !StrUtil.equals(currentField.getName(), field.getName()))
                .map(field -> Pair.of(field.getName(), PsiCommentUtil.getCommentDataStr(field.getDocComment())))
                .collect(Collectors.toList());


        //

        String targetEntityClassName = currentFieldTypeQualifiedName;
        LookupElement lookupElementWithEq = PrioritizedLookupElement.withPriority(
                LookupElementBuilder.create("Nav EasyQuery @Navigate生成")
                        .withInsertHandler((context, item) -> {
                            Project project = context.getProject();
                            // 将 final 变量移到外面，以便 lambda 访问
                            final String finalCurrentClassQualifiedName = currentClassQualifiedName;
                            final List<Pair<String, String>> finalCurrentClassFields = currentClassFields;
                            final String finalTargetEntityClassName = targetEntityClassName;

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


                                    List<Pair<String,String>> entities = entityClasses.stream()
                                            .map(clazz-> Pair.of(clazz.getQualifiedName(),PsiCommentUtil.getCommentDataStr(clazz.getDocComment())))
                                            .collect(Collectors.toList());
                                    data.put("entities", entities);

                                    Map<String, List<Pair<String,String>>> entityAttributesMap = new HashMap<>();


                                    for (PsiClass psiClass : entityClasses) {
                                        PsiField[] allFields = psiClass.getAllFields();
                                        entityAttributesMap.put(psiClass.getQualifiedName(), Arrays.stream(allFields)
                                                .filter(field -> !PsiJavaFieldUtil.ignoreField(field))
                                                 .filter(field-> !EasyQueryElementUtil.hasNavigateAnnotation(field))
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
                                        finalTargetEntityClassName, entityAttributesMap, callback);
                                gui.setVisible(true);
                            });
                        })
                        .withIcon(Icons.EQ),
                400d);
        result.addElement(lookupElementWithEq);

    }

    public static String generateNavPropertyCode(Project project, NavMappingRelation relation) {
        String sourceEntityFullName = relation.getSourceEntity();
        String targetEntityFullName = relation.getTargetEntity();
        String middleEntityFullName = relation.getMappingClass();

        // 找到对应实体类
        PsiClass sourceEntityClass = PsiJavaFileUtil.getPsiClass(project, sourceEntityFullName);
        String sourceEntitySimpleName = sourceEntityClass.getName();
        PsiJavaFile currentClassFile = (PsiJavaFile) sourceEntityClass.getContainingFile();

        PsiImportList currentClassFileImportList = currentClassFile.getImportList();

        PsiClass targetEntityClass = PsiJavaFileUtil.getPsiClass(project, targetEntityFullName);
        String targetEntitySimpleName = targetEntityClass.getName();

        // 添加TargetEntity的引入
//        currentClassFileImportList.add(PsiJavaFileUtil.createImportStatement(project, targetEntityClass));

        String middleEntitySimpleName;
        if (StrUtil.isNotBlank(middleEntityFullName)) {
            PsiClass middleEntityClass = PsiJavaFileUtil.getPsiClass(project, middleEntityFullName);

            // 当前类需要导入包
//            currentClassFileImportList.add(PsiJavaFileUtil.createImportStatement(project, middleEntityClass));

            middleEntitySimpleName = middleEntityClass.getName();
        } else {
            middleEntitySimpleName = null;
        }


        StringBuilder code = new StringBuilder();

        // 需要获取当前的 project


        // 生成注解
        code.append("@Navigate(");

        // 添加关系类型
        code.append("value = RelationTypeEnum.").append(relation.getRelationType());

        // 添加 selfProperty
        if (relation.getSourceFields() != null && relation.getSourceFields().length > 0) {
            code.append(", selfProperty = {");
            code.append(Arrays.stream(relation.getSourceFields()).map(s -> sourceEntitySimpleName + ".Fields." + s + "").collect(Collectors.joining(", ")));
            code.append("}");
        }

        // 添加 selfMappingProperty
        if (relation.getSelfMappingFields() != null && relation.getSelfMappingFields().length > 0) {
            code.append(", selfMappingProperty = {");
            code.append(Arrays.stream(relation.getSelfMappingFields()).map(s -> middleEntitySimpleName + ".Fields." + s + "").collect(Collectors.joining(", ")));
            code.append("}");
        }

        // 添加 mappingClass
        if (middleEntityFullName != null && !middleEntityFullName.isEmpty()) {
            code.append(", mappingClass = ").append(middleEntitySimpleName).append(".class");
        }

        // 添加 targetProperty
        if (relation.getTargetFields() != null && relation.getTargetFields().length > 0) {
            code.append(", targetProperty = {");
            code.append(Arrays.stream(relation.getTargetFields()).map(s -> targetEntitySimpleName + ".Fields." + s + "").collect(Collectors.joining(", ")));
            code.append("}");
        }

        // 添加 targetMappingProperty
        if (relation.getTargetMappingFields() != null && relation.getTargetMappingFields().length > 0) {
            code.append(", targetMappingProperty = {");
            code.append(Arrays.stream(relation.getTargetMappingFields()).map(s -> middleEntitySimpleName + ".Fields." + s + "").collect(Collectors.joining(", ")));
            code.append("}");
        }

        code.append(")");

        return code.toString();
    }
}
