package com.easy.query.plugin.core.config;

import cn.hutool.core.convert.Convert;
import com.easy.query.plugin.action.PreviewEditorSQLAbstractAction;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectSettingsPanel {

    @Getter
    private final JPanel panel;

    /**
     * 数据库类型
     */
    private final JComboBox<String> databaseTypeComboBox;
    /**
     * 数据库类型标签
     */
    private final JLabel databaseTypeLabel;

    public ProjectSettingsPanel() {
        panel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel databasePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        List<String> typeList = PreviewEditorSQLAbstractAction.DATABASE_TUPLES.stream()
                .map(PreviewEditorSQLAbstractAction.DatabaseTuple::getDatabaseType).collect(Collectors.toList());
        databaseTypeComboBox = new JComboBox<>(typeList.toArray(new String[0]));
        databaseTypeLabel = new JLabel("数据库类型");
        databasePanel.add(databaseTypeLabel);
        databasePanel.add(databaseTypeComboBox);

        topPanel.add(databasePanel);
        panel.add(topPanel, BorderLayout.NORTH);

    }

    /**
     * 设置数据库类型
     * 
     * @param databaseType 数据库类型
     */
    public void setDatabaseType(String databaseType) {
        databaseTypeComboBox.setSelectedItem(databaseType);
    }

    /**
     * 获取数据库类型
     * 
     * @return 数据库类型
     */
    public String getDatabaseType() {
        return (String) databaseTypeComboBox.getSelectedItem();
    }

}
