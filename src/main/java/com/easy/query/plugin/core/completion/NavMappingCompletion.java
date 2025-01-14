package com.easy.query.plugin.core.completion;

import com.easy.query.plugin.action.navgen.NavMappingGUI;
import com.easy.query.plugin.action.navgen.NavMappingRelation;
import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.PsiJavaClassUtil;
import com.easy.query.plugin.core.util.PsiJavaFieldUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

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
        String currentClassQualifiedName = currentPsiClass.getQualifiedName();


        //

        LookupElement lookupElementWithEq = PrioritizedLookupElement.withPriority(
                LookupElementBuilder.create("Nav EasyQuery @Navigate生成")
                        .withInsertHandler((context, item) -> {
                            Project project = context.getProject();
                            Consumer<NavMappingRelation> callback = (mappingRelation) -> {
                                ApplicationManager.getApplication().runWriteAction(() -> {


                                    String navText = generateNavPropertyCode(project, mappingRelation);
                                    context.getDocument().replaceString(context.getStartOffset() - 1, // 需要往后回退一位, 将@ 符号也一起替换掉
                                            context.getTailOffset(), navText);
                                });
                            };

                            SwingUtilities.invokeLater(() -> {


                                Collection<PsiClass> entityClasses = PsiJavaFileUtil.getAnnotationPsiClass(project,
                                        "com.easy.query.core.annotation.Table");

                                List<String> entityClassQualifiedNameList = entityClasses.stream().map(PsiClass::getQualifiedName).collect(Collectors.toList());


                                String[] entities = entityClassQualifiedNameList.toArray(new String[0]);

                                Map<String, String[]> entityAttributesMap = new HashMap<>();


                                for (PsiClass psiClass : entityClasses) {
                                    PsiField[] allFields = psiClass.getAllFields();
                                    entityAttributesMap.put(psiClass.getQualifiedName(), Arrays.stream(allFields)
                                            .filter(field -> !PsiJavaFieldUtil.ignoreField(field))
                                            .map(PsiField::getName).collect(Collectors.toList()).toArray(new String[0]));
                                }


                                NavMappingGUI gui = new NavMappingGUI(entities, currentClassQualifiedName, entityAttributesMap,
                                        callback);
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
