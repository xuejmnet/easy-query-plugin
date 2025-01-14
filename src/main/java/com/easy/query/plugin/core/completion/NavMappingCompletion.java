package com.easy.query.plugin.core.completion;

import com.easy.query.plugin.action.navgen.NavMappingGUI;
import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.PsiJavaClassUtil;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
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
                            Consumer<String> callback = (generatedCode) -> {
                                ApplicationManager.getApplication().runWriteAction(() -> {
                                    context.getDocument().replaceString(context.getStartOffset() - 1, // 需要往后回退一位, 将@ 符号也一起替换掉
                                            context.getTailOffset(), generatedCode);
                                });
                            };

                            Project project = context.getProject();
                            SwingUtilities.invokeLater(() -> {


                                Collection<PsiClass> entityClasses = PsiJavaFileUtil.getAnnotationPsiClass(project,
                                        "com.easy.query.core.annotation.Table");

                                List<String> entityClassQualifiedNameList = entityClasses.stream().map(PsiClass::getQualifiedName).collect(Collectors.toList());


                                String[] entities = entityClassQualifiedNameList.toArray(new String[0]);

                                Map<String, String[]> entityAttributesMap = new HashMap<>();


                                for (PsiClass psiClass : entityClasses) {
                                    PsiField[] allFields = psiClass.getAllFields();
                                    entityAttributesMap.put(psiClass.getQualifiedName(), Arrays.stream(allFields)
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
}
