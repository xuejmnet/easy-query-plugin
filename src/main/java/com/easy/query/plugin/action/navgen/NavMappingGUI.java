package com.easy.query.plugin.action.navgen;

import javax.swing.*;
import java.awt.*;

public class NavMappingGUI extends JFrame {
    private static final int WINDOW_WIDTH = 1000;
    private static final int WINDOW_HEIGHT = 600;

    public NavMappingGUI() {
        setTitle("映射关系");
        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 创建主面板
        NavMappingPanel navMappingPanel = new NavMappingPanel();
        add(navMappingPanel, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            NavMappingGUI gui = new NavMappingGUI();
            gui.setVisible(true);
        });
    }
}