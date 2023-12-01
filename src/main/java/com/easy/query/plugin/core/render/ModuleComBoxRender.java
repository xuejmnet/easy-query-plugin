package com.easy.query.plugin.core.render;

import com.intellij.icons.AllIcons;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * create time 2023/11/30 13:26
 * 文件说明
 *
 * @author xuejiaming
 */
public class ModuleComBoxRender extends JLabel implements ListCellRenderer<String> {
    // @Override
    // public Component getListCellRendererComponent(JList list, String value, int index, boolean isSelected, boolean cellHasFocus) {
    //         setText(value);
    //         setIcon(AllIcons.Nodes.Module);
    //     return this;
    // }
    private JLabel label;
    Map<String, String> highlightKey=new HashMap<>();

    public void setHighlightKey(Map<String, String> highlightKey) {
        this.highlightKey = highlightKey;
    }

    public ModuleComBoxRender() {
        setOpaque(true);
        setLayout(new BorderLayout());
        label = new JLabel();
        label.setIcon(AllIcons.Nodes.Module);
        add(label, BorderLayout.WEST);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {

        if (highlightKey.containsKey(value)) {
            label.setText(highlightKey.get(value));
            setText("");
        }else{
            setIcon(AllIcons.Nodes.Module);
            setText(value);
        }
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