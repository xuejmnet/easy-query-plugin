package com.easy.query.plugin.action.navgen;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.geom.Line2D;

public class NavMappingPanel extends JPanel {
    private JComboBox<String> mappingTypeCombo;
    private JComboBox<String> sourceAttr1;
    private JComboBox<String> sourceAttr2;
    private JComboBox<String> middleAttr1;
    private JComboBox<String> middleAttr2;
    private JComboBox<String> targetAttr1;
    private JComboBox<String> targetAttr2;
    private JComboBox<String> middleTargetAttr1;
    private JComboBox<String> middleTargetAttr2;

    public NavMappingPanel() {
        setLayout(null); // 使用绝对布局

        // 初始化组件
        initComponents();

        // 添加组件到面板
        addComponents();

        // 设置初始状态
        updateMappingDisplay();
    }

    private void initComponents() {
        // 顶部映射类型选择 - 修改默认选项为 OneToOne
        mappingTypeCombo = new JComboBox<>(new String[] { "OneToOne", "OneToMany", "ManyToOne", "ManyToMany" });
        mappingTypeCombo.setSelectedItem("OneToOne"); // 设置默认选中项
        mappingTypeCombo.setBounds(300, 20, 150, 30);

        // 左侧当前实体属性
        String[] sourceAttributes = new String[] {
                "当前实体属性1",
                "id",
                "name",
                "code",
                "description"
        };
        sourceAttr1 = new JComboBox<>(sourceAttributes);
        sourceAttr2 = new JComboBox<>(sourceAttributes);
        sourceAttr1.setBounds(50, 200, 150, 30);
        sourceAttr2.setBounds(50, 300, 150, 30);

        // 中间映射属性
        String[] mappingAttributes = new String[] {
                "映射当前实体属性1",
                "source_id",
                "source_code",
                "target_id",
                "target_code"
        };
        middleAttr1 = new JComboBox<>(mappingAttributes);
        middleAttr2 = new JComboBox<>(mappingAttributes);
        middleTargetAttr1 = new JComboBox<>(mappingAttributes);
        middleTargetAttr2 = new JComboBox<>(mappingAttributes);

        middleAttr1.setBounds(350, 200, 150, 30);
        middleAttr2.setBounds(350, 300, 150, 30);
        middleTargetAttr1.setBounds(350, 400, 150, 30);
        middleTargetAttr2.setBounds(350, 500, 150, 30);

        // 右侧目标实体属性
        String[] targetAttributes = new String[] {
                "目标实体属性1",
                "id",
                "name",
                "code",
                "status"
        };
        targetAttr1 = new JComboBox<>(targetAttributes);
        targetAttr2 = new JComboBox<>(targetAttributes);
        targetAttr1.setBounds(650, 400, 150, 30);
        targetAttr2.setBounds(650, 500, 150, 30);

        // 设置所有下拉框的样式
        configureComboBoxStyle(mappingTypeCombo, sourceAttr1, sourceAttr2,
                middleAttr1, middleAttr2, middleTargetAttr1,
                middleTargetAttr2, targetAttr1, targetAttr2);

        // 在 mappingTypeCombo 初始化后添加监听器
        mappingTypeCombo.addActionListener(e -> updateMappingDisplay());
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

    private void addComponents() {
        add(mappingTypeCombo);
        add(sourceAttr1);
        add(sourceAttr2);
        add(middleAttr1);
        add(middleAttr2);
        add(middleTargetAttr1);
        add(middleTargetAttr2);
        add(targetAttr1);
        add(targetAttr2);
    }

    private void updateMappingDisplay() {
        String selectedType = (String) mappingTypeCombo.getSelectedItem();
        boolean isManyToMany = "ManyToMany".equals(selectedType);

        // 更新中间实体相关组件的可见性
        middleAttr1.setVisible(isManyToMany);
        middleAttr2.setVisible(isManyToMany);
        middleTargetAttr1.setVisible(isManyToMany);
        middleTargetAttr2.setVisible(isManyToMany);

        // 如果不是多对多，调整目标属性的位置
        if (!isManyToMany) {
            targetAttr1.setBounds(650, 200, 150, 30);
            targetAttr2.setBounds(650, 300, 150, 30);
        } else {
            targetAttr1.setBounds(650, 400, 150, 30);
            targetAttr2.setBounds(650, 500, 150, 30);
        }

        repaint(); // 重绘面板以更新连接线
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制标题
        g2d.setFont(new Font("宋体", Font.PLAIN, 14));
        g2d.drawString("映射类型:", 220, 40);
        g2d.drawString("当前实体(必须)", 50, 150);

        String selectedType = (String) mappingTypeCombo.getSelectedItem();
        boolean isManyToMany = "ManyToMany".equals(selectedType);

        if (isManyToMany) {
            g2d.drawString("中间实体(可选)", 350, 150);
            g2d.drawString("目标实体(必须)", 650, 150);

            // 绘制多对多模式的连接线
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(new Line2D.Double(200, 215, 350, 215));
            g2d.draw(new Line2D.Double(200, 315, 350, 315));
            g2d.draw(new Line2D.Double(500, 415, 650, 415));
            g2d.draw(new Line2D.Double(500, 515, 650, 515));

            // 绘制箭头
            drawArrow(g2d, 340, 215);
            drawArrow(g2d, 340, 315);
            drawArrow(g2d, 640, 415);
            drawArrow(g2d, 640, 515);
        } else {
            g2d.drawString("目标实体(必须)", 650, 150);

            // 绘制直接连接线
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(new Line2D.Double(200, 215, 650, 215));
            g2d.draw(new Line2D.Double(200, 315, 650, 315));

            // 绘制箭头
            drawArrow(g2d, 640, 215);
            drawArrow(g2d, 640, 315);
        }
    }

    private void drawArrow(Graphics2D g2d, int x, int y) {
        int[] xPoints = { x, x - 10, x - 10 };
        int[] yPoints = { y, y - 5, y + 5 };
        g2d.fillPolygon(xPoints, yPoints, 3);
    }
}