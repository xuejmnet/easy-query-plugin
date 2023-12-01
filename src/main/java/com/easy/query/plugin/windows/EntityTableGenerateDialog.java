package com.easy.query.plugin.windows;

import com.easy.query.plugin.core.RenderEasyQueryTemplate;
import com.easy.query.plugin.core.Template;
import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.entity.TableInfo;
import com.easy.query.plugin.core.persistent.EasyQueryFlexPluginConfigData;
import com.easy.query.plugin.core.render.ModuleComBoxRender;
import com.easy.query.plugin.core.render.TableListCellRenderer;
import com.easy.query.plugin.core.util.DialogUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.ObjectUtil;
import com.easy.query.plugin.core.util.PackageUtil;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.ValidationInfo;
import com.easy.query.plugin.core.util.StrUtil;
import com.easy.query.plugin.core.util.TableUtils;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.FixedSizeButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaDirectoryService;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.ui.DocumentAdapter;
import com.intellij.util.ArrayUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import com.intellij.ui.components.fields.ExtendableTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class EntityTableGenerateDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox selectAll;
    private JTextField tableSearch;
    private JList<String> tableList;
    private ExtendableTextField modelPackagePath;
    private JPanel modelPanel;
    private FixedSizeButton modelBtn;
    private JComboBox<String> modelCombox;
    private JTextField tablePrefix;
    private JButton columnMappingBtn;
    private JButton modelTemplateBtn;


    Map<String, Module> moduleMap;
    Map<String, Map<String, String>> modulePackageMap;
    Boolean isManvenProject;
    Project project;
    Map<String, TableInfo> tableInfoMap;
    List<String> tableNameList;
    Map<String, Set<String>> INVERTED_TABLE_INDEX = new HashMap<>();
    Map<String, Set<String>> INVERTED_MODULE_INDEX = new HashMap<>();
    List<JComboBox<String>> list = Arrays.asList(modelCombox);
    List<JTextField> packageList = Arrays.asList(modelPackagePath);

    public EntityTableGenerateDialog(AnActionEvent actionEvent) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setSize(550, 400);
        setTitle("Entity Generate");
        DialogUtil.centerShow(this);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

//
//
//        tableList.addListSelectionListener(e -> {
//
//            if (e.getValueIsAdjusting()) {
//                return;
//            }
//            boolean selected = selectAll.isSelected();
//            if (selected) {
//                selectAll.setSelected(false);
//            }
//            int size = tableList.getSelectedValuesList().size();
//            if (size == tableList.getModel().getSize() && size > 0) {
//                selectAll.setSelected(true);
//            }
//        });
        project = actionEvent.getProject();

        tableInfoMap = TableUtils.getAllTables(actionEvent)
                .stream().collect(Collectors.toMap(TableInfo::getName, o -> o));

        DefaultListModel<String> model = new DefaultListModel<>();
        // tableNameSet按照字母降序
        tableNameList = new ArrayList<>(tableInfoMap.keySet());
        Collections.sort(tableNameList);
        model.addAll(tableNameList);
        tableList.setModel(model);
        TableListCellRenderer cellRenderer = new TableListCellRenderer(tableInfoMap);
        tableList.setCellRenderer(cellRenderer);

        init();

        tableList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            boolean selected = selectAll.isSelected();
            if (selected) {
                selectAll.setSelected(false);
            }
            int size = tableList.getSelectedValuesList().size();
            if (size == tableList.getModel().getSize() && size > 0) {
                selectAll.setSelected(true);
            }
        });
        selectAll.addActionListener(e -> {
            if (selectAll.isSelected()) {
                tableList.setSelectionInterval(0, tableList.getModel().getSize() - 1);
            } else {
                tableList.clearSelection();
            }
        });

        tableSearch.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                String tableName = tableSearch.getText();
                if (StringUtils.isNotBlank(tableName)) {
                    if (!tableList.isSelectionEmpty()) {
                        tableList.clearSelection();
                    }
                    Set<String> search = search(tableName.trim(), cellRenderer);
                    model.removeAllElements();
                    model.addAll(search);
                }
            }
        });
        initPackagePath();
        modelCombox.addActionListener(e -> {
            EasyQueryConfig configData = getConfigData();
            modelPackagePath.setText(getPackagePath(String.valueOf(modelCombox.getSelectedItem()), ObjectUtil.defaultIfNull(configData.getDomainPath(), "domain")));
        });
    }

    private void initBtn() {
        modelBtn.addActionListener(e -> {
            Object selectedItem = modelCombox.getSelectedItem();
            String s = String.valueOf(selectedItem);
            modelPackagePath.setText(PackageUtil.selectPackage(getModule(s), modelPackagePath.getText()));
        });
    }

    /**
     * 获取模块
     *
     * @param moduleName 模块名称
     * @return {@code Module}
     */
    public  Module getModule(String moduleName) {
        return moduleMap.get(moduleName);
    }
    /**
     * 得到包路径
     *
     * @param moduleName  模块名称
     * @param packageName 系统配置包名
     * @return {@code String}
     */
    public String getPackagePath(String moduleName, String packageName) {
        Map<String, String> moduleMap = modulePackageMap.get(moduleName);
        if (moduleMap == null || moduleMap.isEmpty()) {
            NotificationUtils.notifyError("模块不存在!", "", project);
            return "";
            // throw new RuntimeException(StrUtil.format("模块不存在:{}", moduleName));
        }
        return moduleMap.getOrDefault(packageName, "");
    }

    public EasyQueryConfig getConfigData() {
        EasyQueryConfig config = Template.getEasyQueryConfig(project);

        config.setModelPackage(modelPackagePath.getText());
        config.setModelModule(getTextFieldVal(modelCombox));
        config.setTablePrefix(tablePrefix.getText());
        return config;
    }

    public String getTextFieldVal(JComboBox<String> comboBox) {
        JTextField textField = (JTextField) comboBox.getEditor().getEditorComponent();
        return textField.getText();
    }

    /**
     * 传入表名集合，建立倒排索引
     *
     * @param tableNames 表名
     */
    public void initTableIndexText(Collection<String> tableNames) {
        for (String tableName : tableNames) {
            for (int i = 0; i < tableName.length(); i++) {
                char word = tableName.charAt(i);
                INVERTED_TABLE_INDEX.computeIfAbsent((word + "").toLowerCase(), k -> new HashSet<>()).add(tableName);
            }
        }
    }

    public void initModuleIndexText(Collection<String> tableNames) {
        for (String tableName : tableNames) {
            for (int i = 0; i < tableName.length(); i++) {
                char word = tableName.charAt(i);
                INVERTED_MODULE_INDEX.computeIfAbsent((word + "").toLowerCase(), k -> new HashSet<>()).add(tableName);
            }
        }
    }

    private void init() {
        initTableIndexText(tableNameList);
        // 初始化模块
        initModules(list);
        initBtn();
        initConfigData(null);
        initPackageList();
    }


    public void initPackageList() {
        packageList.stream().forEach(textField -> {
            ComponentValidator validator = new ComponentValidator(project);
            validator.withValidator(() -> {
                String pt = textField.getText();
                return StrUtil.isEmpty(pt) ? new ValidationInfo("请选择生成路径", textField) : null;
            }).installOn(textField);

        });
    }

    private void initPackagePath() {
//        int idx = sinceComBox.getSelectedIndex();
//        if (idx == 0) {
        EasyQueryConfig configData = getConfigData();
        modelPackagePath.setText(getPackagePath(String.valueOf(modelCombox.getSelectedItem()), ObjectUtil.defaultIfNull(configData.getDomainPath(), "domain")));
//        }
    }

    public void initConfigData(EasyQueryConfig config) {
        if (ObjectUtil.isNull(config)) {
            config = Template.getEasyQueryConfig(project);
        }
        modelPackagePath.setText(config.getModelPackage());
        String modelModule = config.getModelModule();
        if (StrUtil.isNotEmpty(modelModule)) {
            modelCombox.setSelectedItem(modelModule);
        } else {
            modelCombox.setSelectedIndex(0);
        }

        for (JComboBox<String> jComboBox : list) {
            Object selectedItem = jComboBox.getSelectedItem();
            if (selectedItem != null && !"".equals(selectedItem)) {
                jComboBox.repaint();
            }
        }
    }

    private void onOK() {
        // add your code here
        onGenerate();


        dispose();
    }
    private void onGenerate(){
        for (JComboBox<String> el : list) {
            JTextField textField = (JTextField) el.getEditor().getEditorComponent();
            String moduleName = textField.getText();
            if (!containsModule(moduleName)) {
                Messages.showWarningDialog(String.format("找不到名称为：【%s】的模块", moduleName), "提示");
                return;
            }
        }

        List<String> selectedTabeList = tableList.getSelectedValuesList();
        if (CollectionUtils.isEmpty(selectedTabeList)) {
            Messages.showWarningDialog("请选择要生成的表", "提示");
            return;
        }
        List<TableInfo> selectedTableInfo = new CopyOnWriteArrayList<>();
        for (String tableName : selectedTabeList) {
            selectedTableInfo.add(tableInfoMap.get(tableName));
        }
        // boolean flag = checkTableInfo(selectedTableInfo);
        // if (flag) {
//        String since = sinceComBox.getSelectedItem().toString();
        EasyQueryConfig configData = getConfigData();
//
//
//        if (!SINCE_CONFIG.equals(since)) {
//            MybatisFlexPluginConfigData.removeSinceConfig(since);
//            MybatisFlexPluginConfigData.configSince(since, configData);
//        }
        EasyQueryFlexPluginConfigData.setCurrentEasyQueryConfig(configData,project);

        startGenCode(selectedTableInfo);
    }


    private void startGenCode(List<TableInfo> selectedTableInfo) {
        EasyQueryConfig configData = getConfigData();
//        for (JCheckBox box : enableList) {
//            boolean selected = box.isSelected();
//            if (selected) {
//                continue;
//            }
//            ReflectUtil.setFieldValue(configData, box.getName(),"");
//        }
        RenderEasyQueryTemplate.assembleData(selectedTableInfo, configData, project,getModule(String.valueOf(modelCombox.getSelectedItem())));
        NotificationUtils.notifySuccess("代码生成成功", project);
        onCancel();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }


    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

    public boolean containsModule(String moduleName) {
        return moduleMap.containsKey(moduleName);
    }

    private Set<String> search(String tableName, TableListCellRenderer cellRenderer) {
        Map<String, String> highlightKey = highlightKey(tableName);
        cellRenderer.setSearchTableName(tableName);
        cellRenderer.setHighlightKey(highlightKey);
        return highlightKey.keySet();
    }

    /**
     * 搜索
     *
     * @param keyword 关键字
     * @return {@code Set<String>}
     */
    public Set<String> search(String keyword) {
        if (StrUtil.isEmpty(keyword)) {
            return INVERTED_TABLE_INDEX.values().stream()
                    .flatMap(el -> el.stream())
                    .collect(Collectors.toSet());
        }
        keyword = keyword.toLowerCase();
        Set<String> result = new HashSet<>();
        for (int i = 0; i < keyword.length(); i++) {
            char key = keyword.charAt(i);
            result.addAll(INVERTED_TABLE_INDEX.getOrDefault(key + "", Collections.emptySet()));
        }
        String finalKeyword = keyword;
        result = result.stream()
                .filter(el -> {
                    for (int i = 0; i < finalKeyword.length(); i++) {
                        String key = finalKeyword.charAt(i) + "";
                        if (StringUtils.containsIgnoreCase(el, key)) {
                            el = el.replaceFirst(key, "");
                        } else {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toSet());
        return result;
    }

    public Map<String, String> highlightKey(String keyword) {

        Set<String> result = search(keyword);
        if (StrUtil.isEmpty(keyword)) {
            return result.stream().collect(Collectors.toMap(el -> el, el -> el));
        }
        // 字符串排序

        Map<String, Integer> idxMap = new HashMap<>();
        Map<String, String> highlightMap = new HashMap<>();
        result.stream()
                .forEach(el -> {
                    String finalKeyword = keyword;
                    String htmlText = "<html>";
                    for (int i = 0; i < el.length(); i++) {
                        String key = el.charAt(i) + "";
                        if (StringUtils.containsIgnoreCase(finalKeyword, key)) {
                            htmlText += "<span style='color:#c60'>" + key + "</span>";
                            finalKeyword = finalKeyword.replaceFirst(key, "");
                            continue;
                        }
                        htmlText += key;
                    }
                    htmlText += "</html>";
                    idxMap.clear();
                    highlightMap.put(el, htmlText);
                });
        return highlightMap;
    }


    /**
     * 初始化模块
     *
     * @param list 列表
     */
    public void initModules(List<JComboBox<String>> list) {
        addModulesItem(list);
    }

    /**
     * 获取模块
     *
     * @return {@code List<String>}
     */
    public void addModulesItem(List<JComboBox<String>> modulesComboxs) {
        Module[] modules = ModuleManager.getInstance(project).getModules();
        if (ArrayUtil.isEmpty(modules)) {
            NotificationUtils.notifyError("目录层级有误!", "", project);
            return;
        }

        boolean isManvenProject = isManvenProject(modules[0]);
        for (JComboBox<String> modulesCombox : modulesComboxs) {
            modulesCombox.setRenderer(new ModuleComBoxRender());

            moduleMap = Arrays.stream(modules)
                    .filter(module -> {
                        if (isManvenProject) {
                            @NotNull VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();
                            return sourceRoots.length > 0;
                        }
                        // 非maven项目只显示main模块,只有main模块才有java目录
                        return module.getName().contains(".main");
                    })
                    .collect(Collectors.toMap(el -> {
                        String name = el.getName();
                        if (name.contains(".")) {
                            String[] strArr = name.split("\\.");
                            return strArr[strArr.length - 2];
                        }
                        return name;
                    }, module -> module));
            String[] array = moduleMap.keySet().toArray(new String[moduleMap.size()]);
            DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(array);
//            FilterComboBoxModel model = new FilterComboBoxModel(new ArrayList<>(moduleMap.keySet()), moduleMap.isEmpty() ? -1 : 0);
            modulesCombox.setModel(model);
            modulesCombox.setSelectedIndex(0);
        }
        getModulePackages();
    }

    public void getModulePackages() {
        modulePackageMap = new HashMap<>();
        for (Module module : moduleMap.values()) {
            Map<String, String> moduleMap = new HashMap<>();
            PsiManager psiManager = PsiManager.getInstance(project);
            FileIndex fileIndex = module != null ? ModuleRootManager.getInstance(module).getFileIndex() : ProjectRootManager.getInstance(project).getFileIndex();
            fileIndex.iterateContent(fileOrDir -> {
                if (fileOrDir.isDirectory() && (fileIndex.isUnderSourceRootOfType(fileOrDir, JavaModuleSourceRootTypes.SOURCES) || fileIndex.isUnderSourceRootOfType(fileOrDir, JavaModuleSourceRootTypes.RESOURCES))) {
                    PsiDirectory psiDirectory = psiManager.findDirectory(fileOrDir);
                    PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
                    if (aPackage != null) {
                        moduleMap.put(aPackage.getName(), aPackage.getQualifiedName());
                    }
                }
                return true;
            });
            String name = module.getName();
            if (name.contains(".")) {
                String[] strArr = name.split("\\.");
                name = strArr[strArr.length - 2];
            }
            modulePackageMap.put(name, moduleMap);
        }
        initModuleIndexText(modulePackageMap.keySet());
    }

    /**
     * 判断是否manven项目
     *
     * @return boolean
     */
    public boolean isManvenProject(Module module) {
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        if (ArrayUtil.isEmpty(contentRoots)) {
            return false;
        }
        VirtualFile contentRoot = contentRoots[0];
        VirtualFile virtualFile = contentRoot.findChild("pom.xml");
        isManvenProject = Objects.nonNull(virtualFile);
        return isManvenProject;
    }
}
