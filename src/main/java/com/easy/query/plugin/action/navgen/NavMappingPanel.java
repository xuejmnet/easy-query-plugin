package com.easy.query.plugin.action.navgen;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class NavMappingPanel extends JPanel {
    private JComboBox<String> mappingTypeCombo;
    private List<AttributeGroup> attributeGroups;
    private JButton addGroupButton;
    private JButton confirmButton;
    private JPanel attributesPanel;
    private static final int INITIAL_GROUP_Y = 200;
    private static final int GROUP_VERTICAL_GAP = 100;
    private JComboBox<String> entitySelector;
    private JComboBox<String> middleEntitySelector;

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
            String[] sourceAttributes = new String[] {
                    "当前实体属性",
                    "id",
                    "name",
                    "code",
                    "description"
            };
            String[] mappingAttributes = new String[] {
                    "映射实体属性",
                    "source_id",
                    "source_code",
                    "target_id",
                    "target_code"
            };
            String[] targetAttributes = new String[] {
                    "目标实体属性",
                    "id",
                    "name",
                    "code",
                    "status"
            };

            sourceAttr = new JComboBox<>(sourceAttributes);
            middleAttr = new JComboBox<>(mappingAttributes);
            middleTargetAttr = new JComboBox<>(mappingAttributes);
            targetAttr = new JComboBox<>(targetAttributes);
            deleteButton = new JButton("删除");

            // 设置组件位置
            sourceAttr.setBounds(50, yPosition, 150, 30);
            middleAttr.setBounds(350, yPosition, 150, 30);
            middleTargetAttr.setBounds(350, yPosition + 50, 150, 30);
            targetAttr.setBounds(650, yPosition, 150, 30);
            deleteButton.setBounds(820, yPosition, 60, 30);

            // 配置样式
            configureComboBoxStyle(sourceAttr, middleAttr, middleTargetAttr, targetAttr);
            deleteButton.setVisible(false); // 第一组默认不显示删除按钮
        }
    }

    public NavMappingPanel() {
        setLayout(null);

        // 创建目标实体选择面板
        JPanel targetEntityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        targetEntityPanel.setBounds(650, 135, 300, 30);

        // 创建中间实体选择面板
        JPanel middleEntityPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        middleEntityPanel.setBounds(350, 135, 300, 30);

        // 创建并配置实体选择下拉框
        entitySelector = new JComboBox<>();
        entitySelector.setPreferredSize(new Dimension(150, 25));
        entitySelector.addItem("实体1");
        entitySelector.addItem("实体2");

        // 创建并配置中间实体选择下拉框
        middleEntitySelector = new JComboBox<>();
        middleEntitySelector.setPreferredSize(new Dimension(150, 25));
        middleEntitySelector.addItem("中间实体1");
        middleEntitySelector.addItem("中间实体2");

        // 添加标签和下拉框到目标实体面板
        JLabel targetLabel = new JLabel("目标实体(必须):");
        targetEntityPanel.add(targetLabel);
        targetEntityPanel.add(entitySelector);

        // 添加标签和下拉框到中间实体面板
        JLabel middleLabel = new JLabel("中间实体(必须):");
        middleEntityPanel.add(middleLabel);
        middleEntityPanel.add(middleEntitySelector);

        add(targetEntityPanel);
        add(middleEntityPanel);

        // 根据映射类型控制中间实体选择器的可见性
        middleEntityPanel.setVisible(false); // 默认隐藏

        attributeGroups = new ArrayList<>();
        initComponents();
        addAttributeGroup();
        updateMappingDisplay();
    }

    private void initComponents() {
        // 调整映射类型选择的位置
        mappingTypeCombo = new JComboBox<>(new String[] { "OneToOne", "OneToMany", "ManyToOne", "ManyToMany" });
        mappingTypeCombo.setSelectedItem("OneToOne");
        mappingTypeCombo.setBounds(300, 100, 150, 30);
        mappingTypeCombo.addActionListener(e -> updateMappingDisplay());

        // 调整添加按钮的位置
        addGroupButton = new JButton("添加映射组");
        addGroupButton.setBounds(50, 180, 100, 30);
        addGroupButton.addActionListener(e -> addAttributeGroup());

        // 确认按钮位置保持不变
        confirmButton = new JButton("确认");
        confirmButton.addActionListener(e -> handleConfirm());
        confirmButton.setBounds(getWidth() - 100, getHeight() - 40, 80, 30);

        add(mappingTypeCombo);
        add(addGroupButton);
        add(confirmButton);
    }

    private void addAttributeGroup() {
        int yPos = INITIAL_GROUP_Y + 80 + attributeGroups.size() * GROUP_VERTICAL_GAP;
        AttributeGroup group = new AttributeGroup(yPos);
        attributeGroups.add(group);

        // 添加组件到面板
        add(group.sourceAttr);
        add(group.middleAttr);
        add(group.middleTargetAttr);
        add(group.targetAttr);
        add(group.deleteButton);

        // 如果不是第一组，显示删除按钮
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

        // 重新调整剩余组的位置
        for (int i = 0; i < attributeGroups.size(); i++) {
            AttributeGroup currentGroup = attributeGroups.get(i);
            int newY = INITIAL_GROUP_Y + i * GROUP_VERTICAL_GAP;
            updateGroupPosition(currentGroup, newY);
        }

        revalidate();
        repaint();
    }

    private void updateGroupPosition(AttributeGroup group, int newY) {
        group.yPosition = newY;
        group.sourceAttr.setBounds(50, newY, 150, 30);
        group.middleAttr.setBounds(350, newY, 150, 30);
        group.middleTargetAttr.setBounds(350, newY + 50, 150, 30);

        // 根据当前映射类型调整目标属性的位置
        String selectedType = (String) mappingTypeCombo.getSelectedItem();
        boolean isManyToMany = "ManyToMany".equals(selectedType);
        if (isManyToMany) {
            group.targetAttr.setBounds(650, newY + 50, 150, 30);
        } else {
            group.targetAttr.setBounds(650, newY, 150, 30);
        }

        group.deleteButton.setBounds(820, newY, 60, 30);
    }

    private void updateMappingDisplay() {
        String selectedType = (String) mappingTypeCombo.getSelectedItem();
        boolean isManyToMany = "ManyToMany".equals(selectedType);

        // 控制中间实体选择器的可见性
        Component[] components = getComponents();
        for (Component component : components) {
            if (component instanceof JPanel && component.getBounds().x == 350 && component.getBounds().y == 135) {
                component.setVisible(isManyToMany);
                break;
            }
        }

        for (AttributeGroup group : attributeGroups) {
            group.middleAttr.setVisible(isManyToMany);
            group.middleTargetAttr.setVisible(isManyToMany);

            if (!isManyToMany) {
                group.targetAttr.setBounds(650, group.yPosition, 150, 30);
            } else {
                group.targetAttr.setBounds(650, group.yPosition + 50, 150, 30);
            }
        }

        repaint();
    }

    private void configureComboBoxStyle(JComboBox<?>... comboBoxes) {
        for (JComboBox<?> box : comboBoxes) {
            box.setBackground(Color.WHITE);
            box.setFont(new Font("宋体", Font.PLAIN, 12));
            // 设置下拉框边框
            box.setBorder(BorderFactory.createLineBorder(Color.GRAY));
            // 设置下拉箭头的颜色和大小
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

        // 绘制标题
        g2d.setFont(new Font("宋体", Font.PLAIN, 14));
        g2d.drawString("映射类型:", 220, 115);
        g2d.drawString("当前实体(必须)", 50, 150);

        String selectedType = (String) mappingTypeCombo.getSelectedItem();
        boolean isManyToMany = "ManyToMany".equals(selectedType);

        if (isManyToMany) {
            g2d.drawString("中间实体(可选)", 350, 150);
            // 移除原来的"目标实体(必须)"文本绘制，因为现在用实体选择器代替

            // 为每个映射组绘制连接线
            g2d.setStroke(new BasicStroke(2));
            for (AttributeGroup group : attributeGroups) {
                // 源实体到中间实体的第一个属性的连线
                int sourceY = group.yPosition + 15;
                int middleY1 = group.yPosition + 15;
                g2d.draw(new Line2D.Double(200, sourceY, 350, middleY1));
                drawArrow(g2d, 340, middleY1);

                // 中间实体的第二个属性到目标实体的连线
                int middleY2 = group.yPosition + 65; // 中间实体第二个属性的Y坐标
                int targetY = group.yPosition + 65; // 目标实体属性的Y坐标，与中间实体第二个属性对齐
                g2d.draw(new Line2D.Double(500, middleY2, 650, targetY));
                drawArrow(g2d, 640, targetY);
            }
        } else {
            // 移除原来的"目标实体(必须)"文本绘制，因为现在用实体选择器代替

            // 为每个映射组绘制直接连接线
            g2d.setStroke(new BasicStroke(2));
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
        MappingData mappingData = collectFormData();
        // 这里可以添加处理收集到的数据的逻辑
        System.out.println("Mapping Type: " + mappingData.getMappingType());
        for (AttributeMapping mapping : mappingData.getAttributeMappings()) {
            System.out.println("Source: " + mapping.getSourceAttribute());
            if ("ManyToMany".equals(mappingData.getMappingType())) {
                System.out.println("Middle Source: " + mapping.getMiddleSourceAttribute());
                System.out.println("Middle Target: " + mapping.getMiddleTargetAttribute());
            }
            System.out.println("Target: " + mapping.getTargetAttribute());
        }
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

    // 获取选中的实体
    public String getSelectedEntity() {
        return (String) entitySelector.getSelectedItem();
    }

    // 新增获取选中的中间实体的方法
    public String getSelectedMiddleEntity() {
        return (String) middleEntitySelector.getSelectedItem();
    }
}