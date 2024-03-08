package com.easy.query.plugin.windows;

import com.easy.query.plugin.core.entity.ClassNode;
import com.easy.query.plugin.core.entity.struct.StructDTOContext;
import com.easy.query.plugin.core.entity.struct.StructDTOEntityContext;
import com.easy.query.plugin.core.render.EntityListCellRenderer;
import com.easy.query.plugin.core.util.DialogUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.easy.query.plugin.core.util.StructDTOUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.ui.DocumentAdapter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
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
    private final StructDTOEntityContext structDTOEntityContext;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JList<String> entityList;
    private JTextField searchEntity;
    Map<String, Set<String>> INVERTED_ENTITY_INDEX = new HashMap<>();

    public EntitySelectDialog(StructDTOEntityContext structDTOEntityContext) {
        this.structDTOEntityContext = structDTOEntityContext;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        setSize(800, 900);
        setTitle("Struct DTO Entity Select");
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

        DefaultListModel<String> model = new DefaultListModel<>();
        Map<String, PsiClass> entityMap = structDTOEntityContext.getEntityClass();
        // tableNameSet按照字母降序
        List<String> entityNameList = new ArrayList<>(entityMap.keySet());
        Collections.sort(entityNameList);
        model.addAll(entityNameList);
        entityList.setModel(model);
        EntityListCellRenderer cellRenderer = new EntityListCellRenderer(entityMap);
        entityList.setCellRenderer(cellRenderer);


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
                .filter(el -> {
                    for (int i = 0; i < keyword.length(); i++) {
                        String key = keyword.charAt(i) + "";
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

    /**
     * 传入表名集合，建立倒排索引
     *
     * @param entityNameList 表名
     */
    public void initEntityIndexText(Collection<String> entityNameList) {
        for (String tableName : entityNameList) {
            for (int i = 0; i < tableName.length(); i++) {
                char word = tableName.charAt(i);
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
        Project project = structDTOEntityContext.getProject();
        Map<String, PsiClass> entityClass = structDTOEntityContext.getEntityClass();
        PsiClass psiClass = entityClass.get(entityName);
        if (psiClass == null) {
            Messages.showWarningDialog("无法找到对象的类型:" + entityName, "提示");
            return;
        }
        Map<String, Map<String, ClassNode>> entityProps = new HashMap<>();
        List<ClassNode> classNodes = new ArrayList<>();
        LinkedHashSet<String> imports = new LinkedHashSet<>();
        StructDTOUtil.parseClassList(project, entityName, psiClass, structDTOEntityContext.getEntityClass(), entityProps, classNodes, imports);
        StructDTOContext structDTOContext = new StructDTOContext(project, structDTOEntityContext.getPath(), structDTOEntityContext.getPackageName(), structDTOEntityContext.getModule(), entityProps);
        structDTOContext.getImports().addAll(imports);
        StructDTODialog structDTODialog = new StructDTODialog(structDTOContext,classNodes);
        structDTODialog.setVisible(true);
        if(!structDTOContext.isSuccess()){
            return;
        }
        dispose();
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
