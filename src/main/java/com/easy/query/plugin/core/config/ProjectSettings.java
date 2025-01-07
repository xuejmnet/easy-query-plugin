package com.easy.query.plugin.core.config;


import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Service({Service.Level.PROJECT})
@State(name = "EasyQueryProjectSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class ProjectSettings implements PersistentStateComponent<ProjectSettings.State> {

    private State projectState = new State();


    @Getter
    @Setter
    public static class State {
        private Boolean featureKeepDtoColumnAnnotation;
    }


    @Override
    public @Nullable ProjectSettings.State getState() {
        return projectState;
    }

    @Override
    public void loadState(@NotNull ProjectSettings.State projectSettings) {
        projectState = projectSettings;
    }


    public static ProjectSettings getInstance(Project project) {
        return project.getService(ProjectSettings.class);
    }
}
