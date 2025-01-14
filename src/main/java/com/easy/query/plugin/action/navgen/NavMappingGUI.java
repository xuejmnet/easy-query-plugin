package com.easy.query.plugin.action.navgen;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class NavMappingGUI extends JDialog {
    private NavMappingPanel mappingPanel;

    public NavMappingGUI(String[] availableEntities, String currentEntityName,
            Map<String, String[]> entityAttributesMap) {
        super();
        setTitle("映射关系");
        setModal(true);
        initComponents(availableEntities, currentEntityName, entityAttributesMap);
    }

    private void initComponents(String[] availableEntities, String currentEntityName,
            Map<String, String[]> entityAttributesMap) {
        setLayout(new BorderLayout());
        mappingPanel = new NavMappingPanel(availableEntities, currentEntityName, entityAttributesMap);
        add(mappingPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String[] entities = { "SysUser", "SysUserRole", "SysRole" };
            String currentEntity = "SysUser";

            Map<String, String[]> entityAttributesMap = new HashMap<>();
            entityAttributesMap.put("SysUser", new String[] { "id", "loginName", "realName" });
            entityAttributesMap.put("SysUserRole", new String[] { "id", "userId", "roleId" });
            entityAttributesMap.put("SysRole", new String[] { "id", "roleCode", "roleName" });

            NavMappingGUI gui = new NavMappingGUI(entities, currentEntity, entityAttributesMap);
            gui.setVisible(true);
        });
    }
}