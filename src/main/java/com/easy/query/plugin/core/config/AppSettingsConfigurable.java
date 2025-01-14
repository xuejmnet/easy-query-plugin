package com.easy.query.plugin.core.config;

import com.easy.query.plugin.windows.AppSettingsComponent;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AppSettingsConfigurable implements SearchableConfigurable {

    private AppSettingsComponent mySettingsComponent;

    private final AppSettings appSettings;

    public AppSettingsConfigurable() {
        this.appSettings = AppSettings.getInstance();
    }


    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return mySettingsComponent.getMainPanel();
    }

    @Override
    public @NotNull @NonNls String getId() {
        return "EasyQuery";
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "EasyQuery";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (mySettingsComponent == null) {
            mySettingsComponent = new AppSettingsComponent();
        }
        return mySettingsComponent.getMainPanel();
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        AppSettings.State state = AppSettings.getInstance().getState();
        state.setAuthor(mySettingsComponent.getAuthor().getText());
        state.setNormalDtoAnnoRemove(mySettingsComponent.getNormalDtoAnnoRemove().getText());
        state.setRequestDtoAnnoRemove(mySettingsComponent.getRequestDtoAnnoRemove().getText());
        state.setResponseDtoAnnoRemove(mySettingsComponent.getResponseDtoAnnoRemove().getText());
        state.setExcelDtoAnnoRemove(mySettingsComponent.getExcelDtoAnnoRemove().getText());
        state.setFieldMissAnnotationsContent(mySettingsComponent.getFieldMissAnnotationsContentText());

    }

    @Override
    public void reset() {
        AppSettings.State state = AppSettings.getInstance().getState();
        mySettingsComponent.getAuthor().setText(state.getAuthor());
        mySettingsComponent.getNormalDtoAnnoRemove().setText(state.getNormalDtoAnnoRemove());
        mySettingsComponent.getRequestDtoAnnoRemove().setText(state.getRequestDtoAnnoRemove());
        mySettingsComponent.getResponseDtoAnnoRemove().setText(state.getResponseDtoAnnoRemove());
        mySettingsComponent.getExcelDtoAnnoRemove().setText(state.getExcelDtoAnnoRemove());
        mySettingsComponent.getFieldMissAnnotationsContent().setText(state.getFieldMissAnnotationsContent());
    }

    @Override
    public void disposeUIResources() {
        mySettingsComponent = null;
    }
}
