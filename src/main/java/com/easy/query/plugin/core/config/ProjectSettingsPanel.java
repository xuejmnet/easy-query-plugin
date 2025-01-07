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

        featureKeepDtoColumnAnnotationCheckBox = new JCheckBox("Keep @Column annotation in DTO classes");
        panel.add(featureKeepDtoColumnAnnotationCheckBox, BorderLayout.NORTH);

    }


    public Boolean getDtoKeepAnnotationColumn() {
        return Convert.toBool(featureKeepDtoColumnAnnotationCheckBox.isSelected(), true);
    }

    public void setFeatureKeepDtoColumnAnnotation(Boolean keepDtoColumnAnnotation) {
        featureKeepDtoColumnAnnotationCheckBox.setSelected(Convert.toBool(keepDtoColumnAnnotation,true));
    }


}
