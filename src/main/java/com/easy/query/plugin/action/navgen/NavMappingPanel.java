package com.easy.query.plugin.action.navgen;

import cn.hutool.core.lang.Pair;
import com.google.common.collect.Lists;
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
/**
 * Nav 注解生成GUI
 * @author link2fun
 */
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
    private final List<Pair<String,String>> availableEntities;
    @Getter
    private final String currentEntityName;
    private final Map<String, List<Pair<String,String>>> entityAttributesMap;
    private final Consumer<NavMappingRelation> confirmCallback;

    // 亮色主题颜色
    private static final Color LIGHT_PRIMARY_COLOR = new Color(33, 150, 243); // Material Blue
    private static final Color LIGHT_BACKGROUND_COLOR = new Color(250, 250, 250);
    private static final Color LIGHT_TEXT_COLOR = new Color(33, 33, 33);
    private static final Color LIGHT_BORDER_COLOR = new Color(224, 224, 224);
    private static final Color LIGHT_BUTTON_HOVER_COLOR = new Color(41, 182, 246);
    private static final Color LIGHT_COMPONENT_BACKGROUND = Color.WHITE;

    // 暗色主题颜色
    private static final Color DARK_PRIMARY_COLOR = new Color(64, 169, 255); // Material Blue (darker)
    private static final Color DARK_BACKGROUND_COLOR = new Color(43, 43, 43);
    private static final Color DARK_TEXT_COLOR = new Color(187, 187, 187);
    private static final Color DARK_BORDER_COLOR = new Color(73, 73, 73);
    private static final Color DARK_BUTTON_HOVER_COLOR = new Color(82, 176, 255);
    private static final Color DARK_COMPONENT_BACKGROUND = new Color(60, 63, 65);

    private static final Font LABEL_FONT = new Font("Microsoft YaHei", Font.PLAIN, 13);
    private static final Font COMBO_FONT = new Font("Microsoft YaHei", Font.PLAIN, 13);
    private static final int BORDER_RADIUS = 8;

    // 当前主题颜色
    private Color primaryColor;
    private Color backgroundColor;
    private Color textColor;
    private Color borderColor;
    private Color buttonHoverColor;
    private Color componentBackground;

    private void initializeThemeColors() {
        boolean isDarkTheme = isDarkTheme();
        primaryColor = isDarkTheme ? DARK_PRIMARY_COLOR : LIGHT_PRIMARY_COLOR;
        backgroundColor = isDarkTheme ? DARK_BACKGROUND_COLOR : LIGHT_BACKGROUND_COLOR;
        textColor = isDarkTheme ? DARK_TEXT_COLOR : LIGHT_TEXT_COLOR;
        borderColor = isDarkTheme ? DARK_BORDER_COLOR : LIGHT_BORDER_COLOR;
        buttonHoverColor = isDarkTheme ? DARK_BUTTON_HOVER_COLOR : LIGHT_BUTTON_HOVER_COLOR;
        componentBackground = isDarkTheme ? DARK_COMPONENT_BACKGROUND : LIGHT_COMPONENT_BACKGROUND;
    }

    private boolean isDarkTheme() {
        // 使用 IntelliJ 的 API 检查当前主题
        try {
            Class<?> jbColorClass = Class.forName("com.intellij.util.ui.JBColor");
            boolean isDark = (boolean) jbColorClass.getMethod("isBright").invoke(null);
            return !isDark;
        } catch (Exception e) {
            // 如果获取失败，尝试使用 UIManager 的方式
            try {
                Color defaultBackground = UIManager.getColor("Panel.background");
                // 计算颜色的亮度
                double brightness = (defaultBackground.getRed() * 0.299 +
                        defaultBackground.getGreen() * 0.587 +
                        defaultBackground.getBlue() * 0.114) / 255;
                return brightness < 0.5;
            } catch (Exception ex) {
                // 如果都失败了，返回false作为默认值
                return false;
            }
        }
    }

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
        JButton sourceAttrButton;
        JButton middleAttrButton;
        JButton middleTargetAttrButton;
        JButton targetAttrButton;
        JButton deleteButton;
        int yPosition;
        
        // 存储选中的属性值
        private String selectedSourceAttr = "";
        private String selectedMiddleAttr = "";
        private String selectedMiddleTargetAttr = "";
        private String selectedTargetAttr = "";

        public AttributeGroup(int yPos) {
            this.yPosition = yPos;
            initializeComponents();
        }

        private void initializeComponents() {
            List<Pair<String,String>> sourceAttributes = entityAttributesMap.getOrDefault(currentEntityName,
                    Lists.newArrayList());

            sourceAttrButton = createAttributeButton("选择属性", sourceAttributes, value -> selectedSourceAttr = value);
            middleAttrButton = createAttributeButton("选择属性", Lists.newArrayList(), value -> selectedMiddleAttr = value);
            middleTargetAttrButton = createAttributeButton("选择属性", Lists.newArrayList(), value -> selectedMiddleTargetAttr = value);
            targetAttrButton = createAttributeButton("选择属性", Lists.newArrayList(), value -> selectedTargetAttr = value);
            deleteButton = new JButton("删除");

            sourceAttrButton.setBounds(50, yPosition, 150, 30);
            middleAttrButton.setBounds(350, yPosition, 150, 30);
            middleTargetAttrButton.setBounds(350, yPosition + 35, 150, 30);
            targetAttrButton.setBounds(650, yPosition, 150, 30);
            deleteButton.setBounds(820, yPosition, 60, 30);

            configureButtonStyle(sourceAttrButton, middleAttrButton, middleTargetAttrButton, targetAttrButton);
            deleteButton.setVisible(false);
        }

        private JButton createAttributeButton(String defaultText, List<Pair<String,String>> attributes, Consumer<String> onSelect) {
            JButton button = new JButton(defaultText);
            button.addActionListener(e -> {
                EntitySelectDialog dialog = new EntitySelectDialog(
                        SwingUtilities.getWindowAncestor(NavMappingPanel.this),
                        "选择属性",
                        attributes);
                dialog.setVisible(true);

                String selectedValue = dialog.getSelectedEntity();
                if (selectedValue != null) {
                    button.setText(selectedValue);
                    onSelect.accept(selectedValue);
                }
            });
            return button;
        }

        private void configureButtonStyle(JButton... buttons) {
            for (JButton button : buttons) {
                button.setFont(COMBO_FONT);
                button.setBackground(componentBackground);
                button.setForeground(textColor);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(borderColor),
                    BorderFactory.createEmptyBorder(0, 5, 0, 5)
                ));
                button.setHorizontalAlignment(SwingConstants.LEFT);
            }
        }

        public void updateAttributes(String middleEntity, String targetEntity) {
            if (middleEntity != null && !middleEntity.isEmpty()) {
                List<Pair<String,String>> middleAttributes = entityAttributesMap.getOrDefault(middleEntity,
                        Lists.newArrayList());
                updateAttributeButton(middleAttrButton, middleAttributes);
                updateAttributeButton(middleTargetAttrButton, middleAttributes);
            }

            if (targetEntity != null && !targetEntity.isEmpty()) {
                List<Pair<String,String>> targetAttributes = entityAttributesMap.getOrDefault(targetEntity,
                        Lists.newArrayList());
                updateAttributeButton(targetAttrButton, targetAttributes);
            }
        }

        private void updateAttributeButton(JButton button, List<Pair<String,String>> attributes) {
            String currentText = button.getText();
            if (attributes.stream().noneMatch(attr->attr.getKey().contains(currentText))) {
                button.setText("选择属性");
                // 重置对应的属性值
                if (button == sourceAttrButton) {
                    selectedSourceAttr = "";
                } else if (button == middleAttrButton) {
                    selectedMiddleAttr = "";
                } else if (button == middleTargetAttrButton) {
                    selectedMiddleTargetAttr = "";
                } else if (button == targetAttrButton) {
                    selectedTargetAttr = "";
                }
            }
            button.removeActionListener(button.getActionListeners()[0]);
            button.addActionListener(e -> {
                EntitySelectDialog dialog = new EntitySelectDialog(
                        SwingUtilities.getWindowAncestor(NavMappingPanel.this),
                        "选择属性",
                        attributes);
                dialog.setVisible(true);

                String selectedValue = dialog.getSelectedEntity();
                if (selectedValue != null) {
                    button.setText(selectedValue);
                    // 更新对应的属性值
                    if (button == sourceAttrButton) {
                        selectedSourceAttr = selectedValue;
                    } else if (button == middleAttrButton) {
                        selectedMiddleAttr = selectedValue;
                    } else if (button == middleTargetAttrButton) {
                        selectedMiddleTargetAttr = selectedValue;
                    } else if (button == targetAttrButton) {
                        selectedTargetAttr = selectedValue;
                    }
                }
            });
        }

        // 获取选中的属性值
        public String getSourceAttribute() {
            return selectedSourceAttr;
        }

        public String getMiddleAttribute() {
            return selectedMiddleAttr;
        }

        public String getMiddleTargetAttribute() {
            return selectedMiddleTargetAttr;
        }

        public String getTargetAttribute() {
            return selectedTargetAttr;
        }
    }

    public NavMappingPanel(List<Pair<String,String>> availableEntities, String currentEntityName,
            Map<String, List<Pair<String,String>>> entityAttributesMap, Consumer<NavMappingRelation> confirmCallback) {
        this(availableEntities, currentEntityName, null, entityAttributesMap, confirmCallback);
    }

    public NavMappingPanel(List<Pair<String,String>> availableEntities, String currentEntityName,
            String defaultTargetEntity, Map<String, List<Pair<String,String>>> entityAttributesMap,
            Consumer<NavMappingRelation> confirmCallback) {
        this.availableEntities = availableEntities;
        this.currentEntityName = currentEntityName;
        this.entityAttributesMap = entityAttributesMap;
        this.confirmCallback = confirmCallback;
        initializeThemeColors();
        initializePanel();
        
        // 如果有默认目标实体，设置它
        if (defaultTargetEntity != null && !defaultTargetEntity.isEmpty()) {
            targetEntityLabel.setText(defaultTargetEntity);
            // 更新所有属性组的目标实体属性
            for (AttributeGroup group : attributeGroups) {
                group.updateAttributes(middleEntityLabel.getText(), defaultTargetEntity);
            }
            updateMappingDisplay();
        }
    }

    private void initializePanel() {
        setLayout(null);
        setPreferredSize(new Dimension(900, 600));
        setBackground(backgroundColor);

        // 映射类型
        JLabel mappingTypeLabel = createStyledLabel("映射类型:", 50, 20, 100, 30);
        add(mappingTypeLabel);

        mappingTypeCombo = createStyledComboBox(new String[]{"", "OneToOne", "OneToMany", "ManyToOne", "ManyToMany"});
        mappingTypeCombo.setBounds(150, 20, 150, 30);
        mappingTypeCombo.setSelectedItem("");
        mappingTypeCombo.addActionListener(e -> updateMappingDisplay());
        add(mappingTypeCombo);

        // 当前实体
        JLabel currentEntityLabel = createStyledLabel("当前实体:", 50, 55, 100, 30);
        add(currentEntityLabel);

        JLabel currentEntityDisplay = createStyledDisplayLabel(currentEntityName, 150, 55, 550, 30);
        add(currentEntityDisplay);

        // 中间实体
        JLabel middleLabel = createStyledLabel("中间实体(可选):", 50, 90, 100, 30);
        add(middleLabel);

        middleEntityLabel = createStyledDisplayLabel("", 150, 90, 550, 30);
        add(middleEntityLabel);

        selectMiddleEntityButton = createStyledButton("选择中间实体", 700, 90, 120, 30);
        selectMiddleEntityButton.addActionListener(e -> selectMiddleEntity());
        add(selectMiddleEntityButton);

        // 目标实体
        JLabel targetLabel = createStyledLabel("目标实体(必须):", 50, 125, 100, 30);
        add(targetLabel);

        targetEntityLabel = createStyledDisplayLabel("", 150, 125, 550, 30);
        add(targetEntityLabel);

        selectTargetEntityButton = createStyledButton("选择目标实体", 700, 125, 120, 30);
        selectTargetEntityButton.addActionListener(e -> selectTargetEntity());
        add(selectTargetEntityButton);

        // 操作按钮
        addGroupButton = createStyledButton("添加映射组", 650, 20, 100, 30);
        add(addGroupButton);
        addGroupButton.addActionListener(e -> addAttributeGroup());

        confirmButton = createStyledButton("确认", 760, 20, 80, 30);
        add(confirmButton);
        confirmButton.addActionListener(e -> handleConfirm());

        attributeGroups = new ArrayList<>();
        addAttributeGroup();
        updateMappingDisplay();
    }

    private JLabel createStyledLabel(String text, int x, int y, int width, int height) {
        JLabel label = new JLabel(text);
        label.setBounds(x, y, width, height);
        label.setFont(LABEL_FONT);
        label.setForeground(textColor);
        return label;
    }

    private JLabel createStyledDisplayLabel(String text, int x, int y, int width, int height) {
        JLabel label = new JLabel(text);
        label.setBounds(x, y, width, height);
        label.setFont(COMBO_FONT);
        label.setForeground(textColor);
        label.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor),
            BorderFactory.createEmptyBorder(0, 5, 0, 5)
        ));
        label.setOpaque(true);
        label.setBackground(componentBackground);
        return label;
    }

    private JButton createStyledButton(String text, int x, int y, int width, int height) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(primaryColor.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(buttonHoverColor);
                } else {
                    g2.setColor(primaryColor);
                }
                
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), BORDER_RADIUS, BORDER_RADIUS);
                g2.dispose();
                
                super.paintComponent(g);
            }
        };
        button.setBounds(x, y, width, height);
        button.setFont(COMBO_FONT);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        return button;
    }

    private JComboBox<String> createStyledComboBox(String[] items) {
        JComboBox<String> comboBox = new JComboBox<>(items);
        comboBox.setFont(COMBO_FONT);
        comboBox.setBackground(componentBackground);
        comboBox.setForeground(textColor);
        comboBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor),
            BorderFactory.createEmptyBorder(0, 5, 0, 5)
        ));
        
        comboBox.setUI(new BasicComboBoxUI() {
            @Override
            protected JButton createArrowButton() {
                JButton button = super.createArrowButton();
                button.setBackground(componentBackground);
                button.setBorder(BorderFactory.createEmptyBorder());
                return button;
            }
        });
        
        return comboBox;
    }

    private void configureComboBoxStyle(JComboBox<?>... comboBoxes) {
        for (JComboBox<?> box : comboBoxes) {
            box.setFont(COMBO_FONT);
            box.setBackground(componentBackground);
            box.setForeground(textColor);
            box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor),
                BorderFactory.createEmptyBorder(0, 5, 0, 5)
            ));
            box.setUI(new BasicComboBoxUI() {
                @Override
                protected JButton createArrowButton() {
                    JButton button = super.createArrowButton();
                    button.setBackground(componentBackground);
                    button.setBorder(BorderFactory.createEmptyBorder());
                    return button;
                }
            });
        }
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

        add(group.sourceAttrButton);
        add(group.middleAttrButton);
        add(group.middleTargetAttrButton);
        add(group.targetAttrButton);
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
        remove(group.sourceAttrButton);
        remove(group.middleAttrButton);
        remove(group.middleTargetAttrButton);
        remove(group.targetAttrButton);
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
        group.sourceAttrButton.setBounds(50, newY, 150, 30);

        String selectedType = (String) mappingTypeCombo.getSelectedItem();
        boolean isManyToMany = "ManyToMany".equals(selectedType);
        boolean hasMiddleEntity = isManyToMany && !middleEntityLabel.getText().isEmpty();

        if (isManyToMany && hasMiddleEntity) {
            group.middleAttrButton.setBounds(350, newY, 150, 30);
            group.middleTargetAttrButton.setBounds(350, newY + 35, 150, 30);
            group.targetAttrButton.setBounds(650, newY + 35, 150, 30);
            group.deleteButton.setBounds(830, newY + 15, 60, 30);
        } else {
            group.targetAttrButton.setBounds(650, newY, 150, 30);
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
            group.middleAttrButton.setVisible(hasMiddleEntity);
            group.middleTargetAttrButton.setVisible(hasMiddleEntity);

            updateGroupPosition(group, newY);
            newY += GROUP_VERTICAL_GAP;
            if (hasMiddleEntity) {
                newY += MANY_TO_MANY_EXTRA_GAP;
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(backgroundColor);
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
        if (relation == null) {
            return;
        }
        if (relation.getRelationType() == null || relation.getRelationType().isEmpty()) {
            JOptionPane.showMessageDialog(this, "请选择映射类型", "提示", JOptionPane.WARNING_MESSAGE);
            return;
        }
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
            String sourceAttr = group.getSourceAttribute();
            String middleAttr = group.getMiddleAttribute();
            String middleTargetAttr = group.getMiddleTargetAttribute();
            String targetAttr = group.getTargetAttribute();

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
            String sourceField = group.getSourceAttribute();
            String targetField = group.getTargetAttribute();

            if (sourceField != null && !sourceField.isEmpty()) {
                sourceFields.add(sourceField);
            }
            if (targetField != null && !targetField.isEmpty()) {
                targetFields.add(targetField);
            }

            if ("ManyToMany".equals(relationType)) {
                String middleSourceField = group.getMiddleAttribute();
                String middleTargetField = group.getMiddleTargetAttribute();

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
        if ("ManyToMany".equals(relationType)) {
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