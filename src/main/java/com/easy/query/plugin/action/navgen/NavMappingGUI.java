package com.easy.query.plugin.action.navgen;

import cn.hutool.core.lang.Pair;
import com.google.common.collect.Lists;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Nav 注解生成GUI
 * @author link2fun
 */
public class NavMappingGUI extends JDialog {
    private NavMappingPanel mappingPanel;
    private Consumer<NavMappingRelation> confirmCallback;

//    public NavMappingGUI(String[] availableEntities, String currentEntityName,
//            Map<String, String[]> entityAttributesMap) {
//        this(availableEntities, currentEntityName, null, entityAttributesMap, null);
//    }
//
//    public NavMappingGUI(String[] availableEntities, String currentEntityName,
//            Map<String, String[]> entityAttributesMap, Consumer<NavMappingRelation> confirmCallback) {
//        this(availableEntities, currentEntityName, null, entityAttributesMap, confirmCallback);
//    }

    public NavMappingGUI(List<Pair<String,String>> availableEntities, String currentEntityName,
            String defaultTargetEntity, Map<String, List<Pair<String,String>>> entityAttributesMap,
            Consumer<NavMappingRelation> confirmCallback) {
        super();
        setTitle("映射关系");
        setModal(true);
        this.confirmCallback = confirmCallback;
        initComponents(availableEntities, currentEntityName, defaultTargetEntity, entityAttributesMap);
    }

    private void initComponents(List<Pair<String,String>> availableEntities, String currentEntityName,
            String defaultTargetEntity, Map<String, List<Pair<String,String>>> entityAttributesMap) {
        setLayout(new BorderLayout());
        mappingPanel = new NavMappingPanel(availableEntities, currentEntityName, defaultTargetEntity, 
                entityAttributesMap, confirmCallback);
        add(mappingPanel, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            List<Pair<String, String>> entities = Lists.newArrayList(
                    Pair.of("com.easy.query.plugin.action.navgen.SysUser", "用户"),
                    Pair.of("com.easy.query.plugin.action.navgen.SysUserRole", "用户角色关联"),
                    Pair.of("com.easy.query.plugin.action.navgen.SysRole", "角色")
            );
            String currentEntity = "com.easy.query.plugin.action.navgen.SysUser";
            String targetEntity = "com.easy.query.plugin.action.navgen.SysUserRole";

            Map<String, List<Pair<String,String>>> entityAttributesMap = new HashMap<>();
            entityAttributesMap.put("com.easy.query.plugin.action.navgen.SysUser", Lists.newArrayList(
                    Pair.of("id", "ID"),
                    Pair.of("loginName", "登录名"),
                    Pair.of("realName", "真实姓名")
            ));
            entityAttributesMap.put("com.easy.query.plugin.action.navgen.SysUserRole", Lists.newArrayList(
                    Pair.of("id", "ID"),
                    Pair.of("userId", "用户ID"),
                    Pair.of("roleId", "角色ID")
            ));
            entityAttributesMap.put("com.easy.query.plugin.action.navgen.SysRole", Lists.newArrayList(
                    Pair.of("id", "ID"),
                    Pair.of("roleCode", "角色编码"),
                    Pair.of("roleName", "角色名称")
            ));

            NavMappingGUI gui = new NavMappingGUI(entities, currentEntity,targetEntity, entityAttributesMap,null);
            gui.setVisible(true);
        });
    }
}