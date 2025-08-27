package com.easy.query.plugin.windows.ui.dto2ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * create time 2024/3/5 11:24
 * 文件说明
 *
 * @author xuejiaming
 */
public class Main extends JFrame {

    private static final long serialVersionUID = 4648172894076113183L;
    protected static TreeModel getDefaultTreeModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("JTree");
        DefaultMutableTreeNode      parent;

        parent = new DefaultMutableTreeNode("colors");
        root.add(parent);
        parent.add(new DefaultMutableTreeNode("blue"));
        parent.add(new DefaultMutableTreeNode("violet"));
        parent.add(new DefaultMutableTreeNode("red"));
        parent.add(new DefaultMutableTreeNode("yellow"));

        parent = new DefaultMutableTreeNode("sports");
        root.add(parent);
        parent.add(new DefaultMutableTreeNode("basketball"));
        parent.add(new DefaultMutableTreeNode("soccer"));
        parent.add(new DefaultMutableTreeNode("football"));
        parent.add(new DefaultMutableTreeNode("hockey"));

        parent = new DefaultMutableTreeNode("food");
        root.add(parent);
        parent.add(new DefaultMutableTreeNode("hot dogs"));
        parent.add(new DefaultMutableTreeNode("pizza"));
        parent.add(new DefaultMutableTreeNode("ravioli"));
        parent.add(new DefaultMutableTreeNode("bananas"));
        return new DefaultTreeModel(root);
    }
    public Main() {
        super();
        setSize(500, 500);
        this.getContentPane().setLayout(new BorderLayout());
        final JCheckBoxTree cbt = new JCheckBoxTree();
        this.getContentPane().add(cbt);
        cbt.addCheckChangeEventListener(new JCheckBoxTree.CheckChangeEventListener() {
            public void checkStateChanged(JCheckBoxTree.CheckChangeEvent event) {
                TreePath[] paths = cbt.getCheckedPaths();
                for (TreePath tp : paths) {
                    for (Object pathPart : tp.getPath()) {
                        System.out.print(pathPart + ",");
                    }
                    System.out.println();
                }
            }
        });
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
    }

    public static void main(String args[]) {
        Main m = new Main();
        m.setVisible(true);
    }
}