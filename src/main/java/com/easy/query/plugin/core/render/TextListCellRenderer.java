package com.easy.query.plugin.core.render;

import com.intellij.psi.PsiClass;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * create time 2023/11/29 13:57
 * 文件说明
 *
 * @author xuejiaming
 */
public class TextListCellRenderer extends JLabel implements ListCellRenderer<String> {
    private JLabel label;
    private JLabel rowEndLabel;
    String searchTableName;


    public String getSearchTableName() {
        return searchTableName;
    }

    public void setSearchTableName(String searchTableName) {
        this.searchTableName = searchTableName;
    }

    public TextListCellRenderer() {
        setOpaque(true);
        setLayout(new BorderLayout());
        label = new JLabel();
        rowEndLabel = new JLabel();
        rowEndLabel.setForeground(Color.GRAY);
        add(label, BorderLayout.WEST);
        add(rowEndLabel, BorderLayout.EAST);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        label.setText(value);
//        rowEndLabel.setText(entityInfoMap.get(value).getName());
        if (isSelected) {
            setBackground(list.getSelectionBackground());
            setForeground(list.getSelectionForeground());
        } else {
            setBackground(list.getBackground());
            setForeground(list.getForeground());
        }
        return this;
    }
}

