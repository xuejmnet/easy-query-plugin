package com.easy.query.plugin.core;

import com.easy.query.plugin.core.config.CustomConfig;
import com.easy.query.plugin.core.entity.AptFileCompiler;
import com.easy.query.plugin.core.entity.AptPropertyInfo;
import com.easy.query.plugin.core.entity.AptSelectPropertyInfo;
import com.easy.query.plugin.core.entity.AptSelectorInfo;
import com.easy.query.plugin.core.entity.AptValueObjectInfo;
import com.easy.query.plugin.core.entity.GenerateFileEntry;
import com.easy.query.plugin.core.entity.PropertyColumn;
import com.easy.query.plugin.core.enums.BeanPropTypeEnum;
import com.easy.query.plugin.core.enums.FileTypeEnum;
import com.easy.query.plugin.core.util.BooleanUtil;
import com.easy.query.plugin.core.util.ClassUtil;
import com.easy.query.plugin.core.util.KtFileUtil;
import com.easy.query.plugin.core.util.MyModuleUtil;
import com.easy.query.plugin.core.util.ObjectUtil;
import com.easy.query.plugin.core.util.ProjectUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.easy.query.plugin.core.util.PsiUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.easy.query.plugin.core.util.VelocityUtils;
import com.easy.query.plugin.core.util.VirtualFileUtils;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.event.EditorMouseEvent;
import com.intellij.openapi.editor.event.EditorMouseListener;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.messages.MessageBus;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.velocity.VelocityContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;

import javax.lang.model.element.TypeElement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * @author bigtian
 */
public class EasyQueryDocumentChangeHandler implements DocumentListener, EditorFactoryListener, Disposable {
    private static final Logger log = Logger.getInstance(EasyQueryDocumentChangeHandler.class);
    public static final Key<Boolean> CHANGE = Key.create("change");
    private static final Key<Boolean> LISTENER = Key.create("listener");

//    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static void createAptFile(List<VirtualFile> virtualFiles, Project project, boolean allCompileFrom) {
//        Project project = ProjectUtils.getCurrentProject();
        virtualFiles = virtualFiles.stream()
                .filter(oldFile -> {
                    if (Objects.isNull(oldFile)) {
                        return false;
                    }
                    Boolean userData = oldFile.getUserData(CHANGE);
                    return !(Objects.isNull(oldFile) || (!oldFile.getName().endsWith(".java") && !oldFile.getName().endsWith(".kt")) || !oldFile.isWritable()) && BooleanUtil.isTrue(userData) && checkFile(project, oldFile);
                }).collect(Collectors.toList());
        Map<PsiDirectory, List<GenerateFileEntry>> psiDirectoryMap = new HashMap<>();
        try {

            // 检查索引是否已准备好
            for (VirtualFile oldFile : virtualFiles) {
                Module moduleForFile = com.intellij.openapi.module.ModuleUtil.findModuleForFile(oldFile, project);
                if (moduleForFile == null) {
                    log.warn("moduleForFile is null," + oldFile.getName());
                    continue;
                }
                CustomConfig config = MyModuleUtil.moduleConfig(moduleForFile);
                if (!ObjectUtil.defaultIfNull(config.isEnable(), true)) {
                    continue;
                }
                String moduleDirPath = MyModuleUtil.getPath(moduleForFile);
                PsiClassOwner psiFile = (PsiClassOwner) VirtualFileUtils.getPsiFile(project, oldFile);
                PsiClass[] classes = psiFile.getClasses();
                if (classes.length == 0) {
                    log.warn("psiJavaFile.getText():[" + psiFile.getText() + "],psiJavaFile.getClasses() is empty");
                    continue;
                }
                PsiClass psiClass = psiFile.getClasses()[0];
                PsiAnnotation entityFileProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityFileProxy");

                PsiAnnotation entityProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityProxy");
                if (entityProxy == null && entityFileProxy == null) {
                    log.warn("annotation [EntityProxy] is null and [EntityFileProxy] is null");
                    continue;
                }
                String easyQueryVersion = getEasyQueryVersion(entityProxy, entityFileProxy);
                if (Objects.equals("1", easyQueryVersion)) {
                    APTVersion1.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
                } else {
                    String easyQueryRevision = getEasyQueryRevision(entityProxy, entityFileProxy);
                    if (Objects.equals("", easyQueryRevision)) {
                        APTVersion2.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
                    } else if (Objects.equals("1", easyQueryRevision)){
                        APTVersion2_1.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
                    }else {
                        APTVersion2_2.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
                    }

                }
            }
            // 等待索引准备好
            DumbService.getInstance(project).runWhenSmart(() -> {
                // 执行需要索引的操作
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    for (Map.Entry<PsiDirectory, List<GenerateFileEntry>> entry : psiDirectoryMap.entrySet()) {
                        PsiDirectory psiDirectory = entry.getKey();
                        List<GenerateFileEntry> psiFiles = entry.getValue();
                        for (GenerateFileEntry generateFile : psiFiles) {
                            PsiFile tmpFile = generateFile.getPsiFile();
                            PsiFile file = psiDirectory.findFile(tmpFile.getName());
                            if (ObjectUtil.isNotNull(file)) {
                                //允许覆盖
                                if (generateFile.isOverrideWrite()) {
                                    String text = tmpFile.getText();
                                    Document document = file.getViewProvider().getDocument();
                                    if (!Objects.equals(document.getText(), text)) {
                                        document.setText(text);
                                    }
                                }
                            } else {
                                psiDirectory.add(tmpFile);
                            }
                        }
                    }
                });
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getEasyQueryVersion(PsiAnnotation entityProxy, PsiAnnotation entityFileProxy) {
        if (entityProxy != null) {
            return PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "version", "1");
        }
        if (entityFileProxy != null) {
            return PsiUtil.getPsiAnnotationValueIfEmpty(entityFileProxy, "version", "1");
        }
        return "1";
    }

    private static String getEasyQueryRevision(PsiAnnotation entityProxy, PsiAnnotation entityFileProxy) {
        if (entityProxy != null) {
            return PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "revision", "");
        }
        if (entityFileProxy != null) {
            return PsiUtil.getPsiAnnotationValueIfEmpty(entityFileProxy, "revision", "");
        }
        return "";
    }


    public EasyQueryDocumentChangeHandler() {
        super();

        try {
            // 所有的文档监听
//            EditorFactory.getInstance().getEventMulticaster().addDocumentListener(this, this);
            //获取已打开的编辑器
            Editor[] allEditors = EditorFactory.getInstance().getAllEditors();
            for (Editor editor : allEditors) {
                addEditorListener(editor);
            }
//            Project project = ProjectUtils.getCurrentProject();
//            if (Objects.isNull(project)) {
//                return;
//            }
////            Deprecated
////            Use com.intellij.util.messages.MessageBus instead: see FileEditorManagerListener.FILE_EDITOR_MANAGER
//
////            FileEditorManager.getInstance(project).addFileEditorManagerListener(this);
//            MessageBus messageBus = project.getMessageBus();
//            messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
        } catch (Exception e) {
            log.error("初始化EasyQueryDocumentChangeHandler出错:" + e.getMessage(), e);
        }

    }


    private void addEditorListener(Editor editor) {
        Document document = editor.getDocument();
        if (BooleanUtils.isNotTrue(document.getUserData(LISTENER))) {
            editor.addEditorMouseListener(new EditorMouseListener() {
                @Override
                public void mouseExited(@NotNull EditorMouseEvent event) {
                    createAptFile(Collections.singletonList(VirtualFileUtils.getVirtualFile(editor.getDocument())), event.getEditor().getProject(), false);
                }
            });
            document.putUserData(LISTENER, true);
            document.addDocumentListener(this);
        }
    }

    private void removeEditorListener(Editor editor) {
        Document document = editor.getDocument();
        if (BooleanUtils.isTrue(document.getUserData(LISTENER))) {
            document.putUserData(LISTENER, false);
            document.removeDocumentListener(this);
        }
    }

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {

        EditorFactoryListener.super.editorCreated(event);
        Editor editor = event.getEditor();
        addEditorListener(editor);
//        ProjectUtils.setCurrentProject(editor.getProject());
    }

    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        removeEditorListener(editor);
    }


    private static boolean checkFile(Project project, VirtualFile currentFile) {
        if (Objects.isNull(currentFile) || currentFile instanceof LightVirtualFile) {
            return false;
        }
        PsiManager psiManager = PsiManager.getInstance(project);
        PsiFile psiFile = psiManager.findFile(currentFile);
        // 支持java和kotlin
        if (!(psiFile instanceof PsiJavaFile) && !(psiFile instanceof KtFile)) {
            return false;
        }
        Set<String> importSet = new HashSet<>();
        if (psiFile instanceof KtFile) {
            KtFile ktFile = (KtFile) psiFile;
            importSet = KtFileUtil.getImportSet(ktFile);
        }
        if (psiFile instanceof PsiJavaFile) {
            PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
            importSet = PsiJavaFileUtil.getQualifiedNameImportSet(psiJavaFile);
        }
        return importSet.contains("com.easy.query.core.annotation.EntityProxy") || importSet.contains("com.easy.query.core.annotation.*") || importSet.contains("com.easy.query.core.annotation.EntityFileProxy");
    }

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        Document document = event.getDocument();
        CharSequence newFragment = event.getNewFragment();
        if ((StrUtil.isBlank(newFragment) && StrUtil.isBlank(event.getOldFragment()))) {
            return;
        }
        VirtualFile currentFile = VirtualFileUtils.getVirtualFile(document);
        if (Objects.nonNull(currentFile)) {
            currentFile.putUserData(CHANGE, true);
        }
    }


    @Override
    public void dispose() {
        Disposer.dispose(this);
    }
}
