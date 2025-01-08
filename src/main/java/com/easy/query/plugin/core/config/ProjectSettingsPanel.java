package com.easy.query.plugin.core.config;

import cn.hutool.core.convert.Convert;
import lombok.Getter;

import javax.swing.*;
import java.awt.*;

public class ProjectSettingsPanel {

    @Getter
    private final JPanel panel;


    private final JCheckBox featureKeepDtoColumnAnnotationCheckBox;


    public ProjectSettingsPanel() {
        panel = new JPanel(new BorderLayout());

        featureKeepDtoColumnAnnotationCheckBox = new JCheckBox("DTO 上 @Column 只有 value 属性时 仍保留, 请确保当前项目映射关系为 PROPERTY_FIRST 或 PROPERTY_ONLY 时取消勾选");
        panel.add(featureKeepDtoColumnAnnotationCheckBox, BorderLayout.NORTH);

    }


    public Boolean getDtoKeepAnnotationColumn() {
        return Convert.toBool(featureKeepDtoColumnAnnotationCheckBox.isSelected(), true);
    }

    public void setFeatureKeepDtoColumnAnnotation(Boolean keepDtoColumnAnnotation) {
        featureKeepDtoColumnAnnotationCheckBox.setSelected(Convert.toBool(keepDtoColumnAnnotation,true));
    }


}
