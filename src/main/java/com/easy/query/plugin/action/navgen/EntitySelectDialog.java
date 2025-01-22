package com.easy.query.plugin.action.navgen;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.stream.Stream;
/**
 * Nav 注解生成GUI
 * @author link2fun
 */
public class EntitySelectDialog extends JDialog {
    private String selectedEntity = null;
    private JList<String> entityList;
    private JTextField filterField;
    private final String[] allEntities;
    private DefaultListModel<String> listModel;

    public EntitySelectDialog(Window owner, String title, String[] entities) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.allEntities = entities;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        setSize(600, 400);
        setLocationRelativeTo(getOwner());

        // 创建筛选面板
        JPanel filterPanel = new JPanel(new BorderLayout());
        filterPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // 创建筛选输入框
        filterField = new JTextField();
        filterField.setPreferredSize(new Dimension(200, 30));
        filterPanel.add(filterField, BorderLayout.CENTER);

        // 添加筛选监听器
        filterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterEntities();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterEntities();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterEntities();
            }
        });

        add(filterPanel, BorderLayout.NORTH);

        // 创建实体列表
        listModel = new DefaultListModel<>();
        entityList = new JList<>(listModel);
        entityList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        entityList.setBorder(new EmptyBorder(5, 5, 5, 5));

        // 添加所有实体到列表
        Arrays.stream(allEntities).forEach(listModel::addElement);

        // 添加双击监听器
        entityList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = entityList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        selectedEntity = listModel.getElementAt(index);
                        dispose();
                    }
                }
            }
        });

        // 设置列表字体和行高
        entityList.setFont(new Font("宋体", Font.PLAIN, 14));
        entityList.setFixedCellHeight(25);

        // 添加滚动面板
        JScrollPane scrollPane = new JScrollPane(entityList);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(scrollPane, BorderLayout.CENTER);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        JButton confirmButton = new JButton("确认");
        confirmButton.addActionListener(e -> {
            selectedEntity = entityList.getSelectedValue();
            dispose();
        });

        JButton cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 设置默认按钮
        getRootPane().setDefaultButton(confirmButton);
    }

    private void filterEntities() {
        String filterText = filterField.getText().toLowerCase().trim();
        listModel.clear();

        Stream.of(allEntities)
                .filter(entity -> entity.toLowerCase().contains(filterText))
                .forEach(listModel::addElement);

        if (listModel.getSize() > 0) {
            entityList.setSelectedIndex(0);
        }
    }

    public String getSelectedEntity() {
        return selectedEntity;
    }
}