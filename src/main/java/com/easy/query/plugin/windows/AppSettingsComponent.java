package com.easy.query.plugin.windows;

import lombok.Getter;

import javax.swing.*;


@Getter
public class AppSettingsComponent {
    private JTextArea responseDtoAnnoRemove;
    private JTextArea normalDtoAnnoRemove;
    private JTextArea requestDtoAnnoRemove;
    private JTextArea excelDtoAnnoRemove;
    private JPanel mainPanel;
    private JTextField author;
    private JTextArea FieldMissAnnotationsContent;


    public JPanel getPanel() {
        return mainPanel;
    }

    public String getResponseDtoAnnoRemoveText() {
        return responseDtoAnnoRemove.getText();
    }

    public void setResponseDtoAnnoRemoveText(String text) {
        responseDtoAnnoRemove.setText(text);
    }

    public String getNormalDtoAnnoRemoveText() {
        return normalDtoAnnoRemove.getText();
    }

    public void setNormalDtoAnnoRemoveText(String text) {
        normalDtoAnnoRemove.setText(text);
    }

    public String getRequestDtoAnnoRemoveText() {
        return requestDtoAnnoRemove.getText();
    }

    public void setRequestDtoAnnoRemoveText(String text) {
        requestDtoAnnoRemove.setText(text);
    }

    public String getExcelDtoAnnoRemoveText() {
        return excelDtoAnnoRemove.getText();
    }

    public void setExcelDtoAnnoRemoveText(String text) {
        excelDtoAnnoRemove.setText(text);
    }

    public String getFieldMissAnnotationsContentText() {
        return FieldMissAnnotationsContent.getText();
    }

    public void setFieldMissAnnotationsContentText(String text) {
        FieldMissAnnotationsContent.setText(text);
    }


}
