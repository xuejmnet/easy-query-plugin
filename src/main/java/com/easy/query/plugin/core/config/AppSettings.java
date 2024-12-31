package com.easy.query.plugin.core.config;

import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

@State(name = "EasyQuerySettings", storages = @Storage("easy-query-plugin.xml"))
public class AppSettings implements PersistentStateComponent<AppSettings.State> {

    private State myState = new State();

    @Override
    public @Nullable AppSettings.State getState() {
        return myState;
    }

    @Override
    public void loadState(@NotNull State state) {
        myState= state;
    }

    @Getter
    @Setter
    public static class State {
        private String author;
        private String normalDtoAnnoRemove;
        private String requestDtoAnnoRemove;
        private String responseDtoAnnoRemove;
        private String excelDtoAnnoRemove;

        public List<String> getRemoveAnnoList(JRadioButton dtoSchemaNormal, JRadioButton dtoSchemaRequest, JRadioButton dtoSchemaResponse, JRadioButton dtoSchemaExcel) {
            if (dtoSchemaRequest.isSelected()) {
                return StrUtil.split(requestDtoAnnoRemove,"\n",true,true);
            }else if (dtoSchemaResponse.isSelected()) {
                return StrUtil.split(responseDtoAnnoRemove, "\n", true, true);
            }else if (dtoSchemaExcel.isSelected()) {
                return StrUtil.split(excelDtoAnnoRemove, "\n", true, true);
            }
            return StrUtil.split(normalDtoAnnoRemove, "\n", true, true);
        }
    }

    public static AppSettings getInstance() {
        return ApplicationManager.getApplication().getService(AppSettings.class);
    }


}
