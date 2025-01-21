package com.easy.query.plugin.action.navgen;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Nav 注解生成GUI
 * @author link2fun
 */
public class NavMappingGUI extends JDialog {
    private NavMappingPanel mappingPanel;
    private Consumer<NavMappingRelation> confirmCallback;

    public NavMappingGUI(String[] availableEntities, String currentEntityName,
            Map<String, String[]> entityAttributesMap) {
        this(availableEntities, currentEntityName, entityAttributesMap, null);
    }

    public NavMappingGUI(String[] availableEntities, String currentEntityName,
            Map<String, String[]> entityAttributesMap, Consumer<NavMappingRelation> confirmCallback) {
        super();
        setTitle("映射关系");
        setModal(true);
        this.confirmCallback = confirmCallback;
        initComponents(availableEntities, currentEntityName, entityAttributesMap);
    }

    private void initComponents(String[] availableEntities, String currentEntityName,
            Map<String, String[]> entityAttributesMap) {
        setLayout(new BorderLayout());
        mappingPanel = new NavMappingPanel(availableEntities, currentEntityName, entityAttributesMap,confirmCallback);
        add(mappingPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String[] entities = { "com.easy.query.plugin.action.navgen.SysUser", "com.easy.query.plugin.action.navgen.SysUserRole", "com.easy.query.plugin.action.navgen.SysRole" };
            String currentEntity = "com.easy.query.plugin.action.navgen.SysUser";

            Map<String, String[]> entityAttributesMap = new HashMap<>();
            entityAttributesMap.put("com.easy.query.plugin.action.navgen.SysUser", new String[] { "id", "loginName", "realName", "loginName", "realName", "loginName", "realName", "loginName", "realName", "loginName", "realName", "loginName", "realName", "loginName", "realName" });
            entityAttributesMap.put("com.easy.query.plugin.action.navgen.SysUserRole", new String[] { "id", "userId", "roleId" });
            entityAttributesMap.put("com.easy.query.plugin.action.navgen.SysRole", new String[] { "id", "roleCode", "roleName" });

            NavMappingGUI gui = new NavMappingGUI(entities, currentEntity, entityAttributesMap);
            gui.setVisible(true);
        });
    }
}