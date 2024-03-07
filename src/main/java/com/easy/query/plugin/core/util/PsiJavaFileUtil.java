package com.easy.query.plugin.core.util;

import com.easy.query.plugin.core.EasyQueryDocumentChangeHandler;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.AnnotationTargetsSearch;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

public class PsiJavaFileUtil {
    public static Set<String> getImportSet(PsiJavaFile psiJavaFile) {
        PsiImportList importList = psiJavaFile.getImportList();
        if (Objects.isNull(importList)) {
            return new HashSet<>();
        }

        return Arrays.stream(Objects.requireNonNull(importList).getAllImportStatements())
                .map(PsiImportStatementBase::getText)
                .collect(Collectors.toSet());
    }

    /**
     * 得到限定名称导入map
     *
     * @param psiJavaFile psi java文件
     * @return {@code Map<String, String>}
     */
    public static Map<String, String> getQualifiedNameImportMap(PsiJavaFile psiJavaFile) {
        Map<String, String> map = new HashMap<>();
        getImportSet(psiJavaFile)
                .forEach(el -> {
                    String qualifiedName = el.replace("import", "" ).replace(";", "" ).trim();
                    map.put(StrUtil.subAfter(qualifiedName, ".", true), qualifiedName);
                });
        return map;
    }

    public static Set<String> getQualifiedNameImportSet(PsiJavaFile psiJavaFile) {

        return new HashSet<>(getQualifiedNameImportMap(psiJavaFile).values());
    }

    /**
     * 获取子类
     *
     * @param qualifiedName 限定名
     * @param searchScope   搜索范围
     * @return {@code Collection<PsiClass>}
     */
    public static Collection<PsiClass> getSonPsiClass(Project project,String qualifiedName, SearchScope searchScope) {
        PsiClass clazz = getPsiClass(project,qualifiedName);
        if(clazz==null){
            return Collections.emptyList();
        }
        return ClassInheritorsSearch.search(clazz, searchScope, true).findAll();
    }

    public static Collection<PsiClass> getAnnotationPsiClass(Project project,String qualifiedName) {
        PsiClass psiClass = PsiJavaFileUtil.getPsiClass(project,qualifiedName);
        if(psiClass==null){
            return Collections.emptyList();
        }
        return AnnotationTargetsSearch.search(psiClass).findAll()
                .stream()
                .filter(el -> el instanceof PsiClass)
                .map(el -> (PsiClass) el)
                .collect(Collectors.toList()
                );
    }

    public static Collection<PsiClass> getAllSonPsiClass(Project project,String qualifiedName) {
        PsiClass clazz = getPsiClass(project,qualifiedName);
        return ClassInheritorsSearch.search(clazz, GlobalSearchScope.allScope(project), true).findAll();
    }

    public static Collection<PsiClass> getProjectSonPsiClass(Project project,String qualifiedName) {
        PsiClass clazz = getPsiClass(project,qualifiedName);
        return ClassInheritorsSearch.search(clazz, GlobalSearchScope.projectScope(project), true).findAll();
    }

    public static PsiClass getPsiClass(Project project,String qualifiedName) {
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        return psiFacade.findClass(qualifiedName, GlobalSearchScope.allScope(project));
    }

    public static PsiClass getPsiClass(Project project,String qualifiedName, GlobalSearchScope scope) {
        JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(project);
        return psiFacade.findClass(qualifiedName, scope);
    }

    public static PsiImportStatement createImportStatement(Project project,PsiClass psiClass) {
        PsiElementFactory instance = PsiElementFactory.getInstance(project);
        return instance.createImportStatement(psiClass);
    }

//    public static String getGenericity(PsiClass psiClass) {
//        String text = psiClass.getText();
//        String genericity = StrUtil.subBetween(text, "<", ">");
//        if (genericity.contains(",")) {
//            genericity = StrUtil.subAfter(genericity, ",", true).trim();
//        }
//        return genericity;
//    }

    /**
     * 获得包名
     *
     * @param psiClass psi类
     * @return {@code String}
     */
    public static String getPackageName(PsiClass psiClass) {
        PsiJavaFile psiJavaFile = (PsiJavaFile) psiClass.getContainingFile();
        return psiJavaFile.getPackageName();
    }

    /**
     * 生成 apt 文件
     */
    public static void createAptFile(Project project) {
        Collection<PsiClass> annotationPsiProxyClass = PsiJavaFileUtil.getAnnotationPsiClass(project,"com.easy.query.core.annotation.EntityProxy" );
        Collection<PsiClass> annotationPsiFileProxyClass = PsiJavaFileUtil.getAnnotationPsiClass(project,"com.easy.query.core.annotation.EntityFileProxy" );
        ArrayList<PsiClass> annotationPsiClass = new ArrayList<>();
        annotationPsiClass.addAll(annotationPsiProxyClass);
        annotationPsiClass.addAll(annotationPsiFileProxyClass);
        List<VirtualFile> virtualFiles = annotationPsiClass.stream()
                .map(el -> {
                    VirtualFile virtualFile = el.getContainingFile()
                            .getVirtualFile();
                    virtualFile.putUserData(EasyQueryDocumentChangeHandler.CHANGE, true);
                    return virtualFile;
                })
                .collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(virtualFiles)){
            EasyQueryDocumentChangeHandler.createAptFile(virtualFiles,project,true);
        }
    }
    public static void createAptCurrentFile(VirtualFile virtualFile,Project project) {
        if(virtualFile!=null){
            virtualFile.putUserData(EasyQueryDocumentChangeHandler.CHANGE, true);
            EasyQueryDocumentChangeHandler.createAptFile(Collections.singletonList(virtualFile),project,false);
        }
    }

}
