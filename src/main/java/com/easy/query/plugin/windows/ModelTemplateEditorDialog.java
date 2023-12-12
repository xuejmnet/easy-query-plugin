package com.easy.query.plugin.windows;

import com.easy.query.plugin.core.util.DialogUtil;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.xml.XMLLanguage;
import com.intellij.openapi.project.Project;
import com.intellij.ui.LanguageTextField;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

public class ModelTemplateEditorDialog extends JDialog {
    private final Project project;
    private final Consumer<String> okFunction;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private LanguageTextField modelTemplateText;

    public ModelTemplateEditorDialog(Project project, String modelTemplate, Consumer<String> okFunction) {
        this.project = project;
        this.okFunction = okFunction;
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
        okFunction.accept(modelTemplateText.getText());
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
