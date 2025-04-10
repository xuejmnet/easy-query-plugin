package com.easy.query.plugin.action;

import com.easy.query.plugin.config.EasyQueryConfigManager;
import com.easy.query.plugin.core.inspection.EasyQueryFieldMissMatchInspection;
import com.easy.query.plugin.core.inspection.EasyQueryOrderByIncorrectInspection;
import com.easy.query.plugin.core.inspection.EasyQuerySetColumnsInspection;
import com.intellij.codeInspection.AbstractBaseJavaLocalInspectionTool;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.QuickFix;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.RegisterToolWindowTask;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.JBColor;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.Pair;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 在整个项目上运行 EasyQuery 检查的 Action
 * 并在工具窗口中显示结果。
 * @author link2fun
 */
public class RunEasyQueryInspectionAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(RunEasyQueryInspectionAction.class);
    private static final String TOOL_WINDOW_ID = "EasyQuery Issues";

    /**
     * 动态扫描并查找所有检查器
     */
    private List<AbstractBaseJavaLocalInspectionTool> findAllInspections() {
        List<AbstractBaseJavaLocalInspectionTool> result = new ArrayList<>();
        
        try (ScanResult scanResult = new ClassGraph()
                .enableClassInfo()
                .acceptPackages("com.easy.query.plugin")
                .scan()) {
            
            // 查找所有继承自 AbstractBaseJavaLocalInspectionTool 的类
            for (ClassInfo classInfo : scanResult.getSubclasses(AbstractBaseJavaLocalInspectionTool.class.getName())) {
                try {
                    // 确保类不是抽象的
                    if (!classInfo.isAbstract()) {
                        Class<?> clazz = classInfo.loadClass();
                        AbstractBaseJavaLocalInspectionTool inspection = 
                            (AbstractBaseJavaLocalInspectionTool) clazz.getDeclaredConstructor().newInstance();
                        result.add(inspection);
                    }
                } catch (Exception e) {
                    LOG.warn("实例化检查器失败: " + classInfo.getName(), e);
                }
            }
        } catch (Exception e) {
            LOG.error("扫描检查器时出错", e);
            // 如果动态扫描失败，回退到手动列表
            result.addAll(Arrays.asList(
                new EasyQueryOrderByIncorrectInspection(),
                new EasyQueryFieldMissMatchInspection(),
                new EasyQuerySetColumnsInspection()
            ));
        }
        
        return result;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 仅当项目打开时启用
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }

        runInspection(project);
    }

    /**
     * 提供给外部调用的方法，不需要AnActionEvent参数
     *
     * @param project 当前项目
     */
    public void runInspectionForProject(Project project) {
        if (project == null) {
            return;
        }
        if (!EasyQueryConfigManager.isProjectUsingEasyQuery(project)) {
            // 项目没有使用easy-query，不进行检查
            return;
        }
        runInspection(project);
    }

    /**
     * 运行检查
     *
     * @param project 当前项目
     */
    private void runInspection(Project project) {
        // 在带进度的后台任务中运行检查
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "扫描 EasyQuery 问题", true) {
            private final List<ProblemDescriptor> allProblems = new ArrayList<>();
            private final List<AbstractBaseJavaLocalInspectionTool> inspections;
            private final InspectionManager manager = InspectionManager.getInstance(project);

            {
                // 动态扫描并初始化所有检查器
                inspections = findAllInspections();
                LOG.info("发现检查器: " + inspections.size() + " 个");
                for (AbstractBaseJavaLocalInspectionTool inspection : inspections) {
                    LOG.info("已加载检查器: " + inspection.getClass().getName());
                }
            }

            // 1. 添加字段来存储结果
            private final List<ProblemDisplayItem> displayItems = new ArrayList<>();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                indicator.setIndeterminate(false);

                // 定义搜索 Java 文件的范围
                GlobalSearchScope projectScope = GlobalSearchScope.projectScope(project);
                Collection<VirtualFile> javaFiles = ReadAction.compute(() ->
                        FileTypeIndex.getFiles(StdFileTypes.JAVA, projectScope));

                int processedFiles = 0;
                int totalFiles = javaFiles.size();
                indicator.setText("正在扫描 Java 文件...");

                for (VirtualFile virtualFile : javaFiles) {
                    if (indicator.isCanceled()) {
                        break;
                    }
                    indicator.setFraction((double) processedFiles / totalFiles);
                    indicator.setText2(virtualFile.getName());

                    // 1. 使用 DumbService 包裹文件处理的 ReadAction
                    DumbService.getInstance(project).runReadActionInSmartMode(() -> {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                        if (psiFile instanceof PsiJavaFile && psiFile.isValid()) {
                            // 为每个检查器创建 holder 和 visitor
                            List<Pair<ProblemsHolder, PsiElementVisitor>> visitors = new ArrayList<>();
                            for (AbstractBaseJavaLocalInspectionTool inspection : inspections) {
                                ProblemsHolder holder = new ProblemsHolder(manager, psiFile, false);
                                PsiElementVisitor visitor = inspection.buildVisitor(holder, false);
                                visitors.add(Pair.create(holder, visitor));
                            }

                            // 使用标准访问者遍历文件元素
                            psiFile.accept(new JavaRecursiveElementWalkingVisitor() {
                                @Override
                                public void visitElement(@NotNull PsiElement element) {
                                    try {
                                        // 委托访问到所有检查器的访问者
                                        for (Pair<ProblemsHolder, PsiElementVisitor> pair : visitors) {
                                            element.accept(pair.getSecond());
                                        }
                                    } catch (Exception ex) {
                                        // 记录元素访问期间的错误
                                        LOG.warn("在文件 " + virtualFile.getName() + " 中访问元素时出错: " + element.getTextRange(), ex);
                                    }
                                    // 继续遍历树
                                    super.visitElement(element);
                                }
                            });

                            // 收集所有检查器的结果
                            synchronized (allProblems) {
                                for (Pair<ProblemsHolder, PsiElementVisitor> pair : visitors) {
                                    allProblems.addAll(pair.getFirst().getResults());
                                }
                            }
                        }
                    }); // DumbService 结束
                    processedFiles++;
                }

                // 2. 使用 DumbService 包裹最终问题处理的 ReadAction
                DumbService.getInstance(project).runReadActionInSmartMode(() -> {
                    PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
                    for (ProblemDescriptor problem : allProblems) {
                        PsiElement element = problem.getPsiElement();
                        if (element != null && element.isValid()) {
                            PsiFile file = element.getContainingFile();
                            if (file != null && file.isValid()) {
                                VirtualFile virtualFile = file.getVirtualFile();
                                // 在 ReadAction 内部获取 Document
                                Document document = psiDocumentManager.getDocument(file);
                                if (virtualFile != null && document != null) {
                                    int offset = element.getTextOffset();
                                    if (offset >= 0 && offset <= document.getTextLength()) {
                                         int correctedOffset = Math.min(offset, Math.max(0, document.getTextLength() -1));
                                         if(correctedOffset<0){
                                             correctedOffset=0;
                                         }
                                        int displayLineNumber = document.getLineNumber(correctedOffset) + 1;
                                        String description = problem.getDescriptionTemplate();
                                        ProblemHighlightType highlightType = problem.getHighlightType();

                                        String inspectionName = "其他检查";
                                        // 首先尝试从前缀中提取检查类型
                                        if (description.startsWith("[EQ插件检查-")) {
                                            int prefixStart = "[EQ插件检查-".length();
                                            int endIndex = description.indexOf(']');
                                            if (endIndex > prefixStart) {
                                                inspectionName = description.substring(prefixStart, endIndex) + "检查";
                                            }
                                        }

                                        // *** 调用 checkSuppressed 在 run 方法的 ReadAction 中 ***
                                        boolean isSuppressed = checkSuppressed(element);
                                        QuickFix[] currentFixes = problem.getFixes();

                                        // 添加到字段 displayItems
                                        displayItems.add(new ProblemDisplayItem(
                                                String.format("%s:%d - %s", file.getName(), displayLineNumber, description),
                                                virtualFile,
                                                offset,
                                                highlightType,
                                                inspectionName,
                                                isSuppressed,
                                                problem,
                                                currentFixes));
                                    }
                                }
                            }
                        }
                    }
                }); // DumbService 结束
            }

            @Override
            public void onSuccess() {
                // 3. onSuccess 只负责调度UI更新，使用字段 displayItems
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) return;
                    // 使用字段 displayItems
                    if (displayItems.isEmpty()) {
                        Messages.showInfoMessage(project, "未发现 EasyQuery 问题。", "扫描完成");
                    } else {
                        // 传递字段 displayItems
                        showResultsInToolWindow(project, displayItems);
                    }
                });
            }

            @Override
            public void onCancel() {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) return;
                    Messages.showInfoMessage(project, "EasyQuery 扫描已取消。", "扫描取消");
                });
            }

            @Override
            public void onThrowable(@NotNull Throwable error) {
                LOG.error("EasyQuery 检查扫描期间出错", error);
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) return;
                    Messages.showErrorDialog(project, "扫描期间发生错误: " + error.getMessage(), "扫描错误");
                });
            }

            // 从 run 的 ReadAction 调用
            private boolean checkSuppressed(PsiElement element) {
                boolean isSuppressed = false;
                // 检查元素上的抑制注解
                if (element instanceof PsiModifierListOwner) {
                    PsiModifierListOwner modifierListOwner = (PsiModifierListOwner) element;
                    PsiModifierList modifierList = modifierListOwner.getModifierList();
                    if (modifierList != null) {
                        PsiAnnotation[] annotations = modifierList.getAnnotations();
                        for (PsiAnnotation annotation : annotations) {
                            // 优化：先简单比较名字，如果匹配再解析qualifiedName
                            String name = annotation.getNameReferenceElement() != null ? annotation.getNameReferenceElement().getReferenceName() : null;
                            if ("SuppressWarnings".equals(name) && "java.lang.SuppressWarnings".equals(annotation.getQualifiedName())) {
                                isSuppressed = true;
                                break;
                            }
                        }
                    }
                }

                // 如果元素不是 PsiModifierListOwner，尝试获取父元素
                if (!isSuppressed) {
                    PsiElement parent = element.getParent();
                    while (parent != null && !(parent instanceof PsiFile)) {
                        if (parent instanceof PsiModifierListOwner) {
                            PsiModifierListOwner parentOwner = (PsiModifierListOwner) parent;
                            PsiModifierList parentModifierList = parentOwner.getModifierList();
                            if (parentModifierList != null) {
                                PsiAnnotation[] parentAnnotations = parentModifierList.getAnnotations();
                                for (PsiAnnotation annotation : parentAnnotations) {
                                    String name = annotation.getNameReferenceElement() != null ? annotation.getNameReferenceElement().getReferenceName() : null;
                                     if ("SuppressWarnings".equals(name) && "java.lang.SuppressWarnings".equals(annotation.getQualifiedName())) {
                                        isSuppressed = true;
                                        break;
                                    }
                                }
                            }
                            if (isSuppressed) break;
                        }
                        parent = parent.getParent();
                    }
                }
                return isSuppressed;
            }
        });
    }

    private void showResultsInToolWindow(Project project, List<ProblemDisplayItem> allItems) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow(TOOL_WINDOW_ID);

        // 如果工具窗口不存在，则注册
        if (toolWindow == null) {
            // 使用 RegisterToolWindowTask.closable 正确注册工具窗口
            // 提供默认图标，因为参数不能为 null
            RegisterToolWindowTask task = RegisterToolWindowTask.closable(
                    TOOL_WINDOW_ID,
                    AllIcons.General.Information, // 使用默认图标
                    ToolWindowAnchor.BOTTOM // 直接设置锚点
            );
            toolWindow = toolWindowManager.registerToolWindow(task);

            // 在继续之前检查注册是否成功
            if (toolWindow == null) {
                LOG.error("注册工具窗口失败: " + TOOL_WINDOW_ID);
                Messages.showErrorDialog(project, "无法创建 EasyQuery Issues 工具窗口。", "错误");
                return;
            }
        }

        // 创建主面板
        JPanel mainPanel = new JPanel(new BorderLayout());

        // 创建控制面板（过滤和排序选项）
        JPanel controlPanel = new JPanel(new BorderLayout());
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JPanel sortPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        // 创建过滤选项
        JLabel severityFilterLabel = new JLabel("按严重程度过滤:");
        JCheckBox errorCheckBox = new JCheckBox("错误", true);
        JCheckBox warningCheckBox = new JCheckBox("警告", true);
        JCheckBox suppressedCheckBox = new JCheckBox("已抑制", true);

        // 获取所有不同的检查类型
        Set<String> inspectionTypes = allItems.stream()
                .map(ProblemDisplayItem::getInspectionName)
                .collect(Collectors.toSet());

        // 创建检查类型筛选器
        JLabel typeFilterLabel = new JLabel("按检查类型过滤:");
        // 使用Map存储每种检查类型对应的复选框
        Map<String, JCheckBox> inspectionTypeCheckBoxes = new HashMap<>();

        // 创建检查类型复选框的面板，使用流式布局
        JPanel inspectionTypePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inspectionTypePanel.add(typeFilterLabel);

        // 为每种检查类型创建一个复选框
        for (String type : inspectionTypes) {
            JCheckBox checkBox = new JCheckBox(type, true);
            inspectionTypeCheckBoxes.put(type, checkBox);
            inspectionTypePanel.add(checkBox);
        }

        filterPanel.add(severityFilterLabel);
        filterPanel.add(errorCheckBox);
        filterPanel.add(warningCheckBox);
        filterPanel.add(suppressedCheckBox);
        filterPanel.add(Box.createHorizontalStrut(20));
        filterPanel.add(inspectionTypePanel);

        // 创建排序选项
        JLabel sortLabel = new JLabel("排序方式:");
        String[] sortOptions = {"严重程度", "检查类型", "文件名", "行号"};
        JComboBox<String> sortComboBox = new JComboBox<>(sortOptions);
        sortPanel.add(sortLabel);
        sortPanel.add(sortComboBox);

        controlPanel.add(filterPanel, BorderLayout.WEST);
        controlPanel.add(sortPanel, BorderLayout.EAST);

        // 创建分组树
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("EasyQuery检查结果");
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        JTree problemTree = new Tree(treeModel);

        // 设置树的渲染器
        problemTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                // 先调用父类方法获取基本组件
                Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

                if (value instanceof DefaultMutableTreeNode) {
                    Object userObject = ((DefaultMutableTreeNode) value).getUserObject();

                    if (userObject instanceof ProblemDisplayItem) {
                        ProblemDisplayItem item = (ProblemDisplayItem) userObject;
                        String originalText = item.toString();
                        String displayText = originalText;

                        // 应用删除线和/或灰色效果
                        boolean applyStrike = item.isFixed();
                        boolean applyGray = item.isFixed() || item.isSuppressed(); // 已修复或已抑制的都变灰

                        if (applyStrike) {
                             // 使用 HTML 添加删除线
                            displayText = "<html><strike>" + escapeHtml(originalText) + "</strike></html>";
                        } else {
                            // 如果不需要删除线，确保文本不是 HTML（除非原本就是）
                            // 这里简单处理，假设原始文本不是 HTML
                             setText(originalText); // 设置非 HTML 文本
                        }

                        // 如果需要 HTML (因为有删除线)，则设置 HTML 文本
                        if (applyStrike) {
                             setText(displayText);
                        }


                        // 设置图标
                        if (item.isFixed()) {
                            setIcon(AllIcons.Actions.Checked); // 使用 "Checked" 图标表示已修复
                        } else if (item.isSuppressed()) {
                            setIcon(AllIcons.Nodes.C_private);
                        } else if (item.getSeverity() == ProblemSeverity.ERROR) {
                            setIcon(AllIcons.General.Error);
                        } else if (item.getSeverity() == ProblemSeverity.WARNING) {
                            setIcon(AllIcons.General.Warning);
                        } else {
                             setIcon(AllIcons.General.Information);
                        }

                        // 设置颜色 (灰色优先)
                        if (applyGray && !selected) {
                            setForeground(JBColor.GRAY);
                        } else if (!item.isFixed() && !item.isSuppressed() && !selected) { // 仅对未修复且未抑制的项应用严重性颜色
                             if (item.getSeverity() == ProblemSeverity.ERROR) {
                                setForeground(JBColor.RED);
                             } else if (item.getSeverity() == ProblemSeverity.WARNING) {
                                setForeground(JBColor.ORANGE);
                             } else {
                                 // 如果不是 Error 或 Warning，使用默认前景色
                                 setForeground(UIManager.getColor("Tree.textForeground"));
                             }
                        } else if (selected) {
                             // 使用选中的前景色
                             setForeground(UIManager.getColor("Tree.selectionForeground"));
                        }


                    } else if (userObject instanceof String) {
                        // 分组节点的渲染不变
                        String nodeText = (String) userObject;
                        setText(nodeText);
                        if (nodeText.startsWith("错误")) {
                            setIcon(AllIcons.General.Error);
                            if (!selected) setFont(getFont().deriveFont(Font.BOLD));
                        } else if (nodeText.startsWith("警告")) {
                            setIcon(AllIcons.General.Warning);
                             if (!selected) setFont(getFont().deriveFont(Font.BOLD));
                        } else if (nodeText.startsWith("已抑制")) {
                            setIcon(AllIcons.Nodes.Locked);
                            if (!selected) setFont(getFont().deriveFont(Font.BOLD));
                        } else if (nodeText.equals("EasyQuery检查结果")) {
                            setIcon(AllIcons.Toolwindows.ToolWindowInspection);
                             if (!selected) setFont(getFont().deriveFont(Font.BOLD));
                        } else {
                             setIcon(AllIcons.Nodes.Folder);
                        }
                         // 分组节点使用默认颜色
                         if (!selected) {
                            setForeground(UIManager.getColor("Tree.textForeground"));
                         } else {
                            setForeground(UIManager.getColor("Tree.selectionForeground"));
                         }
                    }
                }
                return c; // 返回修改后的组件
            }

            // 简单的 HTML 转义方法，防止文本中的特殊字符破坏 HTML 结构
            private String escapeHtml(String text) {
                if (text == null) return "";
                return text.replace("&", "&amp;")
                           .replace("<", "&lt;")
                           .replace(">", "&gt;")
                           .replace("\"", "&quot;")
                           .replace("'", "&#39;");
            }
        });

        // 添加双击处理器和右键菜单
        problemTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                            problemTree.getLastSelectedPathComponent();

                    if (node == null) return;

                    Object userObject = node.getUserObject();
                    if (userObject instanceof ProblemDisplayItem) {
                        ProblemDisplayItem item = (ProblemDisplayItem) userObject;

                        if (item.getVirtualFile().isValid()) {
                            OpenFileDescriptor descriptor = new OpenFileDescriptor(
                                    project, item.getVirtualFile(), item.getOffset());
                            FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
                        }
                    }
                }
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                handlePopup(e);
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                handlePopup(e);
            }
            
            private void handlePopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    // 获取鼠标点击位置的树节点
                    int row = problemTree.getRowForLocation(e.getX(), e.getY());
                    if (row >= 0) {
                        problemTree.setSelectionRow(row);
                        // final 关键字可能需要，以便在 lambda 中访问
                        final DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                                problemTree.getLastSelectedPathComponent();
                        if (node != null && node.getUserObject() instanceof ProblemDisplayItem) {
                            // final 关键字可能需要
                            final ProblemDisplayItem item = (ProblemDisplayItem) node.getUserObject();
                            ProblemDescriptor problemDescriptor = item.getDescriptor();
                            QuickFix[] fixes = item.getFixes();
                            if (problemDescriptor != null && fixes != null && fixes.length > 0) {
                                JPopupMenu popupMenu = new JPopupMenu();
                                for (final QuickFix fix : fixes) { // final for lambda
                                    if (fix != null) {
                                        JMenuItem menuItem = new JMenuItem(fix.getFamilyName());
                                        menuItem.addActionListener(action -> {
                                            PsiElement element = problemDescriptor.getPsiElement();
                                            if (element != null && element.isValid()) {
                                                com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(
                                                    project,
                                                    "ApplyQuickFix: " + fix.getFamilyName(), // 更具体的命令名
                                                    null,
                                                    () -> {
                                                        if (fix instanceof LocalQuickFix) {
                                                            ((LocalQuickFix) fix).applyFix(project, problemDescriptor);
                                                            PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
                                                            PsiFile file = element.getContainingFile();
                                                            if (file != null && file.isValid()) {
                                                                Document document = psiDocumentManager.getDocument(file);
                                                                if (document != null) {
                                                                    psiDocumentManager.doPostponedOperationsAndUnblockDocument(document); // 确保操作完成
                                                                    psiDocumentManager.commitDocument(document);
                                                                    // FileDocumentManager.getInstance().saveDocument(document); // 可选：立即保存文件
                                                                }
                                                            }
                                                        }
                                                    },
                                                     // 应用修复的文件
                                                    element.getContainingFile()
                                                );

                                                // 标记为已修复并更新节点UI
                                                item.setFixed(true);
                                                final DefaultTreeModel model = (DefaultTreeModel) problemTree.getModel();
                                                // 在EDT线程更新UI
                                                ApplicationManager.getApplication().invokeLater(() -> {
                                                      model.nodeChanged(node);
                                                });
                                            }
                                        });
                                        popupMenu.add(menuItem);
                                    }
                                }
                                // 添加导航选项
                                popupMenu.addSeparator();
                                JMenuItem gotoItem = new JMenuItem("跳转到源代码");
                                gotoItem.addActionListener(action -> {
                                    if (item.getVirtualFile().isValid()) {
                                        OpenFileDescriptor descriptor = new OpenFileDescriptor(
                                                project, item.getVirtualFile(), item.getOffset());
                                        FileEditorManager.getInstance(project).openTextEditor(descriptor, true);
                                    }
                                });
                                popupMenu.add(gotoItem);
                                
                                // 显示弹出菜单
                                popupMenu.show(problemTree, e.getX(), e.getY());
                            }
                        }
                    }
                }
            }
        });

        // 添加搜索功能
        new TreeSpeedSearch(problemTree);

        // 创建汇总面板
        JPanel summaryPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel summaryLabel = new JLabel(String.format("共发现 %d 个问题: %d 个错误, %d 个警告",
                allItems.size(),
                allItems.stream().filter(item -> item.getSeverity() == ProblemSeverity.ERROR).count(),
                allItems.stream().filter(item -> item.getSeverity() == ProblemSeverity.WARNING).count()));
        summaryLabel.setIcon(AllIcons.General.Information);
        summaryPanel.add(summaryLabel);

        // 应用过滤和排序方法，更新树显示
        Runnable updateTree = () -> {
            // 应用过滤
            List<ProblemDisplayItem> filteredItems = allItems.stream()
                    .filter(item -> {
                        // 按严重程度过滤
                        if (item.getSeverity() == ProblemSeverity.ERROR && !errorCheckBox.isSelected()) {
                            return false;
                        }
                        if (item.getSeverity() == ProblemSeverity.WARNING && !warningCheckBox.isSelected()) {
                            return false;
                        }

                        // 按抑制状态过滤
                        if (item.isSuppressed() && !suppressedCheckBox.isSelected()) {
                            return false;
                        }

                        // 按检查类型过滤
                        String type = item.getInspectionName();
                        JCheckBox typeCheckBox = inspectionTypeCheckBoxes.get(type);
                        if (typeCheckBox != null && !typeCheckBox.isSelected()) {
                            return false;
                        }

                        return true;
                    })
                    .collect(Collectors.toList());

            // 应用排序
            Comparator<ProblemDisplayItem> comparator;
            String selectedSort = (String) sortComboBox.getSelectedItem();
            switch (selectedSort) {
                case "严重程度":
                    comparator = Comparator.comparing(ProblemDisplayItem::getSeverity);
                    break;
                case "检查类型":
                    comparator = Comparator.comparing(ProblemDisplayItem::getInspectionName);
                    break;
                case "文件名":
                    comparator = Comparator.comparing(i -> i.getVirtualFile().getName());
                    break;
                case "行号":
                    comparator = Comparator.comparing(i -> {
                        String display = i.toString();
                        int colonIndex = display.indexOf(':');
                        int dashIndex = display.indexOf('-');
                        if (colonIndex > 0 && dashIndex > colonIndex) {
                            try {
                                return Integer.parseInt(display.substring(colonIndex + 1, dashIndex).trim());
                            } catch (NumberFormatException e) {
                                return Integer.MAX_VALUE;
                            }
                        }
                        return Integer.MAX_VALUE;
                    });
                    break;
                default:
                    comparator = Comparator.comparing(ProblemDisplayItem::getSeverity);
            }

            // 按严重程度逆序（错误排在前面）
            if ("严重程度".equals(selectedSort)) {
                comparator = comparator.reversed();
            }

            List<ProblemDisplayItem> sortedItems = filteredItems.stream()
                    .sorted(comparator)
                    .collect(Collectors.toList());

            // 重新构建树
            rootNode.removeAllChildren();

            // 获取不同类型的项目数量
            long errorCount = sortedItems.stream()
                    .filter(item -> item.getSeverity() == ProblemSeverity.ERROR && !item.isSuppressed())
                    .count();
            long warningCount = sortedItems.stream()
                    .filter(item -> item.getSeverity() == ProblemSeverity.WARNING && !item.isSuppressed())
                    .count();
            long suppressedCount = sortedItems.stream()
                    .filter(ProblemDisplayItem::isSuppressed)
                    .count();

            // 创建顶级节点
            DefaultMutableTreeNode errorNode = new DefaultMutableTreeNode("错误 (" + errorCount + ")");
            DefaultMutableTreeNode warningNode = new DefaultMutableTreeNode("警告 (" + warningCount + ")");
            DefaultMutableTreeNode suppressedNode = new DefaultMutableTreeNode("已抑制的问题 (" + suppressedCount + ")");

            // 只有在有对应问题时才添加节点
            if (errorCount > 0) rootNode.add(errorNode);
            if (warningCount > 0) rootNode.add(warningNode);
            if (suppressedCount > 0) rootNode.add(suppressedNode);

            // 创建分类子节点
            Map<String, DefaultMutableTreeNode> errorCategoryNodes = new HashMap<>();
            Map<String, DefaultMutableTreeNode> warningCategoryNodes = new HashMap<>();
            Map<String, DefaultMutableTreeNode> suppressedCategoryNodes = new HashMap<>();

            // 将问题添加到对应的分类节点
            for (ProblemDisplayItem item : sortedItems) {
                DefaultMutableTreeNode parentNode;
                Map<String, DefaultMutableTreeNode> categoryNodes;

                if (item.isSuppressed()) {
                    // 已抑制的问题放到抑制分组
                    parentNode = suppressedNode;
                    categoryNodes = suppressedCategoryNodes;
                } else if (item.getSeverity() == ProblemSeverity.ERROR) {
                    parentNode = errorNode;
                    categoryNodes = errorCategoryNodes;
                } else {
                    parentNode = warningNode;
                    categoryNodes = warningCategoryNodes;
                }

                // 获取或创建分类节点
                DefaultMutableTreeNode categoryNode = categoryNodes.get(item.getInspectionName());
                if (categoryNode == null) {
                    categoryNode = new DefaultMutableTreeNode(item.getInspectionName());
                    categoryNodes.put(item.getInspectionName(), categoryNode);
                    parentNode.add(categoryNode);
                }

                // 将问题添加到分类节点
                DefaultMutableTreeNode itemNode = new DefaultMutableTreeNode(item);
                categoryNode.add(itemNode);
            }

            // 更新树模型
            treeModel.reload();
            
            // 展开根节点
            problemTree.expandRow(0);

            // 更新汇总信息
            summaryLabel.setText(String.format("共发现 %d 个问题: %d 个错误, %d 个警告, %d 个已抑制 (已过滤显示 %d 个)",
                    allItems.size(),
                    allItems.stream().filter(item -> item.getSeverity() == ProblemSeverity.ERROR && !item.isSuppressed()).count(),
                    allItems.stream().filter(item -> item.getSeverity() == ProblemSeverity.WARNING && !item.isSuppressed()).count(),
                    allItems.stream().filter(ProblemDisplayItem::isSuppressed).count(),
                    sortedItems.size()));
        };

        // 添加过滤器变化监听器
        ItemListener filterChangeListener = e -> updateTree.run();
        errorCheckBox.addItemListener(filterChangeListener);
        warningCheckBox.addItemListener(filterChangeListener);
        suppressedCheckBox.addItemListener(filterChangeListener);
        for (JCheckBox checkBox : inspectionTypeCheckBoxes.values()) {
            checkBox.addItemListener(filterChangeListener);
        }

        // 添加排序变化监听器
        sortComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                updateTree.run();
            }
        });

        // 创建带有工具栏的面板
        ToolbarDecorator toolbarDecorator = ToolbarDecorator.createDecorator(problemTree)
                .setAddAction(null)  // 禁用添加按钮
                .setRemoveAction(null)  // 禁用删除按钮
                .setEditAction(null)  // 禁用编辑按钮
                .setMoveUpAction(null)  // 禁用上移按钮
                .setMoveDownAction(null)  // 禁用下移按钮
                .addExtraAction(new AnActionButton("刷新", AllIcons.Actions.Refresh) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        // 重新运行扫描
                        runInspection(project);
                    }

                    @Override
                    public @NotNull ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.EDT;
                    }
                })
                .addExtraAction(new AnActionButton("全部展开", AllIcons.Actions.Expandall) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        for (int i = 0; i < problemTree.getRowCount(); i++) {
                            problemTree.expandRow(i);
                        }
                    }

                    @Override
                    public @NotNull ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.EDT;
                    }
                })
                .addExtraAction(new AnActionButton("全部折叠", AllIcons.Actions.Collapseall) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        for (int i = problemTree.getRowCount() - 1; i >= 0; i--) {
                            problemTree.collapseRow(i);
                        }
                        // 只展开根节点
                        problemTree.expandRow(0);
                    }

                    @Override
                    public @NotNull ActionUpdateThread getActionUpdateThread() {
                        return ActionUpdateThread.EDT;
                    }
                });

        // 创建带工具栏的面板
        JPanel decoratedPanel = toolbarDecorator.createPanel();

        // 添加面板到主面板
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(decoratedPanel, BorderLayout.CENTER);
        mainPanel.add(summaryPanel, BorderLayout.SOUTH);

        // 应用初始过滤和排序
        updateTree.run();

        // 创建工具窗口内容
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(mainPanel, "扫描结果", false);
        content.setDisposer(() -> {
        });

        // 将内容添加到工具窗口
        final ToolWindow tw = toolWindow;
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed() || tw.isDisposed()) return;
            tw.getContentManager().removeAllContents(true);
            tw.getContentManager().addContent(content);
            tw.activate(null);
        });
    }

    /**
     * 问题严重级别枚举
     */
    private enum ProblemSeverity {
        ERROR,
        WARNING,
        INFO
    }

    /**
     * 存储可显示的问题信息和导航数据的辅助类。
     */
    private static class ProblemDisplayItem {
        private final String displayString;
        @Getter
        private final VirtualFile virtualFile;
        @Getter
        private final int offset;
        @Getter
        private final ProblemHighlightType highlightType;
        @Getter
        private final String inspectionName;
        private final boolean isSuppressed;
        @Getter
        private final ProblemDescriptor descriptor;
        private boolean isFixed = false; // 添加 isFixed 标志
        // <-- 添加 getter
        @Getter
        private final QuickFix[] fixes; // <-- 添加字段

        public ProblemDisplayItem(String displayString, VirtualFile virtualFile, int offset,
                                  ProblemHighlightType highlightType, String inspectionName, boolean isSuppressed,
                                  ProblemDescriptor descriptor, QuickFix[] fixes) { // <-- 添加到构造函数
            this.displayString = displayString;
            this.virtualFile = virtualFile;
            this.offset = offset;
            this.highlightType = highlightType;
            this.inspectionName = inspectionName;
            this.isSuppressed = isSuppressed;
            this.descriptor = descriptor;
            this.fixes = fixes; // <-- 赋值
        }

        public boolean isSuppressed() {
            return isSuppressed;
        }

        public boolean isFixed() { // 添加 getter
            return isFixed;
        }

        public void setFixed(boolean fixed) { // 添加 setter
            isFixed = fixed;
        }

        /**
         * 获取问题的严重程度
         */
        public ProblemSeverity getSeverity() {
            if (highlightType == ProblemHighlightType.ERROR ||
                    highlightType == ProblemHighlightType.GENERIC_ERROR) {
                return ProblemSeverity.ERROR;
            } else if (highlightType == ProblemHighlightType.WARNING ||
                    highlightType == ProblemHighlightType.GENERIC_ERROR_OR_WARNING) {
                return ProblemSeverity.WARNING;
            } else {
                return ProblemSeverity.INFO;
            }
        }

        @Override
        public String toString() {
            return displayString;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProblemDisplayItem that = (ProblemDisplayItem) o;
            return offset == that.offset && Objects.equals(virtualFile, that.virtualFile);
        }

        @Override
        public int hashCode() {
            return Objects.hash(virtualFile, offset);
        }
    }
}
