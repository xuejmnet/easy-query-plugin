package com.easy.query.plugin.action.navgen;

import lombok.Getter;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.intellij.openapi.command.WriteCommandAction;

public class NavMappingPanel extends JPanel {
    private JComboBox<String> mappingTypeCombo;
    private List<AttributeGroup> attributeGroups;
    private JButton addGroupButton;
    private JButton confirmButton;
    private JPanel attributesPanel;
    private static final int INITIAL_GROUP_Y = 220;
    private static final int GROUP_VERTICAL_GAP = 70;
    private static final int MANY_TO_MANY_EXTRA_GAP = 30;
    private JComboBox<String> entitySelector;
    private JComboBox<String> middleEntitySelector;
    private static final int ENTITY_REGION_START_Y = 180;
    private JLabel middleEntityLabel;
    private JLabel targetEntityLabel;
    private JButton selectMiddleEntityButton;
    private JButton selectTargetEntityButton;
    private final String[] availableEntities;
    @Getter
    private final String currentEntityName;
    private final Map<String, String[]> entityAttributesMap;
    private final Consumer<NavMappingRelation> confirmCallback;

    public static class MappingData {
        private String mappingType;
        private List<AttributeMapping> attributeMappings;

        public MappingData(String mappingType, List<AttributeMapping> attributeMappings) {
            this.mappingType = mappingType;
            this.attributeMappings = attributeMappings;
        }

        public String getMappingType() {
            return mappingType;
        }

        public List<AttributeMapping> getAttributeMappings() {
            return attributeMappings;
        }
    }

    public static class AttributeMapping {
        private String sourceAttribute;
        private String middleSourceAttribute;
        private String middleTargetAttribute;
        private String targetAttribute;

        public AttributeMapping(String sourceAttribute, String middleSourceAttribute,
                String middleTargetAttribute, String targetAttribute) {
            this.sourceAttribute = sourceAttribute;
            this.middleSourceAttribute = middleSourceAttribute;
            this.middleTargetAttribute = middleTargetAttribute;
            this.targetAttribute = targetAttribute;
        }

        public String getSourceAttribute() {
            return sourceAttribute;
        }

        public String getMiddleSourceAttribute() {
            return middleSourceAttribute;
        }

        public String getMiddleTargetAttribute() {
            return middleTargetAttribute;
        }

        public String getTargetAttribute() {
            return targetAttribute;
        }
    }

    private class AttributeGroup {
        JComboBox<String> sourceAttr;
        JComboBox<String> middleAttr;
        JComboBox<String> middleTargetAttr;
        JComboBox<String> targetAttr;
        JButton deleteButton;
        int yPosition;

        public AttributeGroup(int yPos) {
            this.yPosition = yPos;
            initializeComponents();
        }

        private void initializeComponents() {
            String[] sourceAttributes = entityAttributesMap.getOrDefault(currentEntityName,
                    new String[] { "当前实体属性" });

            String[] mappingAttributes = { "映射实体属性" };
            String[] targetAttributes = { "目标实体属性" };

            sourceAttr = new JComboBox<>(sourceAttributes);
            middleAttr = new JComboBox<>(mappingAttributes);
            middleTargetAttr = new JComboBox<>(mappingAttributes);
            targetAttr = new JComboBox<>(targetAttributes);
            deleteButton = new JButton("删除");

            sourceAttr.setMaximumRowCount(15);
            middleAttr.setMaximumRowCount(15);
            middleTargetAttr.setMaximumRowCount(15);
            targetAttr.setMaximumRowCount(15);

            sourceAttr.setBounds(50, yPosition, 150, 600);
            middleAttr.setBounds(350, yPosition, 150, 600);
            middleTargetAttr.setBounds(350, yPosition + 50, 150, 600);
            targetAttr.setBounds(650, yPosition, 150, 600);
            deleteButton.setBounds(820, yPosition, 60, 30);

            configureComboBoxStyle(sourceAttr, middleAttr, middleTargetAttr, targetAttr);
            deleteButton.setVisible(false);
        }

        public void updateAttributes(String middleEntity, String targetEntity) {
            if (middleEntity != null && !middleEntity.isEmpty()) {
                String[] middleAttributes = entityAttributesMap.getOrDefault(middleEntity,
                        new String[] { "映射实体属性" });
                updateComboBoxItems(middleAttr, middleAttributes);
                updateComboBoxItems(middleTargetAttr, middleAttributes);
            }

            if (targetEntity != null && !targetEntity.isEmpty()) {
                String[] targetAttributes = entityAttributesMap.getOrDefault(targetEntity,
                        new String[] { "目标实体属性" });
                updateComboBoxItems(targetAttr, targetAttributes);
            }
        }

        private void updateComboBoxItems(JComboBox<String> comboBox, String[] items) {
            String selected = (String) comboBox.getSelectedItem();
            comboBox.removeAllItems();
            for (String item : items) {
                comboBox.addItem(item);
            }
            if (selected != null && Arrays.asList(items).contains(selected)) {
                comboBox.setSelectedItem(selected);
            }
        }
    }

    public NavMappingPanel(String[] availableEntities, String currentEntityName,
            Map<String, String[]> entityAttributesMap, Consumer<NavMappingRelation> confirmCallback) {
        this.availableEntities = availableEntities;
        this.currentEntityName = currentEntityName;
        this.entityAttributesMap = entityAttributesMap;
        this.confirmCallback = confirmCallback;
        initializePanel();
    }

    private void initializePanel() {
        setLayout(null);
        setPreferredSize(new Dimension(900, 600));

        JLabel mappingTypeLabel = new JLabel("映射类型:");
        mappingTypeLabel.setBounds(50, 20, 100, 30);
        add(mappingTypeLabel);

        mappingTypeCombo = new JComboBox<>(new String[] { "OneToOne", "OneToMany", "ManyToOne", "ManyToMany" });
        mappingTypeCombo.setSelectedItem("OneToOne");
        mappingTypeCombo.setBounds(150, 20, 150, 30);
        mappingTypeCombo.addActionListener(e -> updateMappingDisplay());
        add(mappingTypeCombo);

        JLabel currentEntityLabel = new JLabel("当前实体:");
        currentEntityLabel.setBounds(50, 55, 100, 30);
        add(currentEntityLabel);

        JLabel currentEntityDisplay = new JLabel(currentEntityName);
        currentEntityDisplay.setBounds(150, 55, 550, 30);
        currentEntityDisplay.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        currentEntityDisplay.setOpaque(true);
        currentEntityDisplay.setBackground(Color.WHITE);
        add(currentEntityDisplay);

        JLabel middleLabel = new JLabel("中间实体(可选):");
        middleLabel.setBounds(50, 90, 100, 30);
        add(middleLabel);

        middleEntityLabel = new JLabel("");
        middleEntityLabel.setBounds(150, 90, 550, 30);
        add(middleEntityLabel);

        selectMiddleEntityButton = new JButton("选择中间实体");
        selectMiddleEntityButton.setBounds(700, 90, 120, 30);
        selectMiddleEntityButton.addActionListener(e -> selectMiddleEntity());
        add(selectMiddleEntityButton);

        JLabel targetLabel = new JLabel("目标实体(必须):");
        targetLabel.setBounds(50, 125, 100, 30);
        add(targetLabel);

        targetEntityLabel = new JLabel("");
        targetEntityLabel.setBounds(150, 125, 550, 30);
        add(targetEntityLabel);

        selectTargetEntityButton = new JButton("选择目标实体");
        selectTargetEntityButton.setBounds(700, 125, 120, 30);
        selectTargetEntityButton.addActionListener(e -> selectTargetEntity());
        add(selectTargetEntityButton);

        addGroupButton = new JButton("添加映射组");
        addGroupButton.setBounds(650, 20, 100, 30);
        add(addGroupButton);
        addGroupButton.addActionListener(e -> addAttributeGroup());

        confirmButton = new JButton("确认");
        confirmButton.setBounds(760, 20, 80, 30);
        add(confirmButton);
        confirmButton.addActionListener(e -> handleConfirm());

        attributeGroups = new ArrayList<>();
        addAttributeGroup();
        updateMappingDisplay();
    }

    private void initComponents() {
    }

    private void addAttributeGroup() {
        String selectedType = (String) mappingTypeCombo.getSelectedItem();
        boolean isManyToMany = "ManyToMany".equals(selectedType);

        int yPos = INITIAL_GROUP_Y;
        if (attributeGroups.size() > 0) {
            AttributeGroup lastGroup = attributeGroups.get(attributeGroups.size() - 1);
            yPos = lastGroup.yPosition + GROUP_VERTICAL_GAP;
            if (isManyToMany) {
                yPos += MANY_TO_MANY_EXTRA_GAP;
            }
        }

        AttributeGroup group = new AttributeGroup(yPos);
        attributeGroups.add(group);

        add(group.sourceAttr);
        add(group.middleAttr);
        add(group.middleTargetAttr);
        add(group.targetAttr);
        add(group.deleteButton);

        if (attributeGroups.size() > 1) {
            group.deleteButton.setVisible(true);
            group.deleteButton.addActionListener(e -> removeAttributeGroup(group));
        }

        updateMappingDisplay();
        revalidate();
        repaint();
    }

    private void removeAttributeGroup(AttributeGroup group) {
        remove(group.sourceAttr);
        remove(group.middleAttr);
        remove(group.middleTargetAttr);
        remove(group.targetAttr);
        remove(group.deleteButton);

        attributeGroups.remove(group);

        String selectedType = (String) mappingTypeCombo.getSelectedItem();
        boolean isManyToMany = "ManyToMany".equals(selectedType);

        int newY = INITIAL_GROUP_Y;
        for (AttributeGroup currentGroup : attributeGroups) {
            updateGroupPosition(currentGroup, newY);
            newY += GROUP_VERTICAL_GAP;
            if (isManyToMany) {
                newY += MANY_TO_MANY_EXTRA_GAP;
            }
        }

        revalidate();
        repaint();
    }

    private void updateGroupPosition(AttributeGroup group, int newY) {
        group.yPosition = newY;
        group.sourceAttr.setBounds(50, newY, 150, 30);

        String selectedType = (String) mappingTypeCombo.getSelectedItem();
        boolean isManyToMany = "ManyToMany".equals(selectedType);
        boolean hasMiddleEntity = isManyToMany && !middleEntityLabel.getText().isEmpty();

        if (isManyToMany && hasMiddleEntity) {
            group.middleAttr.setBounds(350, newY, 150, 30);
            group.middleTargetAttr.setBounds(350, newY + 35, 150, 30);
            group.targetAttr.setBounds(650, newY + 35, 150, 30);
            group.deleteButton.setBounds(830, newY + 15, 60, 30);
        } else {
            group.targetAttr.setBounds(650, newY, 150, 30);
            group.deleteButton.setBounds(830, newY, 60, 30);
        }
    }

    private void updateMappingDisplay() {
        String selectedType = (String) mappingTypeCombo.getSelectedItem();
        boolean isManyToMany = "ManyToMany".equals(selectedType);
        boolean hasMiddleEntity = isManyToMany && !middleEntityLabel.getText().isEmpty();

        middleEntityLabel.setVisible(isManyToMany);
        selectMiddleEntityButton.setVisible(isManyToMany);

        int newY = INITIAL_GROUP_Y;
        for (AttributeGroup group : attributeGroups) {
            group.middleAttr.setVisible(hasMiddleEntity);
            group.middleTargetAttr.setVisible(hasMiddleEntity);

            updateGroupPosition(group, newY);
            newY += GROUP_VERTICAL_GAP;
            if (hasMiddleEntity) {
                newY += MANY_TO_MANY_EXTRA_GAP;
            }
        }

        repaint();
    }

    private void configureComboBoxStyle(JComboBox<?>... comboBoxes) {
        for (JComboBox<?> box : comboBoxes) {
            box.setBackground(Color.WHITE);
            box.setFont(new Font("宋体", Font.PLAIN, 12));
            box.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            box.setUI(new BasicComboBoxUI() {
                @Override
                protected JButton createArrowButton() {
                    JButton button = super.createArrowButton();
                    button.setBackground(Color.WHITE);
                    return button;
                }
            });
            box.setEditable(true);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        String selectedType = (String) mappingTypeCombo.getSelectedItem();
        boolean isManyToMany = "ManyToMany".equals(selectedType);
        boolean hasMiddleEntity = isManyToMany && !middleEntityLabel.getText().isEmpty();

        drawEntityRegions(g2d, hasMiddleEntity);

        g2d.setColor(new Color(200, 200, 200));
        g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                0, new float[] { 5 }, 0));

        for (int i = 0; i < attributeGroups.size() - 1; i++) {
            AttributeGroup group = attributeGroups.get(i);
            int lineY = group.yPosition + (hasMiddleEntity ? 80 : 40);
            g2d.drawLine(30, lineY, getWidth() - 30, lineY);
        }

        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.BLACK);

        if (isManyToMany && hasMiddleEntity) {
            for (AttributeGroup group : attributeGroups) {
                int sourceY = group.yPosition + 15;
                int middleY1 = group.yPosition + 15;
                g2d.draw(new Line2D.Double(200, sourceY, 350, middleY1));
                drawArrow(g2d, 340, middleY1);

                int middleY2 = group.yPosition + 50;
                int targetY = group.yPosition + 50;
                g2d.draw(new Line2D.Double(500, middleY2, 650, targetY));
                drawArrow(g2d, 640, targetY);
            }
        } else {
            for (AttributeGroup group : attributeGroups) {
                int y = group.yPosition + 15;
                g2d.draw(new Line2D.Double(200, y, 650, y));
                drawArrow(g2d, 640, y);
            }
        }
    }

    private void drawArrow(Graphics2D g2d, int x, int y) {
        int[] xPoints = { x, x - 10, x - 10 };
        int[] yPoints = { y, y - 5, y + 5 };
        g2d.fillPolygon(xPoints, yPoints, 3);
    }

    private void handleConfirm() {
        NavMappingRelation relation = getNavMappingRelation();
        if (relation != null) {
            if (confirmCallback != null) {
                // 使用 WriteCommandAction 包装文档修改操作
                WriteCommandAction.runWriteCommandAction(null, () -> {
                    confirmCallback.accept(relation);
                });
            }
        }
        // 关闭当前窗口
        SwingUtilities.getWindowAncestor(this).dispose();
    }



    public MappingData collectFormData() {
        String mappingType = (String) mappingTypeCombo.getSelectedItem();
        List<AttributeMapping> attributeMappings = new ArrayList<>();

        for (AttributeGroup group : attributeGroups) {
            String sourceAttr = (String) group.sourceAttr.getSelectedItem();
            String middleAttr = (String) group.middleAttr.getSelectedItem();
            String middleTargetAttr = (String) group.middleTargetAttr.getSelectedItem();
            String targetAttr = (String) group.targetAttr.getSelectedItem();

            attributeMappings.add(new AttributeMapping(
                    sourceAttr,
                    middleAttr,
                    middleTargetAttr,
                    targetAttr));
        }

        return new MappingData(mappingType, attributeMappings);
    }

    public String getSelectedEntity() {
        return targetEntityLabel.getText();
    }

    public String getSelectedMiddleEntity() {
        return middleEntityLabel.getText();
    }

    private void drawEntityRegions(Graphics2D g2d, boolean hasMiddleEntity) {
        Color originalColor = g2d.getColor();
        Stroke originalStroke = g2d.getStroke();
        Font originalFont = g2d.getFont();

        g2d.setFont(new Font("宋体", Font.BOLD, 12));

        int height = getHeight() - 50;
        int startY = ENTITY_REGION_START_Y;

        g2d.setColor(new Color(100, 149, 237, 50));
        g2d.fillRect(40, startY, 170, height - startY);
        g2d.setColor(new Color(100, 149, 237));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRect(40, startY, 170, height - startY);
        g2d.drawString("当前实体", 90, startY - 5);

        if (hasMiddleEntity) {
            g2d.setColor(new Color(144, 238, 144, 50));
            g2d.fillRect(340, startY, 170, height - startY);
            g2d.setColor(new Color(60, 179, 113));
            g2d.drawRect(340, startY, 170, height - startY);
            g2d.drawString("中间实体", 390, startY - 5);
        }

        g2d.setColor(new Color(255, 165, 0, 50));
        g2d.fillRect(640, startY, 170, height - startY);
        g2d.setColor(new Color(255, 140, 0));
        g2d.drawRect(640, startY, 170, height - startY);
        g2d.drawString("目标实体", 690, startY - 5);

        g2d.setColor(originalColor);
        g2d.setStroke(originalStroke);
        g2d.setFont(originalFont);
    }

    private void selectMiddleEntity() {
        EntitySelectDialog dialog = new EntitySelectDialog(
                SwingUtilities.getWindowAncestor(this),
                "选择中间实体",
                availableEntities);
        dialog.setVisible(true);

        String selectedEntity = dialog.getSelectedEntity();
        if (selectedEntity != null) {
            middleEntityLabel.setText(selectedEntity);
            for (AttributeGroup group : attributeGroups) {
                group.updateAttributes(selectedEntity, targetEntityLabel.getText());
            }
            updateMappingDisplay();
        }
    }

    private void selectTargetEntity() {
        EntitySelectDialog dialog = new EntitySelectDialog(
                SwingUtilities.getWindowAncestor(this),
                "选择目标实体",
                availableEntities);
        dialog.setVisible(true);

        String selectedEntity = dialog.getSelectedEntity();
        if (selectedEntity != null) {
            targetEntityLabel.setText(selectedEntity);
            for (AttributeGroup group : attributeGroups) {
                group.updateAttributes(middleEntityLabel.getText(), selectedEntity);
            }
            updateMappingDisplay();
        }
    }

    public NavMappingRelation getNavMappingRelation() {
        String sourceEntity = getCurrentEntityName();
        String targetEntity = getSelectedEntity();

        // 获取映射类型
        String relationType = (String) mappingTypeCombo.getSelectedItem();

        // 收集所有映射组的数据
        List<String> sourceFields = new ArrayList<>();
        List<String> targetFields = new ArrayList<>();
        List<String> selfMappingFields = new ArrayList<>();
        List<String> targetMappingFields = new ArrayList<>();

        for (AttributeGroup group : attributeGroups) {
            String sourceField = (String) group.sourceAttr.getSelectedItem();
            String targetField = (String) group.targetAttr.getSelectedItem();

            if (sourceField != null && !sourceField.isEmpty()) {
                sourceFields.add(sourceField);
            }
            if (targetField != null && !targetField.isEmpty()) {
                targetFields.add(targetField);
            }

            if (Objects.equals(relationType, "ManyToMany")) {
                String middleSourceField = (String) group.middleAttr.getSelectedItem();
                String middleTargetField = (String) group.middleTargetAttr.getSelectedItem();

                if (middleSourceField != null && !middleSourceField.isEmpty()) {
                    selfMappingFields.add(middleSourceField);
                }
                if (middleTargetField != null && !middleTargetField.isEmpty()) {
                    targetMappingFields.add(middleTargetField);
                }
            }
        }

        // 如果必要字段为空，返回null
        if (sourceEntity == null || targetEntity == null ||
                sourceFields.isEmpty() || targetFields.isEmpty()) {
            return null;
        }

        // 对于多对多关系，检查中间表相关字段
        String mappingClass = null;
        if (relationType == "ManyToMany") {
            mappingClass = getSelectedMiddleEntity();
            if (mappingClass == null || mappingClass.isEmpty() ||
                    selfMappingFields.isEmpty() || targetMappingFields.isEmpty()) {
                return null;
            }
        }

        return new NavMappingRelation(
                sourceEntity,
                targetEntity,
                sourceFields.toArray(new String[0]),
                targetFields.toArray(new String[0]),
                relationType,
                mappingClass,
                selfMappingFields.toArray(new String[0]),
                targetMappingFields.toArray(new String[0]),
                true // 默认设置propIsProxy为true
        );
    }
}