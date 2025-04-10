package com.easy.query.plugin.action;

import com.easy.query.plugin.core.inspection.EasyQueryFieldMissMatchInspection;
import com.easy.query.plugin.core.inspection.EasyQueryOrderByIncorrectInspection;
import com.easy.query.plugin.core.inspection.EasyQueryWhereExpressionInspection;
import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
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
import org.jetbrains.annotations.NotNull;

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
 */
public class RunEasyQueryInspectionAction extends AnAction {

    private static final Logger LOG = Logger.getInstance(RunEasyQueryInspectionAction.class);
    private static final String TOOL_WINDOW_ID = "EasyQuery Issues";

    @Override
    public void update(@NotNull AnActionEvent e) {
        // 仅当项目打开时启用
        Project project = e.getProject();
        e.getPresentation().setEnabledAndVisible(project != null);
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
     * 运行检查
     *
     * @param project 当前项目
     */
    private void runInspection(Project project) {
        // 在带进度的后台任务中运行检查
        ProgressManager.getInstance().run(new Task.Backgroundable(project, "扫描 EasyQuery 问题", true) {
            private final List<ProblemDescriptor> allProblems = new ArrayList<>();
            private final EasyQueryWhereExpressionInspection whereInspection = new EasyQueryWhereExpressionInspection();
            private final EasyQueryOrderByIncorrectInspection orderByInspection = new EasyQueryOrderByIncorrectInspection();
            private final EasyQueryFieldMissMatchInspection fieldInspection = new EasyQueryFieldMissMatchInspection();
            private final InspectionManager manager = InspectionManager.getInstance(project);

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

                    // 在 ReadAction 中处理每个文件
                    ReadAction.run(() -> {
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(virtualFile);
                        if (psiFile instanceof PsiJavaFile && psiFile.isValid()) {
                            // Where 检查
                            ProblemsHolder whereHolder = new ProblemsHolder(manager, psiFile, false);
                            PsiElementVisitor whereVisitor = whereInspection.buildVisitor(whereHolder, false);

                            // OrderBy 检查
                            ProblemsHolder orderByHolder = new ProblemsHolder(manager, psiFile, false);
                            PsiElementVisitor orderByVisitor = orderByInspection.buildVisitor(orderByHolder, false);

                            // DTO 字段检查
                            ProblemsHolder fieldHolder = new ProblemsHolder(manager, psiFile, false);
                            PsiElementVisitor fieldVisitor = fieldInspection.buildVisitor(fieldHolder, false);

                            // 使用标准访问者遍历文件元素
                            psiFile.accept(new JavaRecursiveElementWalkingVisitor() {
                                @Override
                                public void visitElement(@NotNull PsiElement element) {
                                    try {
                                        // 委托访问到检查的访问者
                                        element.accept(whereVisitor);
                                        element.accept(orderByVisitor);
                                        element.accept(fieldVisitor);
                                    } catch (Exception ex) {
                                        // 记录元素访问期间的错误
                                        LOG.warn("在文件 " + virtualFile.getName() + " 中访问元素时出错: " + element.getTextRange(), ex);
                                    }
                                    // 至关重要的是，调用 super 继续遍历树
                                    super.visitElement(element);
                                }
                            });

                            // 添加在此文件中发现的问题（如果有）
                            synchronized (allProblems) {
                                allProblems.addAll(whereHolder.getResults());
                                allProblems.addAll(orderByHolder.getResults());
                                allProblems.addAll(fieldHolder.getResults());
                            }
                        }
                    });
                    processedFiles++;
                }
            }

            @Override
            public void onSuccess() {
                // 在 UI 线程中显示结果
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (project.isDisposed()) return; // 项目可能已关闭
                    if (allProblems.isEmpty()) {
                        Messages.showInfoMessage(project, "未发现 EasyQuery 问题。", "扫描完成");
                    } else {
                        showResultsInToolWindow(project, allProblems);
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
        });
    }

    private void showResultsInToolWindow(Project project, List<ProblemDescriptor> problems) {
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

        // 准备问题列表
        List<ProblemDisplayItem> allItems = new ArrayList<>();

        ReadAction.run(() -> {
            PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
            for (ProblemDescriptor problem : problems) {
                PsiElement element = problem.getPsiElement();
                if (element != null && element.isValid()) {
                    PsiFile file = element.getContainingFile();
                    if (file != null && file.isValid()) {
                        VirtualFile virtualFile = file.getVirtualFile();
                        Document document = psiDocumentManager.getDocument(file);
                        if (virtualFile != null && document != null) {
                            int offset = element.getTextOffset();
                            if (offset >= 0 && offset < document.getTextLength()) {
                                int displayLineNumber = document.getLineNumber(offset) + 1;
                                String description = problem.getDescriptionTemplate();
                                ProblemHighlightType highlightType = problem.getHighlightType();

                                // 通过问题描述判断问题类型，而不是使用 ProblemDescriptorImpl
                                String inspectionName = "一般检查";
                                String desc = description.toLowerCase();

                                // 首先尝试从前缀中提取检查类型
                                if (description.startsWith("[EQ插件检查-")) {
                                    int prefixStart = "[EQ插件检查-".length();
                                    int endIndex = description.indexOf(']');
                                    if (endIndex > prefixStart) {
                                        // 直接提取[EQ插件检查-XXX]中的XXX部分
                                        String extractedType = description.substring(prefixStart, endIndex);
                                        inspectionName = extractedType + "检查";
                                    }
                                }
                                // 如果没有前缀或无法从前缀中识别，将其归类为"其他检查"
                                else {
                                    inspectionName = "其他检查";
                                }

                                // 检测是否有抑制注解
                                boolean isSuppressed = false;

                                // 检查元素上的抑制注解
                                if (element instanceof PsiModifierListOwner) {
                                    PsiModifierListOwner modifierListOwner = (PsiModifierListOwner) element;
                                    PsiModifierList modifierList = modifierListOwner.getModifierList();
                                    if (modifierList != null) {
                                        // 检查是否有 @SuppressWarnings 注解
                                        PsiAnnotation[] annotations = modifierList.getAnnotations();
                                        for (PsiAnnotation annotation : annotations) {
                                            String qualifiedName = annotation.getQualifiedName();
                                            if ("java.lang.SuppressWarnings".equals(qualifiedName)) {
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
                                                    String qualifiedName = annotation.getQualifiedName();
                                                    if ("java.lang.SuppressWarnings".equals(qualifiedName)) {
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

                                allItems.add(new ProblemDisplayItem(
                                        String.format("%s:%d - %s", file.getName(), displayLineNumber, description),
                                        virtualFile,
                                        offset,
                                        highlightType,
                                        inspectionName,
                                        isSuppressed));
                            }
                        }
                    }
                }
            }
        });

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
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

                if (value instanceof DefaultMutableTreeNode) {
                    Object userObject = ((DefaultMutableTreeNode) value).getUserObject();

                    if (userObject instanceof ProblemDisplayItem) {
                        ProblemDisplayItem item = (ProblemDisplayItem) userObject;
                        setText(item.toString());

                        if (item.isSuppressed()) {
                            // 抑制问题使用特殊图标
                            setIcon(AllIcons.Nodes.C_private);
                            if (!selected) {
                                setForeground(JBColor.GRAY);
                            }
                        } else if (item.getSeverity() == ProblemSeverity.ERROR) {
                            setIcon(AllIcons.General.Error);
                            if (!selected) {
                                setForeground(JBColor.RED);
                            }
                        } else if (item.getSeverity() == ProblemSeverity.WARNING) {
                            setIcon(AllIcons.General.Warning);
                            if (!selected) {
                                setForeground(JBColor.ORANGE);
                            }
                        }
                    } else if (userObject instanceof String) {
                        String nodeText = (String) userObject;
                        setText(nodeText);

                        if (nodeText.startsWith("错误")) {
                            setIcon(AllIcons.General.Error);
                            if (!selected) {
                                setFont(getFont().deriveFont(Font.BOLD));
                            }
                        } else if (nodeText.startsWith("警告")) {
                            setIcon(AllIcons.General.Warning);
                            if (!selected) {
                                setFont(getFont().deriveFont(Font.BOLD));
                            }
                        } else if (nodeText.startsWith("已抑制")) {
                            setIcon(AllIcons.Nodes.Locked);
                            if (!selected) {
                                setFont(getFont().deriveFont(Font.BOLD));
                            }
                        } else if (nodeText.equals("EasyQuery检查结果")) {
                            setIcon(AllIcons.General.Information);
                            if (!selected) {
                                setFont(getFont().deriveFont(Font.BOLD));
                            }
                        } else {
                            setIcon(AllIcons.Nodes.Folder);
                        }
                    }
                }

                return this;
            }
        });

        // 添加双击处理器
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

            // 展开节点
            problemTree.expandRow(0); // 根节点
            for (int i = 1; i < Math.min(4, problemTree.getRowCount()); i++) {
                problemTree.expandRow(i);
            }

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
                })
                .addExtraAction(new AnActionButton("全部展开", AllIcons.Actions.Expandall) {
                    @Override
                    public void actionPerformed(@NotNull AnActionEvent e) {
                        for (int i = 0; i < problemTree.getRowCount(); i++) {
                            problemTree.expandRow(i);
                        }
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
        private final VirtualFile virtualFile;
        private final int offset;
        private final ProblemHighlightType highlightType;
        private final String inspectionName;
        private final boolean isSuppressed;  // 是否被抑制

        public ProblemDisplayItem(String displayString, VirtualFile virtualFile, int offset,
                                  ProblemHighlightType highlightType, String inspectionName, boolean isSuppressed) {
            this.displayString = displayString;
            this.virtualFile = virtualFile;
            this.offset = offset;
            this.highlightType = highlightType;
            this.inspectionName = inspectionName;
            this.isSuppressed = isSuppressed;
        }

        public VirtualFile getVirtualFile() {
            return virtualFile;
        }

        public int getOffset() {
            return offset;
        }

        public ProblemHighlightType getHighlightType() {
            return highlightType;
        }

        public String getInspectionName() {
            return inspectionName;
        }

        public boolean isSuppressed() {
            return isSuppressed;
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
