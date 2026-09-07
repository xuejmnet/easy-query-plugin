package com.easy.query.plugin.core.util;

import com.easy.query.plugin.core.EasyQueryDocumentChangeHandler;
import com.easy.query.plugin.core.entity.GenerateFileEntry;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.AnnotationTargetsSearch;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import org.jetbrains.annotations.NotNull;

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
     * 编译全部：非模态后台任务中完成搜索与生成（均为后台读操作，模板渲染的 PSI 解析亦在后台执行，不阻塞 EDT），
     * 写回在 EDT 逐文件执行（含目录解析/创建）。全程可取消；收尾动作仅在写入完整完成（未被取消）后执行。
     *
     * @param onSuccess 任务成功完成后的收尾动作，可为 null
     */
    public static void createAptFile(Project project, Runnable onSuccess) {
        if (!EasyQueryDocumentChangeHandler.tryAcquireCompileLock()) {
            NotificationUtils.notifyWarning("EasyQuery 正在编译中，已忽略本次触发", "EasyQuery", project);
            return;
        }
        new Task.Backgroundable(project, "EasyQuery: 编译全部", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                boolean writeScheduled = false;
                try {
                    // 搜索阶段：后台读操作，dumb 模式下等待 smart 后执行（保留原有等待语义）
                    List<VirtualFile> virtualFiles = DumbService.getInstance(project).runReadActionInSmartMode(() -> {
                        indicator.checkCanceled();
                        return collectEntityFiles(project, indicator);
                    });
                    if (virtualFiles.isEmpty()) {
                        return;
                    }
                    // 生成阶段：纯只读，仍在后台读操作中执行（含模板渲染），不阻塞 EDT
                    Map<String, List<GenerateFileEntry>> psiDirectoryMap =
                            DumbService.getInstance(project).runReadActionInSmartMode(
                                    () -> EasyQueryDocumentChangeHandler.generateAptFiles(virtualFiles, project, true, indicator));
                    if (psiDirectoryMap.isEmpty() || project.isDisposed()) {
                        return;
                    }
                    writeScheduled = true;
                    // 写入阶段回到 EDT：逐文件独立派发（派发间 EDT 可响应事件与取消），
                    // 完成后释放编译锁并执行收尾，取消时不执行收尾
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (project.isDisposed()) {
                            EasyQueryDocumentChangeHandler.releaseCompileLock();
                            return;
                        }
                        EasyQueryDocumentChangeHandler.writeAptFilesDispatched(project, psiDirectoryMap, indicator,
                                () -> {
                                    if (onSuccess != null) {
                                        onSuccess.run();
                                    }
                                },
                                EasyQueryDocumentChangeHandler::releaseCompileLock);
                    }, ModalityState.NON_MODAL);
                } finally {
                    // 搜索/生成失败、取消或无实体时直接释放锁（写入已调度则由 EDT 回调释放）
                    if (!writeScheduled) {
                        EasyQueryDocumentChangeHandler.releaseCompileLock();
                    }
                }
            }
        }.queue();
    }

    /**
     * 搜索项目中所有标注 EntityProxy/EntityFileProxy 的实体文件并标记为待生成。
     */
    private static List<VirtualFile> collectEntityFiles(Project project, ProgressIndicator indicator) {
        Collection<PsiClass> annotationPsiProxyClass = PsiJavaFileUtil.getAnnotationPsiClass(project,"com.easy.query.core.annotation.EntityProxy" );
        Collection<PsiClass> annotationPsiFileProxyClass = PsiJavaFileUtil.getAnnotationPsiClass(project,"com.easy.query.core.annotation.EntityFileProxy" );
        ArrayList<PsiClass> annotationPsiClass = new ArrayList<>();
        annotationPsiClass.addAll(annotationPsiProxyClass);
        annotationPsiClass.addAll(annotationPsiFileProxyClass);
        List<VirtualFile> virtualFiles = annotationPsiClass.stream()
                .map(el -> {
                    indicator.checkCanceled();
                    VirtualFile virtualFile = el.getContainingFile()
                            .getVirtualFile();
                    virtualFile.putUserData(EasyQueryDocumentChangeHandler.CHANGE, true);
                    return virtualFile;
                })
                .collect(Collectors.toList());
        return virtualFiles;
    }
    public static void createAptCurrentFile(VirtualFile virtualFile,Project project) {
        if(virtualFile!=null){
            virtualFile.putUserData(EasyQueryDocumentChangeHandler.CHANGE, true);
            EasyQueryDocumentChangeHandler.createAptFile(Collections.singletonList(virtualFile),project,false);
        }
    }


}
