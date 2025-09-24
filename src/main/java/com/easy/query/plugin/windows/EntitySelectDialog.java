package com.easy.query.plugin.windows;

import cn.hutool.core.util.NumberUtil;
import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.entity.ClassNode;
import com.easy.query.plugin.core.entity.struct.StructDTOContext;
import com.easy.query.plugin.core.entity.struct.StructDTOEntityContext;
import com.easy.query.plugin.core.persistent.EasyQueryQueryPluginConfigData;
import com.easy.query.plugin.core.render.EntityListCellRenderer;
import com.easy.query.plugin.core.util.DialogUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.StrUtil;
import com.easy.query.plugin.core.util.StructDTOUtil;
import com.easy.query.plugin.core.validator.InputAnyValidatorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.ui.DocumentAdapter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EntitySelectDialog extends JDialog {
    private StructDTOEntityContext structDTOEntityContext;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JList<String> entityList;
    private JTextField searchEntity;
    private JButton settingBtn;
    Map<String, Set<String>> INVERTED_ENTITY_INDEX = new HashMap<>();

    public EntitySelectDialog(StructDTOEntityContext structDTOEntityContext) {
        this.structDTOEntityContext = structDTOEntityContext;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
// 获取屏幕的大小
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(800, (int) screenSize.getWidth() - 50), (int) Math.min(900, (int) (screenSize.getHeight() * 0.9)));
        setTitle("Struct DTO Entity Select");
        DialogUtil.centerShow(this);
        Project project = structDTOEntityContext.getProject();
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
        settingBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                EasyQueryConfig config = EasyQueryQueryPluginConfigData.getAllEnvStructDTOIgnore(new EasyQueryConfig());
                if (config.getConfig() == null) {
                    config.setConfig(new HashMap<>());
                }
                String projectName = project.getName();
                String setting = config.getConfig().get(projectName);

                ModelTemplateEditorDialog modelTemplateEditorDialog = new ModelTemplateEditorDialog(project, setting, newTemplate -> {
                    config.getConfig().put(projectName, newTemplate);
                    EasyQueryQueryPluginConfigData.saveAllEnvEnvStructDTOIgnore(config);
                    NotificationUtils.notifySuccess("保存成功", project);
                });
                modelTemplateEditorDialog.setVisible(true);
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

        DefaultListModel<String> model = new DefaultListModel<>();
        Map<String, PsiClass> entityMap = structDTOEntityContext.getEntityClass();
        // tableNameSet按照字母降序
        List<String> entityNameList = new ArrayList<>(entityMap.keySet());
        Collections.sort(entityNameList);
        model.addAll(entityNameList);
        entityList.setModel(model);
        EntityListCellRenderer cellRenderer = new EntityListCellRenderer(entityMap);
        entityList.setCellRenderer(cellRenderer);
        entityList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {

                    String entityName = entityList.getSelectedValue();
                    ok0(entityName);
                } else {
                    super.mouseClicked(e);
                }
            }
        });


        searchEntity.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                String entityName = searchEntity.getText();
                if (StringUtils.isNotBlank(entityName)) {
                    if (!entityList.isSelectionEmpty()) {
                        entityList.clearSelection();
                    }
                    Set<String> search = search(entityName.trim(), cellRenderer);
                    model.removeAllElements();
                    model.addAll(search);
                } else {
                    cellRenderer.setHighlightKey(new HashMap<>());
                    model.removeAllElements();
                    model.addAll(entityNameList);
                }
            }
        });

        initEntityIndexText(entityNameList);
    }

    private Set<String> search(String entityName, EntityListCellRenderer cellRenderer) {
        Map<String, String> highlightKey = highlightKey(entityName);
        cellRenderer.setSearchTableName(entityName);
        cellRenderer.setHighlightKey(highlightKey);
        return highlightKey.keySet();
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
                String packageName = cn.hutool.core.util.StrUtil.subBefore(el, ".", true);
                String entityName = cn.hutool.core.util.StrUtil.subAfter(el, ".", true);
                String finalKeyword = keyword;
                String htmlText = "<html>";
                htmlText += packageName + ".";
                for (int i = 0; i < entityName.length(); i++) {
                    String key = entityName.charAt(i) + "";
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
     * 搜索
     *
     * @param keyword 关键字
     * @return {@code Set<String>}
     */
    public Set<String> search(String keyword) {
        if (StrUtil.isEmpty(keyword)) {
            return INVERTED_ENTITY_INDEX.values().stream()
                .flatMap(el -> el.stream())
                .collect(Collectors.toSet());
        }
        Set<String> result = new HashSet<>();
        for (int i = 0; i < keyword.length(); i++) {
            char key = keyword.charAt(i);
            result.addAll(INVERTED_ENTITY_INDEX.getOrDefault(key + "", Collections.emptySet()));
        }
        result = result.stream()
//                .map(o-> cn.hutool.core.util.StrUtil.subAfter(o,".",true))
            .filter(el -> {
                String elEntity = cn.hutool.core.util.StrUtil.subAfter(el, ".", true);
                for (int i = 0; i < keyword.length(); i++) {
                    String key = keyword.charAt(i) + "";
                    if (StringUtils.containsIgnoreCase(elEntity, key)) {
                        elEntity = elEntity.replaceFirst(key, "");
                    } else {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toSet());
        return result;
    }

    /**
     * 传入表名集合，建立倒排索引
     *
     * @param entityNameList 表名
     */
    public void initEntityIndexText(Collection<String> entityNameList) {
        for (String tableName : entityNameList) {
            String entityName = cn.hutool.core.util.StrUtil.subAfter(tableName, ".", true);
            for (int i = 0; i < entityName.length(); i++) {
                char word = entityName.charAt(i);
                INVERTED_ENTITY_INDEX.computeIfAbsent((word + "").toLowerCase(), k -> new HashSet<>()).add(tableName);
            }
        }
    }

    private void onOK() {
        // add your code here

        List<String> selectedEntityList = entityList.getSelectedValuesList();
        if (CollectionUtils.isEmpty(selectedEntityList)) {
            Messages.showWarningDialog("请选择要生成的表", "提示");
            return;
        }
        if (selectedEntityList.size() != 1) {
            Messages.showWarningDialog("请选择要生成的单个对象", "提示");
            return;
        }

        String entityName = selectedEntityList.get(0);
        boolean oked = ok0(entityName);
        if (!oked) {
            return;
        }
        dispose();
    }

    public boolean ok0(String entityName) {
        Project project = structDTOEntityContext.getProject();
        Map<String, PsiClass> entityClass = structDTOEntityContext.getEntityClass();
        PsiClass psiClass = entityClass.get(entityName);
        if (psiClass == null) {
            Messages.showWarningDialog("无法找到对象的类型:" + entityName, "提示");
            return false;
        }
//        Set<String> ignoreColumns = getIgnoreColumns(project);

        Messages.InputDialog dialog = new Messages.InputDialog("请输入树形深度,无限级输入-1", "树形深度", Messages.getQuestionIcon(), "5", new InputAnyValidatorImpl());

        dialog.show();
        if (!dialog.isOK()) {
            return false;
        }
        String settingVal = dialog.getInputString();
        if (StrUtil.isBlank(settingVal)) {
            Messages.showWarningDialog("无法读取树形深度", "提示");
            return false;
        }
        boolean integer = NumberUtil.isInteger(settingVal);
        if (!integer) {
            Messages.showWarningDialog("树形深度:[" + settingVal + "]只能是数字", "提示");
            return false;
        }
        int deepMax = Integer.parseInt(settingVal);


        Map<String, Map<String, ClassNode>> entityProps = new HashMap<>();
        List<ClassNode> classNodes = new ArrayList<>();
        LinkedHashSet<String> imports = new LinkedHashSet<>();
        StructDTOUtil.parseClassList(deepMax, project, entityName, psiClass, structDTOEntityContext.getEntityClass(), entityProps, classNodes, imports, new HashSet<>());
        StructDTOContext structDTOContext = new StructDTOContext(project, structDTOEntityContext.getPath(), structDTOEntityContext.getPackageName(), structDTOEntityContext.getModule(), entityProps);
        structDTOContext.getImports().addAll(imports);

        // 传递 DTO className 到下一个窗口上下文
        String dtoClassName = structDTOEntityContext.getDtoClassName();
        PsiClass dtoPsiClass = structDTOEntityContext.getDtoPsiClass();
        structDTOContext.setDtoClassName(dtoClassName);
        structDTOContext.setDtoPsiClass(dtoPsiClass);

        StructDTODialog structDTODialog = new StructDTODialog(structDTOContext, classNodes);

        structDTODialog.setVisible(true);

        if (!structDTOContext.isSuccess()) {
            return false;
        }
        return true;
    }


    private Set<String> getIgnoreColumns(Project project) {

        EasyQueryConfig allEnvStructDTOIgnore = EasyQueryQueryPluginConfigData.getAllEnvStructDTOIgnore(null);
        if (allEnvStructDTOIgnore != null) {
            Map<String, String> config = allEnvStructDTOIgnore.getConfig();
            if (config != null) {
                String settingVal = config.get(project.getName());
                if (cn.hutool.core.util.StrUtil.isNotBlank(settingVal)) {
                    String[] shortNames = settingVal.split(",");
                    return Arrays.stream(shortNames).collect(Collectors.toSet());

                }
            }
        }
        return new HashSet<>(0);
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
//    public static void main(String[] args) {
//        EntitySelectDialog dialog = new EntitySelectDialog();
//        dialog.pack();
//        dialog.setVisible(true);
//        System.exit(0);
//    }
}
