package com.easy.query.plugin.core;

import com.easy.query.plugin.core.config.CustomConfig;
import com.easy.query.plugin.core.entity.AptPropertyInfo;
import com.easy.query.plugin.core.enums.FileTypeEnum;
import com.easy.query.plugin.core.util.BooleanUtil;
import com.easy.query.plugin.core.util.ClassUtil;
import com.easy.query.plugin.core.util.KtFileUtil;
import com.easy.query.plugin.core.util.Modules;
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
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.testFramework.LightVirtualFile;
import org.apache.velocity.VelocityContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author bigtian
 */
public class EasyQueryDocumentChangeHandler implements DocumentListener, EditorFactoryListener, Disposable, FileEditorManagerListener {
    private static final Logger log = Logger.getInstance(EasyQueryDocumentChangeHandler.class);
    public static final Key<Boolean> CHANGE = Key.create("change");
    private static final Key<Boolean> LISTENER = Key.create("listener");
//    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent event) {
    }

    public static void createAptFile(List<VirtualFile> virtualFiles) {
        Project project = ProjectUtils.getCurrentProject();
        virtualFiles = virtualFiles.stream()
                .filter(oldFile -> {
                    if (Objects.isNull(oldFile)) {
                        return false;
                    }
                    Boolean userData = oldFile.getUserData(CHANGE);
                    return !(Objects.isNull(oldFile) || (!oldFile.getName().endsWith(".java")&&!oldFile.getName().endsWith(".kt")) || !oldFile.isWritable()) && BooleanUtil.isTrue(userData) && checkFile(oldFile);
                }).collect(Collectors.toList());
        Map<PsiDirectory, List<PsiFile>> psiDirectoryMap = new HashMap<>();
        try {

            // 检查索引是否已准备好
            for (VirtualFile oldFile : virtualFiles) {
                Module moduleForFile = ModuleUtil.findModuleForFile(oldFile, project);
                CustomConfig config = Modules.moduleConfig(moduleForFile);
                if (!ObjectUtil.defaultIfNull(config.isEnable(), true)) {
                    continue;
                }
                String moduleDirPath = Modules.getPath(moduleForFile);
                PsiClassOwner psiFile = (PsiClassOwner) VirtualFileUtils.getPsiFile(project, oldFile);
                FileTypeEnum fileType = PsiUtil.getFileType(psiFile);
                String path = moduleDirPath + CustomConfig.getConfig(config.getGenPath(),
                        fileType, Modules.isManvenProject(moduleForFile))
                        + psiFile.getPackageName().replace(".", "/") + "/proxy";

                PsiDirectory psiDirectory = VirtualFileUtils.createSubDirectory(moduleForFile, path);
                // 等待索引准备好
                DumbService.getInstance(project).runWhenSmart(() -> {
                    // 在智能模式下，执行需要等待索引准备好的操作，比如创建文件
                    // 创建文件等操作代码
                    oldFile.putUserData(CHANGE, false);
                    PsiClass[] classes = psiFile.getClasses();
                    if (classes.length == 0) {
                        log.warn("psiJavaFile.getText():[" + psiFile.getText() + "],psiJavaFile.getClasses() is empty");
                        return;
                    }
                    PsiClass psiClass = psiFile.getClasses()[0];
                    PsiAnnotation entityProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityProxy");
                    if (entityProxy == null) {
                        log.warn("annotation [EntityProxy] is null");
                        return;
                    }
                    String entityName = psiClass.getName();
                    String entityFullName = psiClass.getQualifiedName();
                    //获取对应的代理对象名称
                    String proxyEntityName = PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "value", psiClass.getName() + "Proxy");
                    //代理对象属性忽略
                    Set<String> proxyIgnoreProperties = PsiUtil.getPsiAnnotationValues(entityProxy, "ignoreProperties", new HashSet<>());
                    //是否是数据库对象
                    PsiAnnotation entityTable = psiClass.getAnnotation("com.easy.query.core.annotation.Table");
                    //获取对应的忽略属性
                    Set<String> tableAndProxyIgnoreProperties = PsiUtil.getPsiAnnotationValues(entityTable, "ignoreProperties", proxyIgnoreProperties);
                    //是否存在忽略属性
                    boolean hasIgnoreProperties = !tableAndProxyIgnoreProperties.isEmpty();
                    PsiField[] fields = psiClass.getAllFields();
                    List<AptPropertyInfo> aptProperties = new ArrayList<>(fields.length);
                    for (PsiField field : fields) {
                        String name = field.getName();
                        if (hasIgnoreProperties && tableAndProxyIgnoreProperties.contains(name)) {
                            continue;
                        }
                        boolean isBeanProperty = ClassUtil.hasGetterAndSetter(psiClass, name);
                        if (!isBeanProperty) {
                            continue;
                        }
                        String psiFieldPropertyType = PsiUtil.getPsiFieldPropertyType(field);
                        String psiFieldComment = PsiUtil.getPsiFieldClearComment(field);
                        aptProperties.add(new AptPropertyInfo(proxyEntityName, name, psiFieldPropertyType, psiFieldComment, entityName));
                    }

                    VelocityContext context = new VelocityContext();
                    context.put("entityName", entityName);
                    context.put("entityFullName", entityFullName);
                    context.put("proxyEntityName", proxyEntityName);
                    context.put("packageName", psiFile.getPackageName() + "." + ObjectUtil.defaultIfEmpty(config.getAllInTablesPackage(), "proxy"));
                    context.put("properties", aptProperties);
                    String suffix = Modules.getProjectTypeSuffix(moduleForFile);
                    PsiFile psiProxyFile = VelocityUtils.render(context, Template.getTemplateContent("AptTemplate" + suffix), proxyEntityName + suffix);
                    psiDirectoryMap.computeIfAbsent(psiDirectory, k -> new ArrayList<>()).add(psiProxyFile);
                });
            }
            // 等待索引准备好
            DumbService.getInstance(project).runWhenSmart(() -> {
                // 执行需要索引的操作
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    for (Map.Entry<PsiDirectory, List<PsiFile>> entry : psiDirectoryMap.entrySet()) {
                        PsiDirectory psiDirectory = entry.getKey();
                        List<PsiFile> psiFiles = entry.getValue();
                        for (PsiFile tmpFile : psiFiles) {
                            PsiFile file = psiDirectory.findFile(tmpFile.getName());
                            if (ObjectUtil.isNotNull(file)) {
                                file.getViewProvider().getDocument().setText(tmpFile.getText());
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


    public EasyQueryDocumentChangeHandler() {
        super();

        try {
            // 所有的文档监听
            EditorFactory.getInstance().getEventMulticaster().addDocumentListener(this, this);
            Document document;

            for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
                ProjectUtils.setCurrentProject(editor.getProject());
                document = editor.getDocument();
                document.putUserData(LISTENER, true);
                editor.addEditorMouseListener(new EditorMouseListener() {
                    @Override
                    public void mouseExited(@NotNull EditorMouseEvent event) {
                        createAptFile(Arrays.asList(VirtualFileUtils.getVirtualFile(editor.getDocument())));
                    }
                });
            }
            Project project = ProjectUtils.getCurrentProject();
            if (Objects.isNull(project)) {
                return;
            }
            FileEditorManager.getInstance(project).addFileEditorManagerListener(this);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    @Override
    public void editorReleased(@NotNull EditorFactoryEvent event) {
        Editor editor = event.getEditor();
        Document document = editor.getDocument();
        if (Boolean.TRUE.equals(document.getUserData(LISTENER))) {
            document.putUserData(LISTENER, false);
            document.removeDocumentListener(this);
        }
    }

    @Override
    public void editorCreated(@NotNull EditorFactoryEvent event) {
        try {
            EditorFactoryListener.super.editorCreated(event);
            Editor editor = event.getEditor();
            editor.addEditorMouseListener(new EditorMouseListener() {
                @Override
                public void mouseExited(@NotNull EditorMouseEvent event) {
                    createAptFile(Arrays.asList(VirtualFileUtils.getVirtualFile(editor.getDocument())));
                }
            });
            Document document = editor.getDocument();
            if (Boolean.TRUE.equals(document.getUserData(LISTENER))) {
                document.putUserData(LISTENER, true);
                document.addDocumentListener(this);
            }
            ProjectUtils.setCurrentProject(editor.getProject());
        } catch (Exception e) {

        }
    }


    private static boolean checkFile(VirtualFile currentFile) {
        if (Objects.isNull(currentFile) || currentFile instanceof LightVirtualFile) {
            return false;
        }
        PsiManager psiManager = PsiManager.getInstance(ProjectUtils.getCurrentProject());
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
        return importSet.contains("com.easy.query.core.annotation.EntityProxy");
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
