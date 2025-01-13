package com.easy.query.plugin.core.config;

import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ProjectSettingsConfigurable implements SearchableConfigurable {

    private ProjectSettingsPanel projectSettingsPanel;

    private Project project;

    public ProjectSettingsConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return projectSettingsPanel.getPanel();
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "EasyQuery Project Settings";
    }

    @Override
    public @Nullable JComponent createComponent() {
        projectSettingsPanel = new ProjectSettingsPanel();
        return projectSettingsPanel.getPanel();
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void disposeUIResources() {
        projectSettingsPanel = null;
    }

    @Override
    public void apply() {
        ProjectSettings.State state = ProjectSettings.getInstance(project).getState();

        // 设置DTO上@Column只有value属性时是否保留
        state.setFeatureKeepDtoColumnAnnotation(projectSettingsPanel.getDtoKeepAnnotationColumn());
        // 设置数据库类型
        state.setDatabaseType(projectSettingsPanel.getDatabaseType());

    }

    @Override
    public void reset() {
        ProjectSettings.State state = ProjectSettings.getInstance(project).getState();
        // 设置DTO上@Column只有value属性时是否保留
        projectSettingsPanel.setFeatureKeepDtoColumnAnnotation(state.getFeatureKeepDtoColumnAnnotation());
        // 设置数据库类型
        projectSettingsPanel.setDatabaseType(state.getDatabaseType());
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "EasyQuery Project Settings";
    }
}
