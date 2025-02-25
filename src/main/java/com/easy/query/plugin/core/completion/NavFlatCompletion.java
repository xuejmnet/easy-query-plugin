//package com.easy.query.plugin.core.completion;
//
//import cn.hutool.core.lang.Pair;
//import com.easy.query.plugin.action.navgen.NavMappingGUI;
//import com.easy.query.plugin.action.navgen.NavMappingRelation;
//import com.easy.query.plugin.core.icons.Icons;
//import com.easy.query.plugin.core.util.EasyQueryElementUtil;
//import com.easy.query.plugin.core.util.PsiCommentUtil;
//import com.easy.query.plugin.core.util.PsiJavaClassUtil;
//import com.easy.query.plugin.core.util.PsiJavaFieldUtil;
//import com.easy.query.plugin.core.util.PsiJavaFileUtil;
//import com.easy.query.plugin.core.util.StrUtil;
//import com.easy.query.plugin.windows.EntitySelectDialog;
//import com.intellij.codeInsight.completion.CompletionContributor;
//import com.intellij.codeInsight.completion.CompletionParameters;
//import com.intellij.codeInsight.completion.CompletionResultSet;
//import com.intellij.codeInsight.completion.CompletionType;
//import com.intellij.codeInsight.completion.PrioritizedLookupElement;
//import com.intellij.codeInsight.lookup.LookupElement;
//import com.intellij.codeInsight.lookup.LookupElementBuilder;
//import com.intellij.openapi.application.ApplicationManager;
//import com.intellij.openapi.project.Project;
//import com.intellij.psi.PsiAnnotation;
//import com.intellij.psi.PsiClass;
//import com.intellij.psi.PsiClassType;
//import com.intellij.psi.PsiElement;
//import com.intellij.psi.PsiField;
//import com.intellij.psi.PsiImportList;
//import com.intellij.psi.PsiJavaFile;
//import com.intellij.psi.PsiMethod;
//import com.intellij.psi.PsiType;
//import com.intellij.psi.util.PsiTreeUtil;
//import org.jetbrains.annotations.NotNull;
//
//import javax.swing.*;
//import java.util.Arrays;
//import java.util.Collection;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Objects;
//import java.util.function.Consumer;
//import java.util.stream.Collectors;
//
///**
// * 导航映射自动补全
// *
// * @author link2fun
// */
//public class NavFlatCompletion extends CompletionContributor {
//
//    @Override
//    public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
//
//        System.out.println("NavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletion");
//        System.out.println("NavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletion");
//        System.out.println("NavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletion");
//        System.out.println("NavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletion");
//        System.out.println("NavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletionNavFlatCompletion");
//        if (parameters.getCompletionType() != CompletionType.BASIC) {
//            return;
//        }
//
//        PsiElement position = parameters.getPosition();
//        PsiClass topLevelDtoClass = PsiTreeUtil.getTopmostParentOfType(position, PsiClass.class);
//        if (!PsiJavaClassUtil.isElementRelatedToClass(position)) {
//            // 只处理类下面的直接元素, 方法内的不处理
//            return;
//        }
//        PsiClass currentPsiClass = PsiTreeUtil.getParentOfType(position, PsiClass.class);
//        if (Objects.isNull(currentPsiClass)) {
//            return;
//        }
//        if (PsiTreeUtil.getParentOfType(position, PsiAnnotation.class) == null) {
//            // 当前输入的不是注解
//            return;
//        }
//        // 需要是在字段上的注解
//        if (!(PsiTreeUtil.getParentOfType(position, PsiField.class, PsiMethod.class, PsiClass.class) instanceof PsiField)) {
//            return;
//        }
//
////        PsiField currentField = PsiTreeUtil.getParentOfType(position, PsiField.class);
////        // 获取当前字段的类型
////        if (currentField == null) {
////            return;
////        }
////        PsiType currentFieldType = currentField.getType();
////        String currentFieldTypeQualifiedName = null;
////        if (currentFieldType instanceof PsiClassType) {
////            PsiClassType classType = (PsiClassType) currentFieldType;
////            PsiClass resolvedClass = classType.resolve();
////            if (resolvedClass != null) {
////                if (resolvedClass.getQualifiedName() != null && (resolvedClass.getQualifiedName().equals("java.util.List") || resolvedClass.getQualifiedName().equals("java.util.Set"))) {
////                    PsiType[] genericParameters = classType.getParameters();
////                    if (genericParameters.length > 0 && genericParameters[0] instanceof PsiClassType) {
////                        PsiClass genericClass = ((PsiClassType) genericParameters[0]).resolve();
////                        if (genericClass != null) {
////                            currentFieldTypeQualifiedName = genericClass.getQualifiedName();
////                        }
////                    }
////                } else {
////                    currentFieldTypeQualifiedName = resolvedClass.getQualifiedName();
////                }
////            }
////        }
////
////
////        String currentClassQualifiedName = currentPsiClass.getQualifiedName();
////        List<Pair<String, String>> currentClassFields = Arrays.stream(currentPsiClass.getAllFields())
////                .filter(field -> !PsiJavaFieldUtil.ignoreField(field))
////                .filter(field-> !EasyQueryElementUtil.hasNavigateAnnotation(field))
////                // 还需要过滤掉当前字段， 毕竟 当前字段是被关联的， 所以不能作为关联属性
////                .filter(field-> !StrUtil.equals(currentField.getName(), field.getName()))
////                .map(field -> Pair.of(field.getName(), PsiCommentUtil.getCommentDataStr(field.getDocComment())))
////                .collect(Collectors.toList());
//
//
//        PsiClass linkPsiClass = PsiJavaClassUtil.getLinkPsiClass(currentPsiClass);
//        boolean hasAnnoTable = PsiJavaClassUtil.hasAnnoTable(linkPsiClass);
//        if (!hasAnnoTable) {
//            return;
//        }
//        //
//
////        String targetEntityClassName = currentFieldTypeQualifiedName;
//        LookupElement lookupElementWithEq = PrioritizedLookupElement.withPriority(
//                LookupElementBuilder.create("NavigateFlat 生成")
//                        .withInsertHandler((context, item) -> {
//                            Project project = context.getProject();
//                            Consumer<NavMappingRelation> callback = (mappingRelation) -> {
//                                ApplicationManager.getApplication().runWriteAction(() -> {
//
//
//                                    String navText = generateNavPropertyCode(project, mappingRelation);
//                                    context.getDocument().replaceString(context.getStartOffset() - 1, // 需要往后回退一位, 将@ 符号也一起替换掉
//                                            context.getTailOffset(), navText);
//                                });
//                            };
//
////                            EntitySelectDialog entitySelectDialog = new EntitySelectDialog(dtoStructContext);
////                            SwingUtilities.invokeLater(() -> {
//////            entitySelectDialog.setVisible(true);
////                                // 跳过选择实体窗口, 直接进入字段选择
////                                entitySelectDialog.ok0(linkPsiClass.getQualifiedName());
////                                entitySelectDialog.dispose();
////                            });
//                        })
//                        .withIcon(Icons.EQ),
//                400d);
//        result.addElement(lookupElementWithEq);
//
//    }
//
//    public static String generateNavPropertyCode(Project project, NavMappingRelation relation) {
//        String sourceEntityFullName = relation.getSourceEntity();
//        String targetEntityFullName = relation.getTargetEntity();
//        String middleEntityFullName = relation.getMappingClass();
//
//        // 找到对应实体类
//        PsiClass sourceEntityClass = PsiJavaFileUtil.getPsiClass(project, sourceEntityFullName);
//        String sourceEntitySimpleName = sourceEntityClass.getName();
//        PsiJavaFile currentClassFile = (PsiJavaFile) sourceEntityClass.getContainingFile();
//
//        PsiImportList currentClassFileImportList = currentClassFile.getImportList();
//
//        PsiClass targetEntityClass = PsiJavaFileUtil.getPsiClass(project, targetEntityFullName);
//        String targetEntitySimpleName = targetEntityClass.getName();
//
//        // 添加TargetEntity的引入
////        currentClassFileImportList.add(PsiJavaFileUtil.createImportStatement(project, targetEntityClass));
//
//        String middleEntitySimpleName;
//        if (StrUtil.isNotBlank(middleEntityFullName)) {
//            PsiClass middleEntityClass = PsiJavaFileUtil.getPsiClass(project, middleEntityFullName);
//
//            // 当前类需要导入包
////            currentClassFileImportList.add(PsiJavaFileUtil.createImportStatement(project, middleEntityClass));
//
//            middleEntitySimpleName = middleEntityClass.getName();
//        } else {
//            middleEntitySimpleName = null;
//        }
//
//
//        StringBuilder code = new StringBuilder();
//
//        // 需要获取当前的 project
//
//
//        // 生成注解
//        code.append("@Navigate(");
//
//        // 添加关系类型
//        code.append("value = RelationTypeEnum.").append(relation.getRelationType());
//
//        // 添加 selfProperty
//        if (relation.getSourceFields() != null && relation.getSourceFields().length > 0) {
//            code.append(", selfProperty = {");
//            code.append(Arrays.stream(relation.getSourceFields()).map(s -> sourceEntitySimpleName + ".Fields." + s + "").collect(Collectors.joining(", ")));
//            code.append("}");
//        }
//
//        // 添加 selfMappingProperty
//        if (relation.getSelfMappingFields() != null && relation.getSelfMappingFields().length > 0) {
//            code.append(", selfMappingProperty = {");
//
//            code.append(Arrays.stream(relation.getSelfMappingFields()).map(s -> middleEntitySimpleName + ".Fields." + s + "").collect(Collectors.joining(", ")));
//            code.append("}");
//        }
//
//        // 添加 mappingClass
//        if (middleEntityFullName != null && !middleEntityFullName.isEmpty()) {
//            code.append(", mappingClass = ").append(middleEntitySimpleName).append(".class");
//        }
//
//        // 添加 targetProperty
//        if (relation.getTargetFields() != null && relation.getTargetFields().length > 0) {
//            code.append(", targetProperty = {");
//            code.append(Arrays.stream(relation.getTargetFields()).map(s -> targetEntitySimpleName + ".Fields." + s + "").collect(Collectors.joining(", ")));
//            code.append("}");
//        }
//
//        // 添加 targetMappingProperty
//        if (relation.getTargetMappingFields() != null && relation.getTargetMappingFields().length > 0) {
//            code.append(", targetMappingProperty = {");
//            code.append(Arrays.stream(relation.getTargetMappingFields()).map(s -> middleEntitySimpleName + ".Fields." + s + "").collect(Collectors.joining(", ")));
//            code.append("}");
//        }
//
//        code.append(")");
//
//        return code.toString();
//    }
//}
