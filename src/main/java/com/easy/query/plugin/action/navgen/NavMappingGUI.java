package com.easy.query.plugin.action.navgen;

import javax.swing.*;
import java.awt.*;

public class NavMappingGUI extends JDialog {
    private NavMappingPanel mappingPanel;

    public NavMappingGUI(String[] availableEntities, String currentEntityName) {
        super();
        setTitle("映射关系");
        setModal(true);
        initComponents(availableEntities, currentEntityName);
    }

    private void initComponents(String[] availableEntities, String currentEntityName) {
        setLayout(new BorderLayout());

        // 使用传入的实体列表和当前实体名称创建NavMappingPanel
        mappingPanel = new NavMappingPanel(availableEntities, currentEntityName);
        add(mappingPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(getOwner());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NavMappingGUI gui = new NavMappingGUI(
                    new String[] { "Entity1", "Entity2", "Entity3" },
                    "CurrentEntity");
            gui.setVisible(true);
        });
    }
}