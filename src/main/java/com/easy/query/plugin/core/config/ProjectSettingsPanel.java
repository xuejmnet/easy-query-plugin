package com.easy.query.plugin.core.config;

import cn.hutool.core.convert.Convert;
import com.easy.query.plugin.action.AbstractPreviewSqlAction;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectSettingsPanel {

    @Getter
    private final JPanel panel;

    /**
     * DTO上@Column只有value属性时是否保留
     */
    private final JCheckBox featureKeepDtoColumnAnnotationCheckBox;

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

        featureKeepDtoColumnAnnotationCheckBox = new JCheckBox(
                "DTO 上 @Column 只有 value 属性时 仍保留, 请确保当前项目映射关系为 PROPERTY_FIRST 或 PROPERTY_ONLY 时取消勾选");
        topPanel.add(featureKeepDtoColumnAnnotationCheckBox);

        JPanel databasePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        List<String> typeList = AbstractPreviewSqlAction.DATABASE_TUPLES.stream()
                .map(AbstractPreviewSqlAction.DatabaseTuple::getDatabaseType).collect(Collectors.toList());
        databaseTypeComboBox = new JComboBox<>(typeList.toArray(new String[0]));
        databaseTypeLabel = new JLabel("数据库类型");
        databasePanel.add(databaseTypeLabel);
        databasePanel.add(databaseTypeComboBox);

        topPanel.add(databasePanel);
        panel.add(topPanel, BorderLayout.NORTH);

    }

    /**
     * 获取DTO上@Column只有value属性时是否保留
     * 
     * @return 是否保留
     */
    public Boolean getDtoKeepAnnotationColumn() {
        return Convert.toBool(featureKeepDtoColumnAnnotationCheckBox.isSelected(), true);
    }

    /**
     * 设置DTO上@Column只有value属性时是否保留
     * 
     * @param keepDtoColumnAnnotation 是否保留
     */
    public void setFeatureKeepDtoColumnAnnotation(Boolean keepDtoColumnAnnotation) {
        featureKeepDtoColumnAnnotationCheckBox.setSelected(Convert.toBool(keepDtoColumnAnnotation, true));
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
