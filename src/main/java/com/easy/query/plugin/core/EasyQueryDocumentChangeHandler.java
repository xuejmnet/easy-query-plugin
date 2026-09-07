package com.easy.query.plugin.core;

import com.easy.query.plugin.core.config.CustomConfig;
import com.easy.query.plugin.core.entity.GenerateFileEntry;
import com.easy.query.plugin.core.util.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.testFramework.LightVirtualFile;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


/**
 * @author bigtian
 */
public class EasyQueryDocumentChangeHandler implements DocumentListener, EditorFactoryListener, Disposable {
    public static final Key<Boolean> CHANGE = Key.create("change");
    private static final Logger log = Logger.getInstance(EasyQueryDocumentChangeHandler.class);
    private static final Key<Boolean> LISTENER = Key.create("listener");
    /**
     * 编译全部进行中标志：期间忽略新的编译全部/单文件生成触发，读+写全部结束后释放
     */
    private static final AtomicBoolean COMPILING = new AtomicBoolean(false);
    /**
     * 插件自身写命令执行期间为 true（仅 EDT 访问）。
     * 期间产生的文档变更不算用户修改，documentChanged 不标记 CHANGE，
     * 避免生成文件在编辑器中打开时 mouseExited 触发多余的重新生成。
     */
    private static boolean selfWriting = false;

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

    public static boolean tryAcquireCompileLock() {
        return COMPILING.compareAndSet(false, true);
    }

    public static void releaseCompileLock() {
        COMPILING.set(false);
    }

//    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public static boolean isCompiling() {
        return COMPILING.get();
    }

    public static void createAptFile0(PsiClassOwner psiFile, PsiClass psiClass, Project project, Map<String, List<GenerateFileEntry>> psiDirectoryMap, String moduleDirPath, CustomConfig config,
                                      Module moduleForFile, VirtualFile oldFile, boolean allCompileFrom) {
        PsiAnnotation entityFileProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityFileProxy");

        PsiAnnotation entityProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityProxy");
        if (entityProxy == null && entityFileProxy == null) {
            log.warn("annotation [EntityProxy] is null and [EntityFileProxy] is null");
            return;
        }
        String easyQueryVersion = getEasyQueryVersion(entityProxy, entityFileProxy);
        if (Objects.equals("1", easyQueryVersion)) {
            APTVersion1.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
        } else {
            String easyQueryRevision = getEasyQueryRevision(entityProxy, entityFileProxy);
            if (Objects.equals("", easyQueryRevision)) {
                APTVersion2.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
            } else if (Objects.equals("1", easyQueryRevision)) {
                APTVersion2_1.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
            } else if (Objects.equals("2", easyQueryRevision)) {
                APTVersion2_2.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
            } else if (Objects.equals("5", easyQueryRevision)) {
                APTVersion2_5.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
            } else if (Objects.equals("6", easyQueryRevision)) {
                APTVersion2_6.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
            } else if (Objects.equals("7", easyQueryRevision)) {
                APTVersion2_7.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
            } else if (Objects.equals("8", easyQueryRevision)) {
                APTVersion2_8.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
            } else {
                APTVersion2_9.generateApt(project, psiDirectoryMap, entityFileProxy, entityProxy, psiFile, moduleDirPath, config, moduleForFile, psiClass, oldFile, allCompileFrom);
            }

        }
    }

    public static void createAptFile(List<VirtualFile> virtualFiles, Project project, boolean allCompileFrom) {
        // 编译全部进行中时忽略新的生成触发
        if (isCompiling()) {
            log.info("EasyQuery 正在编译中，忽略本次生成触发");
            return;
        }
        // 检查索引是否已准备好
        if (BooleanUtils.isTrue(DumbService.getInstance(project).isDumb())) {
            log.info("索引未准备好，将在索引完成后重新执行");
            // 复制列表，避免在lambda中引用非final变量
            final List<VirtualFile> finalVirtualFiles = new ArrayList<>(virtualFiles);
            DumbService.getInstance(project).runWhenSmart(() -> {
                createAptFile(finalVirtualFiles, project, allCompileFrom);
            });
            return;
        }
        // 生成阶段放后台读操作（含模板渲染的 PSI 解析），仅写入回到 EDT，
        // 避免在 EDT 上累积解析/索引等待造成界面冻结
        new Task.Backgroundable(project, "EasyQuery: 生成 APT 文件", true) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                Map<String, List<GenerateFileEntry>> psiDirectoryMap =
                        DumbService.getInstance(project).runReadActionInSmartMode(
                                () -> generateAptFiles(virtualFiles, project, allCompileFrom, indicator));
                if (psiDirectoryMap.isEmpty() || project.isDisposed()) {
                    return;
                }
                ApplicationManager.getApplication().invokeLater(
                        () -> writeAptFiles(project, psiDirectoryMap, indicator),
                        ModalityState.NON_MODAL);
            }
        }.queue();
    }

    /**
     * 生成阶段（纯只读，在后台线程的读操作中执行）：过滤待生成文件并构建 目录路径->生成条目 映射，
     * 不执行任何写入；目录解析/创建延迟到写入阶段（EDT）。
     */
    public static Map<String, List<GenerateFileEntry>> generateAptFiles(List<VirtualFile> virtualFiles, Project project, boolean allCompileFrom, ProgressIndicator indicator) {
        Map<String, List<GenerateFileEntry>> psiDirectoryMap = new HashMap<>();
        try {
            List<VirtualFile> candidates = virtualFiles.stream()
                    .filter(oldFile -> {
                        if (Objects.isNull(oldFile)) {
                            return false;
                        }
                        Boolean userData = oldFile.getUserData(CHANGE);
                        return !(Objects.isNull(oldFile) || (!oldFile.getName().endsWith(".java") && !oldFile.getName().endsWith(".kt")) || !oldFile.isWritable()) && BooleanUtil.isTrue(userData) && checkFile(project, oldFile);
                    }).collect(Collectors.toList());

            for (VirtualFile oldFile : candidates) {
                if (indicator != null) {
                    indicator.checkCanceled();
                }
                Module moduleForFile = com.intellij.openapi.module.ModuleUtil.findModuleForFile(oldFile, project);
                if (moduleForFile == null) {
                    log.warn("moduleForFile is null," + oldFile.getName());
                    continue;
                }
                CustomConfig config = MyModuleUtil.moduleConfig(moduleForFile);
                if (!ObjectUtil.defaultIfNull(config.getEnable(), true)) {
                    continue;
                }
                String moduleDirPath = MyModuleUtil.getPath(moduleForFile);
                PsiClassOwner psiFile = (PsiClassOwner) VirtualFileUtils.getPsiFile(project, oldFile);
                PsiClass[] classes = psiFile.getClasses();
                if (classes.length == 0) {
                    log.warn("psiJavaFile.getText():[" + psiFile.getText() + "],psiJavaFile.getClasses() is empty");
                    continue;
                }
                for (PsiClass mainClass : classes) {
                    createAptFile0(psiFile, mainClass, project, psiDirectoryMap, moduleDirPath, config, moduleForFile, oldFile, allCompileFrom);
                    for (PsiClass innerClass : mainClass.getInnerClasses()) {
                        if (innerClass.hasModifierProperty(PsiModifier.PUBLIC) && innerClass.hasModifierProperty(PsiModifier.STATIC)) {
                            createAptFile0(psiFile, innerClass, project, psiDirectoryMap, moduleDirPath, config, moduleForFile, oldFile, allCompileFrom);
                        }
                    }
                }
            }
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成APT文件出错:" + e.getMessage(), e);
        }
        return psiDirectoryMap;
    }

    /**
     * 写入阶段（须在 EDT 执行）：逐条目独立写命令落盘，命令间允许取消。
     * 目录按生成阶段记录的路径字符串解析，缺失时创建。
     *
     * @return 是否完整写入（取消时抛出取消异常，不会返回 true）
     */
    public static boolean writeAptFiles(Project project, Map<String, List<GenerateFileEntry>> psiDirectoryMap, ProgressIndicator indicator) {
        for (Map.Entry<String, List<GenerateFileEntry>> entry : psiDirectoryMap.entrySet()) {
            String dirPath = entry.getKey();
            for (GenerateFileEntry generateFile : entry.getValue()) {
                if (indicator != null) {
                    indicator.checkCanceled();
                } else {
                    ProgressManager.checkCanceled();
                }
                writeGeneratedFile(project, dirPath, generateFile);
            }
        }
        return true;
    }

    /**
     * 写入阶段（编译全部专用，须在 EDT 调用）：逐文件独立派发写入。
     * 每个文件一次 invokeLater 派发，派发之间 EDT 可处理用户事件，避免整批写入在单次派发中长时间阻塞界面；
     * 成功完成回调 onSuccess（任意终止路径——成功/取消/异常/项目释放——都会执行 onFinished 且仅执行一次）。
     */
    public static void writeAptFilesDispatched(Project project, Map<String, List<GenerateFileEntry>> psiDirectoryMap,
                                               @org.jetbrains.annotations.Nullable ProgressIndicator indicator,
                                               @org.jetbrains.annotations.NotNull Runnable onSuccess,
                                               @org.jetbrains.annotations.NotNull Runnable onFinished) {
        List<Map.Entry<String, GenerateFileEntry>> units = new ArrayList<>();
        psiDirectoryMap.forEach((dirPath, files) -> files.forEach(generateFile -> units.add(new HashMap.SimpleEntry<>(dirPath, generateFile))));
        dispatchNextWrite(project, units, 0, indicator, onSuccess, onFinished);
    }

    private static void dispatchNextWrite(Project project, List<Map.Entry<String, GenerateFileEntry>> units, int index,
                                          ProgressIndicator indicator, Runnable onSuccess, Runnable onFinished) {
        boolean scheduledNext = false;
        try {
            if (project.isDisposed()) {
                return;
            }
            if (indicator != null) {
                indicator.checkCanceled();
            } else {
                ProgressManager.checkCanceled();
            }
            if (index >= units.size()) {
                onSuccess.run();
                return;
            }
            Map.Entry<String, GenerateFileEntry> unit = units.get(index);
            writeGeneratedFile(project, unit.getKey(), unit.getValue());
            scheduledNext = true;
            ApplicationManager.getApplication().invokeLater(
                    () -> dispatchNextWrite(project, units, index + 1, indicator, onSuccess, onFinished),
                    ModalityState.NON_MODAL);
        } finally {
            if (!scheduledNext) {
                onFinished.run();
            }
        }
    }

    /**
     * 落盘单个生成文件：独立写命令（共享 undo 组 id，可按文件撤销），
     * 覆盖与新增两条路径统一先格式化再写入，保证输出格式一致。
     * 目录按生成阶段记录的路径字符串在写入线程（EDT）解析，缺失时创建。
     */
    private static void writeGeneratedFile(Project project, String dirPath, GenerateFileEntry generateFile) {
        WriteCommandAction.runWriteCommandAction(project, "EasyQuery 生成 APT 文件", "easy-query.apt.generate", () -> {
            selfWriting = true;
            try {
                PsiDirectory psiDirectory = VirtualFileUtils.ensurePsiDirectory(project, dirPath);
                if (psiDirectory == null) {
                    log.warn("ensurePsiDirectory is null, path:" + dirPath);
                    return;
                }
                PsiFile tmpFile = generateFile.getPsiFile();
                CodeStyleManager.getInstance(project).reformat(tmpFile);
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
            } finally {
                selfWriting = false;
            }
        });
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

    private static boolean checkFile(Project project, VirtualFile currentFile) {
        if (Objects.isNull(currentFile) || currentFile instanceof LightVirtualFile) {
            return false;
        }
        // 增加判断如果当前文件不合法, 则不触发
        if (!currentFile.isValid()) {
            return false;
        }
        // 检查索引是否准备好
        if (DumbService.getInstance(project).isDumb()) {
            return false; // 在索引未准备好时返回false，稍后会重试
        }

        // 使用 ReadAction 包裹 PSI 访问，以兼容 IntelliJ IDEA 2026.1 的线程访问限制
        return ReadAction.compute(() -> {
            PsiManager psiManager = PsiManager.getInstance(project);
            PsiFile psiFile = psiManager.findFile(currentFile);
            // 支持java和kotlin
            if (!(psiFile instanceof PsiJavaFile) && !(psiFile instanceof KtFile)) {
                return false;
            }
            String text = psiFile.getText();
            //        Set<String> importSet = new HashSet<>();
            //        if (psiFile instanceof KtFile) {
            //            KtFile ktFile = (KtFile) psiFile;
            //            importSet = KtFileUtil.getImportSet(ktFile);
            //        }
            //        if (psiFile instanceof PsiJavaFile) {
            //            PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
            //            importSet = PsiJavaFileUtil.getQualifiedNameImportSet(psiJavaFile);
            //        }
            return text.contains("com.easy.query.core.annotation.EntityProxy") || text.contains("com.easy.query.core.annotation.*") || text.contains("com.easy.query.core.annotation.EntityFileProxy");
        });
    }

    private void addEditorListener(Editor editor) {
        Document document = editor.getDocument();
        if (BooleanUtils.isNotTrue(document.getUserData(LISTENER))) {
            editor.addEditorMouseListener(new EditorMouseListener() {
                @Override
                public void mouseExited(@NotNull EditorMouseEvent event) {
                    Project project = event.getEditor().getProject();
                    if (Objects.isNull(project)) {
                        return;
                    }
                    createAptFile(Collections.singletonList(VirtualFileUtils.getVirtualFile(editor.getDocument())), project, false);
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

    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        // 插件自身写命令引发的变更不视为用户修改，不标记 CHANGE
        if (selfWriting) {
            return;
        }
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
