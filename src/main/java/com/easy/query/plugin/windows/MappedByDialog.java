package com.easy.query.plugin.windows;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.entity.MappedByItem;
import com.easy.query.plugin.core.render.TextListCellRenderer;
import com.easy.query.plugin.core.util.DialogUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class MappedByDialog extends JDialog {
    private Map<String, MappedByItem> targetNavigate;
    private Project project;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextArea FieldText;
    private JList<String> FieldList;
    private JTextField NavigateText;

    public MappedByDialog(Map<String, MappedByItem> targetNavigate, Consumer<String> consumer, Project project) {
        this.targetNavigate = targetNavigate;
        this.project = project;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        // 获取屏幕的大小
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(600, (int) screenSize.getWidth() - 50), Math.min(300, (int) (screenSize.getHeight() * 0.9)));
        setTitle("MappedBy");
        DialogUtil.centerShow(this);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                onOK(consumer);
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


        DefaultListModel<String> model = new DefaultListModel<>();
        // tableNameSet按照字母降序
        List<String> entityNameList = new ArrayList<>(targetNavigate.keySet());
        Collections.sort(entityNameList);
        model.addAll(entityNameList);
        FieldList.setModel(model);
        TextListCellRenderer cellRenderer = new TextListCellRenderer();
        FieldList.setCellRenderer(cellRenderer);
        FieldList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String fieldName = FieldList.getSelectedValue();
                MappedByItem mappedByItem = targetNavigate.get(fieldName);
                if (mappedByItem == null) {
                    FieldText.setText("无法获取当前属性内容:fieldName");
                } else {
                    FieldText.setText(formatAnnotation(mappedByItem.filedText));
                }
            }
        });
    }

    public static String formatAnnotation(String input) {
        if(input==null){
            return null;
        }

        // 在逗号后面加换行和缩进
        String formatted = input
            .replaceAll(",\\s*", ",\n\t");


        // 如果只是单独一个 )，也换行
        formatted = formatted.replaceAll("\\)\\s*", "\n)");
        // 如果遇到 ) 后紧跟 private，就换行
        formatted = formatted.replaceAll("\\)\\s*private", ")\nprivate");

        return formatted;
    }
    private void onOK(Consumer<String> consumer) {
        // add your code here


        String text = NavigateText.getText();
        if (StrUtil.isBlank(text)) {
            Messages.showErrorDialog(project, "请输入MappedBy属性名", "错误提示");
            return;
        }
        String trimFieldName = StrUtil.trim(text);
        if (targetNavigate.containsKey(trimFieldName)) {
            Messages.showErrorDialog(project, "输入的MappedBy属性名:"+trimFieldName+"已存在", "错误提示");
            return;
        }
        consumer.accept(text);
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
}
