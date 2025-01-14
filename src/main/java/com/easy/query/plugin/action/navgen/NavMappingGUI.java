package com.easy.query.plugin.action.navgen;

import javax.swing.*;
import java.awt.*;

public class NavMappingGUI extends JDialog {
    private NavMappingPanel mappingPanel;

    public NavMappingGUI(String[] availableEntities) {
        super();
        setTitle("映射关系");
        setModal(true);
        initComponents(availableEntities);
    }

    private void initComponents(String[] availableEntities) {
        setLayout(new BorderLayout());

        // 使用传入的实体列表创建NavMappingPanel
        mappingPanel = new NavMappingPanel(availableEntities);
        add(mappingPanel, BorderLayout.CENTER);

        // ... 其他初始化代码 ...

        pack();
        setLocationRelativeTo(getOwner());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NavMappingGUI gui = new NavMappingGUI(
                    new String[] { "Entity1", "Entity2", "Entity3" });
            gui.setVisible(true);
        });
    }
}