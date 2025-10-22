package com.easy.query.plugin.windows;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.easy.query.plugin.core.util.DialogUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.LanguageTextField;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

public class ModelTemplateEditorDialog extends JDialog {
    private Project project;
    private Consumer<String> okFunction;
    private boolean shouldJson;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private LanguageTextField modelTemplateText;

    public ModelTemplateEditorDialog(Project project, String modelTemplate, boolean shouldJson, Consumer<String> okFunction) {
        this.project = project;
        this.okFunction = okFunction;
        this.shouldJson = shouldJson;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setSize(500, 700);
        setTitle("Model Template");
        DialogUtil.centerShow(this);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        modelTemplateText.setText(modelTemplate);
    }

    private void onOK() {
        // add your code here
        String inputJson = modelTemplateText.getText();
        if (shouldJson) {
            if (StrUtil.isBlank(inputJson)) {
                okFunction.accept("{}");
                return;
            }
            try {

                LinkedHashMap<String, String> configMap = JSONObject.parseObject(inputJson,
                    new TypeReference<LinkedHashMap<String, String>>() {
                    });
            } catch (Exception ex) {
                Messages.showWarningDialog("输入内容为Map<String,String>,其中key为按钮名称,value为要忽略的值", "提示");
                return;
            }
        }
        okFunction.accept(inputJson);
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    /**
     * 创建自定义控件
     */
    private void createUIComponents() {
        modelTemplateText = new LanguageTextField(JavaLanguage.INSTANCE, project, "", false);
    }
}
