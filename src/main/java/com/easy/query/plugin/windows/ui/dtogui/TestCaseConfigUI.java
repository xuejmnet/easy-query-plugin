package com.easy.query.plugin.windows.ui.dtogui;

import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.util.ArrayList;
// reference data: https://blog.csdn.net/weixin_33735910/article/details/114123652

public class TestCaseConfigUI {
    private JFrame frame;
    private JTree tree;
    private CheckBoxTreeNode rootNode;
    private ArrayList<String> existCasesList;

    public void showUI(Project project){
        frame =new JFrame("TestCase Config");
//        Toolkit toolkit=Toolkit.getDefaultToolkit(); // 获取Toolkit对象
//        Image icon = toolkit.getImage("D:\\shao\\android.png"); // 获取图片对象
//        frame.setIconImage(icon);
        frame.setBounds(200,200,650,500);
        JPanel panel = new JPanel();
        BoxLayout boxLayout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(boxLayout);
        File file = new File(project.getBasePath(), "scripts");
        if(file.exists()){
            if(file.listFiles().length>0){
                readExistCases(project);
                tree =new JTree();
                rootNode =new CheckBoxTreeNode("scripts");
                listFiles(file, rootNode);
                DefaultTreeModel model =new DefaultTreeModel(rootNode);
                tree.addMouseListener(new CheckBoxTreeNodeSelectionListener());
                tree.setModel(model);
                tree.setCellRenderer(new CheckBoxTreeCellRenderer());
                JScrollPane scroll =new JScrollPane(tree);
                panel.add(scroll);
            }
        }
        frame.getContentPane().add(panel);
        addFrameFooter(frame, panel, project);
        frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        frame.setVisible(true);
    }

    private void readExistCases(Project project){
        /*
        function :read existed testcases from project.txt, for set status of these testcases is selected.
        params project :project object, can get project path from it.
         */
        existCasesList = new ArrayList<>();
        File configFile = new File(project.getBasePath(),"project_scripts.txt");
        if(configFile.exists()){
            try {
                BufferedReader reader = new BufferedReader(new FileReader(configFile));
                for(Object msg:reader.lines().toArray()){
                    String[] msgSplits=msg.toString().split("/");
                    existCasesList.add(msgSplits[msgSplits.length-1]);
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void listFiles(File file, CheckBoxTreeNode node){
        /*
        function :Add scripts to CheckBoxTreeNode by recursion
         */
        if(file.isDirectory()){
            File[] _file= file.listFiles();
            for(int i=0; i<_file.length; i++){
                if(_file[i].getName().equals("__pycache__")){
                    continue;
                }
                CheckBoxTreeNode child =new CheckBoxTreeNode(_file[i].getName());
                node.add(child);
                listFiles(_file[i], child);
            }
        }else{
            if(existCasesList.contains(node.getUserObject().toString())){
                node.setSelected(true);
            }
        }
    }

    public void addFrameFooter(JFrame jFrame, JPanel jPanel, Project project){
        /*
        function :add the footer part of the frame
        params jFrame :the main window of frame
        params jPanel :the footer's parent widget
        params project :for get project information
         */
        JPanel panelFooter = new JPanel();
        panelFooter.setLayout(new BoxLayout(panelFooter, BoxLayout.X_AXIS));

        panelFooter.add(new JPanel());
        panelFooter.add(new JPanel());

        JPanel panel = new JPanel();
        JButton btnSave=new JButton("  Save  ");
        JButton btnCancel=new JButton("Cancel");
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(rootNode!=null){
                    try {
                        File file = new File(project.getBasePath(),"project_scripts.txt");
                        if(!file.exists()){
                            file.createNewFile();
                        }
                        BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
                        configSelectedTestcase(rootNode, writer);
                        writer.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }finally {
                        jFrame.dispose();
                    }
                }
            }
        });
        btnCancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                jFrame.dispose();
//                System.exit(0);
            }
        });
        panel.add(btnSave);
        panel.add(btnCancel);
        panelFooter.add(panel);
        jPanel.add(panelFooter);
    }

    private void configSelectedTestcase(CheckBoxTreeNode node, BufferedWriter fileWriter){
        /*
        function :write selected testcases to config file when click the button of save.
        params node :tree node object
        params fileWriter :the object of writing testcases
         */
        if(!node.isLeaf()){
            for (int j=0; j<node.getChildCount(); j++){
                CheckBoxTreeNode childNode = (CheckBoxTreeNode)node.getChildAt(j);
                configSelectedTestcase(childNode, fileWriter);
            }
        }else{
            if(!node.isSelected){
                return;
            }
            Object[] objects = node.getUserObjectPath();
            StringBuilder builder = new StringBuilder();
            for(int i=0; i<objects.length;i++){
                if(i==0){
                    builder.append(objects[i].toString());
                }else{
                    builder.append("/"+objects[i].toString());
                }
            }
            try {
                fileWriter.write(builder.toString()+"\n");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void main(String[] args) {

    }
}
