package com.easy.query.plugin.windows;

import cn.hutool.setting.Setting;
import com.alibaba.fastjson2.JSON;
import com.easy.query.plugin.config.EasyQueryConfigManager;
import com.easy.query.plugin.config.EasyQueryProjectSettingKey;
import com.easy.query.plugin.core.RenderEasyQueryTemplate;
import com.easy.query.plugin.core.Template;
import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.entity.MatchTypeMapping;
import com.easy.query.plugin.core.entity.TableMetadata;
import com.easy.query.plugin.core.persistent.EasyQueryQueryPluginConfigData;
import com.easy.query.plugin.core.render.ModuleComBoxRender;
import com.easy.query.plugin.core.render.TableListCellRenderer;
import com.easy.query.plugin.core.util.DialogUtil;
import com.easy.query.plugin.core.util.EasyQueryConfigUtil;
import com.easy.query.plugin.core.util.FileChooserUtil;
import com.easy.query.plugin.core.util.MyModuleUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.ObjectUtil;
import com.easy.query.plugin.core.util.PackageUtil;
import com.easy.query.plugin.core.validator.InputValidatorImpl;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.ui.MessageConstants;
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
import com.intellij.util.PlatformIcons;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;
import com.intellij.ui.components.fields.ExtendableTextField;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class EntityTableGenerateDialog extends JDialog {
    public static final String SINCE_CONFIG = "---请选择配置---" ;
    public static final String PROJECT_CONFIG = "【使用当前项目配置】" ;
    public static final String SINCE_CONFIG_ADD = "添加配置" ;
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
    private JCheckBox builderCheckBox;
    private JCheckBox dataCheckBox;
    private JCheckBox allArgsConstructorCheckBox;
    private JCheckBox noArgsConstructorCheckBox;
    private JCheckBox accessorsCheckBox;
    private JCheckBox requiredArgsConstructorCheckBox;
    private JTextField author;
    private JCheckBox entityProxyCheck;
    private JCheckBox entityFileProxyCheck;
    private JComboBox sinceComBox;
    private JButton saveConfigBtn;
    private JTextField modelSuffixText;
    private JCheckBox swaggerCheckBox;
    private JCheckBox swagger3CheckBox;
    private JCheckBox easyAssertMessageCheckBox;
    private JButton importBtn;
    private JButton exportBtn;
    private JButton confDelBtn;
    private JTextField ignoreColumnsText;
    private JTextField superClassText;
    private JButton overrideBtn;
    private JButton search_btn;
    private JButton projectBtn;

    Map<String, Module> moduleMap;
    Map<String, Map<String, String>> modulePackageMap;
    Boolean isManvenProject;
    Project project;
    Map<String, TableMetadata> tableInfoMap;
    List<String> tableNameList;
    Map<String, Set<String>> INVERTED_TABLE_INDEX = new HashMap<>();
    Map<String, Set<String>> INVERTED_MODULE_INDEX = new HashMap<>();
    List<JComboBox<String>> list = Arrays.asList(modelCombox);
    List<JTextField> packageList = Arrays.asList(modelPackagePath);

    private String modelTemplate;
    private Map<String, List<MatchTypeMapping>> typeMapping;

    public EntityTableGenerateDialog(AnActionEvent actionEvent) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        // 获取屏幕的大小
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(1000, (int) screenSize.getWidth() - 50), Math.min(900, (int) (screenSize.getHeight() * 0.9)));
        setTitle("Entity Generate");
        DialogUtil.centerShow(this);
        confDelBtn.setIcon(PlatformIcons.DELETE_ICON);
        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        overrideBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOverride();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        saveConfigBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onSaveCurrent();
            }
        });
        confDelBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onDelCurrent();
            }
        });
        exportBtn.addActionListener(e -> {

            String exportPath = FileChooserUtil.chooseDirectory(project);

            if (StrUtil.isEmpty(exportPath)) {
                return;
            }
            EasyQueryQueryPluginConfigData.export(exportPath);
        });
        importBtn.addActionListener(e -> {

            VirtualFile virtualFile = FileChooserUtil.chooseFileVirtual(project);
            if (ObjectUtil.isNull(virtualFile)) {
                return;
            }
            String path = virtualFile.getPath();
            EasyQueryQueryPluginConfigData.importConfig(path);
            init();
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

        project = actionEvent.getProject();

        tableInfoMap = TableUtils.getAllTables(actionEvent)
            .stream().collect(Collectors.toMap(TableMetadata::getName, o -> o));

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

        search_btn.addActionListener(e -> {
            String tableName = tableSearch.getText();
            if (StringUtils.isNotBlank(tableName)) {
                if (!tableList.isSelectionEmpty()) {
                    tableList.clearSelection();
                }
                Set<String> search = searchLike(tableName.trim(), cellRenderer);
                model.removeAllElements();
                model.addAll(search);
            }
        });
        projectBtn.addActionListener(e -> {
            configToProjectSetting();
        });
        modelTemplateBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ModelTemplateEditorDialog modelTemplateEditorDialog = new ModelTemplateEditorDialog(project, modelTemplate, newTemplate -> {
                    modelTemplate = newTemplate;
                });
                modelTemplateEditorDialog.setVisible(true);
            }
        });
        columnMappingBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ColumnMappingDialog columnMappingDialog = new ColumnMappingDialog(project, typeMapping, typeMap -> {
                    typeMapping = typeMap;
                });
                columnMappingDialog.setVisible(true);
            }
        });
//        initPackagePath();
//        modelCombox.addActionListener(e -> {
//            EasyQueryConfig configData = getConfigData();
//            modelPackagePath.setText(getPackagePath(String.valueOf(modelCombox.getSelectedItem()), ObjectUtil.defaultIfNull(configData.getModelPackage(), "domain")));
//        });
    }

    /**
     * Constructor with pre-selected table name
     * 构造函数，预选择表名
     *
     * @param actionEvent          操作事件
     * @param preSelectedTableName 预选择的表名
     */
    public EntityTableGenerateDialog(AnActionEvent actionEvent, String preSelectedTableName) {
        this(actionEvent); // Call the main constructor
        // Pre-populate the search field with the selected table name
        if (preSelectedTableName != null && !preSelectedTableName.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                tableSearch.setText(preSelectedTableName.trim());
                // Trigger search to filter the table list
                performSearch(preSelectedTableName.trim());
            });
        }
    }

    /**
     * Perform search and update table list
     * 执行搜索并更新表列表
     *
     * @param searchText 搜索文本
     */
    private void performSearch(String searchText) {
        if (StringUtils.isNotBlank(searchText)) {
            if (!tableList.isSelectionEmpty()) {
                tableList.clearSelection();
            }
            TableListCellRenderer cellRenderer = (TableListCellRenderer) tableList.getCellRenderer();
            Set<String> searchResult = search(searchText, cellRenderer);
            DefaultListModel<String> model = (DefaultListModel<String>) tableList.getModel();
            model.removeAllElements();
            model.addAll(searchResult);

            // Auto-select the table if there's an exact match
            for (String tableName : searchResult) {
                if (tableName.equalsIgnoreCase(searchText)) {
                    tableList.setSelectedValue(tableName, true);
                    break;
                }
            }
        }
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
    public Module getModule(String moduleName) {
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
            return "" ;
            // throw new RuntimeException(StrUtil.format("模块不存在:{}", moduleName));
        }
        return moduleMap.getOrDefault(packageName, "");
    }

    private EasyQueryConfig getConfigData0(){
        EasyQueryConfig config=null;
        String selectItem = sinceComBox.getSelectedItem() + "" ;
        if(PROJECT_CONFIG.equals(selectItem)){
            config=getSettingConfigData0();
        }else{
            config = Template.getEasyQueryConfig(project, selectItem);
        }
        return config;
    }
    private EasyQueryConfig getSettingConfigData0(){
        String generateValue = EasyQueryConfigUtil.getProjectSettingStr(project, EasyQueryProjectSettingKey.SQL_GENERATE, "");
        if(StrUtil.isNotBlank(generateValue)){
            try {
                byte[] decode = Base64.getDecoder().decode(generateValue.getBytes(StandardCharsets.UTF_8));
                String configJson = new String(decode, StandardCharsets.UTF_8);
                return JSON.parseObject(configJson, EasyQueryConfig.class);
            }catch (Exception e){
                Messages.showWarningDialog("找不到名称为：【"+PROJECT_CONFIG+"】的配置", "提示");
            }
        }
        return null;
    }
    public EasyQueryConfig getConfigData() {



        EasyQueryConfig config = getConfigData0();
        if(config==null){
            throw new RuntimeException((String.format("找不到名称为：【%s】的配置", sinceComBox.getSelectedItem() + "")));
        }

        config.setModelPackage(modelPackagePath.getText());
        config.setModelModule(getTextFieldVal(modelCombox));
        config.setTablePrefix(tablePrefix.getText());
        config.setModelSuffix(modelSuffixText.getText());
        config.setAuthor(author.getText());
        config.setBuilder(builderCheckBox.isSelected());
        config.setData(dataCheckBox.isSelected());
        config.setAllArgsConstructor(allArgsConstructorCheckBox.isSelected());
        config.setNoArgsConstructor(noArgsConstructorCheckBox.isSelected());
        config.setAccessors(accessorsCheckBox.isSelected());
        config.setRequiredArgsConstructor(requiredArgsConstructorCheckBox.isSelected());
        config.setEntityProxy(entityProxyCheck.isSelected());
        config.setEntityFileProxy(entityFileProxyCheck.isSelected());
        config.setModelTemplate(modelTemplate);
        config.setSwagger(swaggerCheckBox.isSelected());
        config.setSwagger3(swagger3CheckBox.isSelected());
        config.setEasyAssertMessage(easyAssertMessageCheckBox.isSelected());
        config.setTypeMapping(typeMapping);
        config.setIgnoreColumns(ignoreColumnsText.getText());
        String superClass = superClassText.getText();
        config.setModelSuperClass(StringUtils.isBlank(superClass) ? null : superClass);
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
                INVERTED_TABLE_INDEX.computeIfAbsent((word + ""), k -> new HashSet<>()).add(tableName);
            }
        }
    }

    public void initModuleIndexText(Collection<String> tableNames) {
        for (String tableName : tableNames) {
            for (int i = 0; i < tableName.length(); i++) {
                char word = tableName.charAt(i);
                INVERTED_MODULE_INDEX.computeIfAbsent((word + ""), k -> new HashSet<>()).add(tableName);
            }
        }
    }

    private void init() {
        initTableIndexText(tableNameList);
        // 初始化模块
        initModules(list);
        initBtn();
        initSinceComBox(null);
        initConfigData(null);
        initPackageList();
    }

    private boolean initProjectSettingConfig() {
        String generateValue = EasyQueryConfigUtil.getProjectSettingStr(project, EasyQueryProjectSettingKey.SQL_GENERATE, null);
        if (StrUtil.isNotBlank(generateValue)) {
            sinceComBox.insertItemAt(PROJECT_CONFIG,1);
            return true;
        }
        return false;
    }

    public void initSinceComBox(Integer idx) {
        Set<String> list = EasyQueryQueryPluginConfigData.getProjectSinceMap().keySet();
        sinceComBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                if (SINCE_CONFIG_ADD.equals(value.toString())) {
                    setIcon(AllIcons.General.Add);
                } else {
                    setIcon(null); // 清除图标
                }
                setText(value.toString());
                return this;
            }
        });
        sinceComBox.removeAllItems();
        sinceComBox.addItem(SINCE_CONFIG);
        for (String item : list) {
            if (!EasyQueryQueryPluginConfigData.isHiddenConfigKey(item)) {
                sinceComBox.insertItemAt(item, 1);
            }
        }
        boolean settingHasConfig = initProjectSettingConfig();
        sinceComBox.addItem(SINCE_CONFIG_ADD);
        if (ObjectUtil.isNull(idx)) {
            sinceComBox.setSelectedIndex(sinceComBox.getItemCount() > 2 ? 1 : 0);
        } else {
            sinceComBox.setSelectedIndex(idx);
        }
        sinceComBox.revalidate();
        sinceComBox.repaint();


        sinceComBox.addActionListener(e -> {
            Object selectedItem = sinceComBox.getSelectedItem();
            if (ObjectUtil.isNull(selectedItem)) {
                return;
            }
            if (selectedItem.toString().equals(SINCE_CONFIG_ADD)) {
                sinceComBox.hidePopup();
                Messages.InputDialog dialog = new Messages.InputDialog("请输入配置名称", "配置名称", Messages.getQuestionIcon(), "", new InputValidatorImpl());
                dialog.show();
                String configName = dialog.getInputString();
                if (StrUtil.isBlank(configName)) {
                    initSinceComBox(null);
                    return;
                }
                if (EasyQueryQueryPluginConfigData.isHiddenConfigKey(configName)) {
                    initSinceComBox(null);
                    Messages.showWarningDialog("配置名称:[" + configName + "]是系统默认的", "提示");
                    return;
                }
                EasyQueryQueryPluginConfigData.saveConfigSince(configName, getConfigData());
                NotificationUtils.notifySuccess("保存成功", project);
                initSinceComBox(null);
                return;
            } else if (SINCE_CONFIG.equals(selectedItem.toString())) {
                initConfigData(null);
                return;
            }
            String key = selectedItem.toString();
            LinkedHashMap<String, EasyQueryConfig> projectSinceMap = EasyQueryQueryPluginConfigData.getProjectSinceMap();
            if(settingHasConfig){
                EasyQueryConfig settingConfig = getSettingConfigData0();
                projectSinceMap.put(PROJECT_CONFIG,settingConfig);
            }
            EasyQueryConfig config = projectSinceMap.getOrDefault(key, new EasyQueryConfig());
            initConfigData(config);
        });
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

//    private void initPackagePath() {
////        int idx = sinceComBox.getSelectedIndex();
////        if (idx == 0) {
//        EasyQueryConfig configData = getConfigData();
//        modelPackagePath.setText(getPackagePath(String.valueOf(modelCombox.getSelectedItem()), ObjectUtil.defaultIfNull(configData.getModelPackage(), "domain")));

    /// /        }
//    }


    public void initConfigData(EasyQueryConfig config) {
        if (ObjectUtil.isNull(config)) {
            config = getConfigData0();
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

        tablePrefix.setText(config.getTablePrefix());
        author.setText(config.getAuthor());
        modelSuffixText.setText(config.getModelSuffix());
        dataCheckBox.setSelected(config.isData());
        builderCheckBox.setSelected(config.isBuilder());
        allArgsConstructorCheckBox.setSelected(config.isAllArgsConstructor());
        noArgsConstructorCheckBox.setSelected(config.isNoArgsConstructor());
        accessorsCheckBox.setSelected(config.isAccessors());
        requiredArgsConstructorCheckBox.setSelected(config.isRequiredArgsConstructor());
        entityProxyCheck.setSelected(config.isEntityProxy());
        entityFileProxyCheck.setSelected(config.isEntityFileProxy());
        swaggerCheckBox.setSelected(config.isSwagger());
        easyAssertMessageCheckBox.setSelected(config.isEasyAssertMessage());
        swagger3CheckBox.setSelected(config.isSwagger3());
        modelTemplate = config.getModelTemplate();
        typeMapping = config.getTypeMapping();
        superClassText.setText(config.getModelSuperClass());
        ignoreColumnsText.setText(config.getIgnoreColumns());
    }

    private void onOK() {
        // add your code here
        boolean close = onGenerate(false);
        if (!close) {
            return;
        }


        dispose();
    }

    private void onOverride() {
        // add your code here
        boolean close = onGenerate(true);
        if (!close) {
            return;
        }

        NotificationUtils.notifySuccess("功能开发中...", project);

        dispose();
    }

    private boolean onGenerate(boolean override) {
        for (JComboBox<String> el : list) {
            JTextField textField = (JTextField) el.getEditor().getEditorComponent();
            String moduleName = textField.getText();
            if (!containsModule(moduleName)) {
                Messages.showWarningDialog(String.format("找不到名称为：【%s】的模块", moduleName), "提示");
                return false;
            }
        }

        List<String> selectedTabeList = tableList.getSelectedValuesList();
        if (CollectionUtils.isEmpty(selectedTabeList)) {
            Messages.showWarningDialog("请选择要生成的表", "提示");
            return false;
        }
        List<TableMetadata> selectedTableInfo = new CopyOnWriteArrayList<>();
        for (String tableName : selectedTabeList) {
            selectedTableInfo.add(tableInfoMap.get(tableName));
        }
        startGenCode(selectedTableInfo, override);
        return true;
    }


    private void startGenCode(List<TableMetadata> selectedTableInfo, boolean override) {
        EasyQueryConfig configData = getConfigData();
//        for (JCheckBox box : enableList) {
//            boolean selected = box.isSelected();
//            if (selected) {
//                continue;
//            }
//            ReflectUtil.setFieldValue(configData, box.getName(),"");
//        }
        RenderEasyQueryTemplate.assembleData(selectedTableInfo, configData, project, getModule(String.valueOf(modelCombox.getSelectedItem())), override);
        NotificationUtils.notifySuccess("代码生成成功", project);
        onCancel();
    }

    /**
     * 配置保存到setting文件
     */
    private void configToProjectSetting() {
        EasyQueryConfig configData = getConfigData();
        String jsonString = JSON.toJSONString(configData);
        if (jsonString == null) {
            Messages.showWarningDialog("无法获取当前配置信息:[" + sinceComBox.getSelectedItem() + "]", "提示");
            return;
        }
        byte[] encode = Base64.getEncoder().encode(jsonString.getBytes(StandardCharsets.UTF_8));
        String value = new String(encode, StandardCharsets.UTF_8);
        try {
            Setting setting = EasyQueryConfigUtil.getProjectConfig(project);
            setting.put(EasyQueryProjectSettingKey.SQL_GENERATE, value);
            setting.store();
            NotificationUtils.notifySuccess("配置保存成功", project);
        } catch (Exception ex) {
            Messages.showWarningDialog("配置保存失败:" + ex.getMessage(), "提示");
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    private void onSaveCurrent() {
        Object selectedItem = sinceComBox.getSelectedItem();
        if (Objects.isNull(selectedItem)) {
            return;
        }
        String key = selectedItem.toString();
        if(PROJECT_CONFIG.equals(key)){
            Messages.showWarningDialog("配置保存失败:" + key+"无法被保存", "提示");
            return;
        }
        if (SINCE_CONFIG.equals(key)) {
            Messages.InputDialog dialog = new Messages.InputDialog("请输入配置名称", "配置名称", Messages.getQuestionIcon(), "", new InputValidatorImpl());
            dialog.show();
            String configName = dialog.getInputString();
            if (StrUtil.isBlank(configName)) {
                return;
            }
            EasyQueryQueryPluginConfigData.saveConfigSince(configName, getConfigData());
            NotificationUtils.notifySuccess("保存成功", project);
            initSinceComBox(null);
        } else {
            int flag = Messages.showYesNoDialog("确定要覆盖当前配置[" + key + "]吗？", "提示", Messages.getQuestionIcon());
            if (MessageConstants.YES == flag) {
                EasyQueryConfig configData = getConfigData();
                EasyQueryQueryPluginConfigData.saveConfigSince(key, configData);
                initConfigData(configData);
                NotificationUtils.notifySuccess("保存成功", project);
            }
        }
    }

    private void onDelCurrent() {
        Object selectedItem = sinceComBox.getSelectedItem();
        if (Objects.isNull(selectedItem)) {
            return;
        }
        String key = selectedItem.toString();
        if (SINCE_CONFIG.equals(key)) {
            NotificationUtils.notifyWarning("请选择要删除的配置信息", "警告", project);
            return;
        } else {
            int flag = Messages.showYesNoDialog("确定要删除当前配置[" + key + "]吗？", "提示", Messages.getQuestionIcon());
            if (MessageConstants.YES == flag) {
                EasyQueryQueryPluginConfigData.delConfigSince(key);
                initConfigData(null);
                initSinceComBox(null);
                NotificationUtils.notifySuccess("删除成功", project);
            }
        }
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

    private Set<String> searchLike(String tableName, TableListCellRenderer cellRenderer) {
        Map<String, String> highlightKey = highlightKeyLike(tableName);
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
        Set<String> result = new HashSet<>();
        for (int i = 0; i < keyword.length(); i++) {
            char key = keyword.charAt(i);
            result.addAll(INVERTED_TABLE_INDEX.getOrDefault(key + "", Collections.emptySet()));
        }
        result = result.stream()
            .filter(el -> {
                return match(el, keyword);
            })
            .collect(Collectors.toSet());
        return result;
    }

    private boolean match(String el, String keyword) {
        try {

            int contentIndex = 0, keywordIndex = 0;
            int contentLen = el.length(), keywordLen = keyword.length();

            // 处理边界情况
            if (keywordLen == 0) return true; // keyword为空
            if (contentLen < keywordLen) return false; // content比keyword短

            while (contentIndex < contentLen && keywordIndex < keywordLen) {
                // 如果剩余content长度不足，提前终止
                if (contentLen - contentIndex < keywordLen - keywordIndex) {
                    return false;
                }
                // 当前字符匹配成功，移动keyword指针
                if (el.charAt(contentIndex) == keyword.charAt(keywordIndex)) {
                    keywordIndex++;
                }
                contentIndex++;
            }
            return keywordIndex == keywordLen;
        } catch (Exception ex) {

            for (int i = 0; i < keyword.length(); i++) {
                String key = keyword.charAt(i) + "" ;
                if (StringUtils.containsIgnoreCase(el, key)) {
                    el = el.replaceFirst(key, "");
                } else {
                    return false;
                }
            }
            return true;
        }
    }

    public Set<String> searchLike(String keyword) {
        if (StrUtil.isEmpty(keyword)) {
            return INVERTED_TABLE_INDEX.values().stream()
                .flatMap(el -> el.stream())
                .collect(Collectors.toSet());
        }
        Set<String> result = new HashSet<>();
        for (int i = 0; i < keyword.length(); i++) {
            char key = keyword.charAt(i);
            result.addAll(INVERTED_TABLE_INDEX.getOrDefault(key + "", Collections.emptySet()));
        }
        result = result.stream()
            .filter(el -> {
                if (StrUtil.isNotBlank(el)) {
                    return el.contains(keyword);
                }
                return false;
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

        Map<String, String> highlightMap = new HashMap<>();
        result.forEach(el -> {
            String finalKeyword = keyword;
            StringBuilder htmlText = new StringBuilder("<html>");
            for (int i = 0; i < el.length(); i++) {
                String key = el.charAt(i) + "" ;
                if (StringUtils.containsIgnoreCase(finalKeyword, key)) {
                    htmlText.append("<span style='color:#c60'>").append(key).append("</span>");
                    finalKeyword = finalKeyword.replaceFirst(key, "");
                    continue;
                }
                htmlText.append(key);
            }
            htmlText.append("</html>");
            highlightMap.put(el, htmlText.toString());
        });
        return highlightMap;
    }

    public Map<String, String> highlightKeyLike(String keyword) {

        Set<String> result = searchLike(keyword);
        if (StrUtil.isEmpty(keyword)) {
            return result.stream().collect(Collectors.toMap(el -> el, el -> el));
        }
        // 字符串排序

        Map<String, String> highlightMap = new HashMap<>();
        result.forEach(el -> {
            String finalKeyword = keyword;
            StringBuilder htmlText = new StringBuilder("<html>");
            for (int i = 0; i < el.length(); i++) {
                String key = el.charAt(i) + "" ;
                if (StringUtils.containsIgnoreCase(finalKeyword, key)) {
                    htmlText.append("<span style='color:#c60'>").append(key).append("</span>");
                    finalKeyword = finalKeyword.replaceFirst(key, "");
                    continue;
                }
                htmlText.append(key);
            }
            htmlText.append("</html>");
            highlightMap.put(el, htmlText.toString());
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

        boolean isMavenProject = MyModuleUtil.isMavenProject(modules[0]);
        for (JComboBox<String> modulesCombox : modulesComboxs) {
            modulesCombox.setRenderer(new ModuleComBoxRender());

            moduleMap = Arrays.stream(modules)
                .filter(module -> {
                    if (isMavenProject) {
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
}
