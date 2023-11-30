package com.easy.query.plugin.windows;

import com.easy.query.plugin.core.entity.TableInfo;
import com.easy.query.plugin.core.render.TableListCellRenderer;
import com.easy.query.plugin.core.util.DialogUtil;
import com.easy.query.plugin.core.util.ObjectUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.easy.query.plugin.core.util.TableUtils;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.ui.DocumentAdapter;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EntityTableGenerateDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBox selectAll;
    private JTextField tableSearch;
    private JList<String> tableList;


    Project project;
    Map<String, TableInfo> tableInfoMap;
    List<String> tableNameList;
    Map<String, Set<String>> INVERTED_INDEX = new HashMap<>();

    public EntityTableGenerateDialog(AnActionEvent actionEvent) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setSize(500, 800);
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
        project= actionEvent.getProject();

        tableInfoMap = TableUtils.getAllTables(actionEvent)
                .stream().collect(Collectors.toMap(TableInfo::getName, o->o));

        DefaultListModel<String> model = new DefaultListModel<>();
        // tableNameSet按照字母降序
        tableNameList = new ArrayList<>(tableInfoMap.keySet());
        Collections.sort(tableNameList);
        model.addAll(tableNameList);
        tableList.setModel(model);
        TableListCellRenderer cellRenderer = new TableListCellRenderer(tableInfoMap);
        tableList.setCellRenderer(cellRenderer);


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
                if(StringUtils.isNotBlank(tableName)){
                    if(!tableList.isSelectionEmpty()){
                        tableList.clearSelection();
                    }
                    Set<String> search = search(tableName.trim(), cellRenderer);
                    model.removeAllElements();
                    model.addAll(search);
                }
            }
        });
        initIndexText(tableNameList);
    }

    /**
     * 传入表名集合，建立倒排索引
     *
     * @param tableNames 表名
     */
    public void initIndexText(Collection<String> tableNames) {
        for (String tableName : tableNames) {
            for (int i = 0; i < tableName.length(); i++) {
                char word = tableName.charAt(i);
                INVERTED_INDEX.computeIfAbsent((word + "").toLowerCase(), k -> new HashSet<>()).add(tableName);
            }
        }
    }
    private void onOK() {
        // add your code here
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }


    private void createUIComponents() {
        // TODO: place custom component creation code here
    }


    private  Set<String> search(String tableName, TableListCellRenderer cellRenderer) {
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
    public  Set<String> search(String keyword) {
        if (StrUtil.isEmpty(keyword)) {
            return INVERTED_INDEX.values().stream()
                    .flatMap(el -> el.stream())
                    .collect(Collectors.toSet());
        }
        keyword = keyword.toLowerCase();
        Set<String> result = new HashSet<>();
        for (int i = 0; i < keyword.length(); i++) {
            char key = keyword.charAt(i);
            result.addAll(INVERTED_INDEX.getOrDefault(key + "", Collections.emptySet()));
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
    public  Map<String, String> highlightKey(String keyword) {

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
}
