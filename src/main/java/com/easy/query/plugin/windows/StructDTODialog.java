package com.easy.query.plugin.windows;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.TypeReference;
import com.easy.query.plugin.core.RenderEasyQueryTemplate;
import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.entity.ClassNode;
import com.easy.query.plugin.core.entity.PropAppendable;
import com.easy.query.plugin.core.entity.StructDTOApp;
import com.easy.query.plugin.core.entity.StructDTOProp;
import com.easy.query.plugin.core.entity.TreeClassNode;
import com.easy.query.plugin.core.entity.struct.RenderStructDTOContext;
import com.easy.query.plugin.core.entity.struct.StructDTOContext;
import com.easy.query.plugin.core.persistent.EasyQueryQueryPluginConfigData;
import com.easy.query.plugin.core.util.DialogUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.validator.InputAnyValidatorImpl;
import com.easy.query.plugin.windows.ui.dto2ui.JCheckBoxTree;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class StructDTODialog extends JDialog {
    private final StructDTOContext structDTOContext;
    private final List<ClassNode> classNodes;
    private TreeModel treeModel;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JCheckBoxTree entityProps;
    private JCheckBox combineCk;
    private JCheckBox dataCheck;
    private JPanel dynamicBtnPanel;
    private Map<String, String> buttonMaps;

    private TreeModel initTree(List<ClassNode> classNodes) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Entities");
        for (ClassNode classNode : classNodes) {
            DefaultMutableTreeNode parent = new DefaultMutableTreeNode(classNode);
            root.add(parent);
            initProps(parent, classNode);
        }
        return new DefaultTreeModel(root);
    }

    private void initProps(DefaultMutableTreeNode parent, ClassNode classNode) {
        if (CollUtil.isEmpty(classNode.getChildren())) {
            return;
        }
        for (ClassNode child : classNode.getChildren()) {
            DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(child);
            parent.add(childNode);
            initProps(childNode, child);
        }
    }

    public StructDTODialog(StructDTOContext structDTOContext, List<ClassNode> classNodes) {
        this.structDTOContext = structDTOContext;
        this.classNodes = classNodes;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setSize(800, 900);
        setTitle("Struct DTO");
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
        dataCheck.setSelected(true);
        BoxLayout boxLayout = new BoxLayout( dynamicBtnPanel, BoxLayout.LINE_AXIS );
        dynamicBtnPanel.setLayout( boxLayout );
        dynamicIgnoreButtons(structDTOContext.getProject());
    }
    private void dynamicIgnoreButtons(Project project){
        this.buttonMaps=new LinkedHashMap<>();
        EasyQueryConfig config = EasyQueryQueryPluginConfigData.getAllEnvStructDTOIgnore(new EasyQueryConfig());
        if (config.getConfig() == null) {
            config.setConfig(new HashMap<>());
        }
        String projectName = project.getName();
        String setting = config.getConfig().get(projectName);
        initIgnoreButtons(setting);
        for (Map.Entry<String, String> kv : buttonMaps.entrySet()) {
            JButton jButton = new JButton(kv.getKey());
            jButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    onIgnoreCancel(kv.getKey());
                }
            });
            dynamicBtnPanel.add(jButton);
        }

    }

    private void onIgnoreCancel(String key){
        String s = this.buttonMaps.get(key);
        if(s!=null){
            List<String> ignoreProperties = Arrays.asList(s.split(","));

            TreePath[] checkedPaths = entityProps.getCheckedPaths();
            if (checkedPaths == null || checkedPaths.length == 0) {
                return;
            }
            long count = Arrays.stream(checkedPaths).filter(o -> o.getPathCount() == 2).count();
            if (count != 1) {
                return;
            }
            for (String ignoreProperty : ignoreProperties) {
                this.entityProps.removeCheckedPathsByName(ignoreProperty);
            }
        }
    }
    private void initIgnoreButtons(String setting){
        try {

            LinkedHashMap<String, String> configMap = JSONObject.parseObject(setting, new TypeReference<LinkedHashMap<String, String>>() {
            });
            buttonMaps.putAll(configMap);
        }catch (Exception ignored){

        }
    }

    private void onOK() {
        // add your code here

        Object root = this.treeModel.getRoot();
        if (root instanceof TreeModel) {
            TreeModel treeModelRoot = (TreeModel) root;
            System.out.println(treeModelRoot);
        }
        TreePath[] checkedPaths = entityProps.getCheckedPaths();
        if (checkedPaths == null || checkedPaths.length == 0) {
            NotificationUtils.notifySuccess("请选择节点", structDTOContext.getProject());
            return;
        }
        long count = Arrays.stream(checkedPaths).filter(o -> o.getPathCount() == 2).count();
        if (count != 1) {
            NotificationUtils.notifySuccess("请选择一个对象节点", structDTOContext.getProject());
            return;
        }


        List<TreeClassNode> nodeList = Arrays.stream(checkedPaths).filter(o -> o.getPathCount() > 1)
                .map(o -> {
                    int pathCount = o.getPathCount();
                    ClassNode classNode = (ClassNode) ((DefaultMutableTreeNode) o.getLastPathComponent()).getUserObject();
                    return new TreeClassNode(pathCount, classNode);
                }).sorted((a, b) -> {
                    if (a.getPathCount() != b.getPathCount()) {
                        return a.getPathCount() - b.getPathCount();
                    } else {
                        return a.getClassNode().getSort() - b.getClassNode().getSort();
                    }
                }).collect(Collectors.toList());


        Iterator<TreeClassNode> iterator = nodeList.iterator();
        TreeClassNode appNode = iterator.next();
        ClassNode app = appNode.getClassNode();
        StructDTOApp structDTOApp = new StructDTOApp(app.getName(), app.getOwner(), structDTOContext.getPackageName(), app.getSort());

        String entityDTOName = "InitDTOName";
        Messages.InputDialog dialog = new Messages.InputDialog("请输入DTO名称", "提示名称", Messages.getQuestionIcon(), StrUtil.subAfter(structDTOApp.getEntityName(), ".", true) + "DTO", new InputAnyValidatorImpl());
        dialog.show();
        if (dialog.isOK()) {
            String dtoName = dialog.getInputString();
            if (StrUtil.isBlank(dtoName)) {
                Messages.showErrorDialog(structDTOContext.getProject(), "输入的dto名称为空", "错误提示");
                return;
            }
            entityDTOName = dtoName;
        } else {
            return;
        }


        RenderStructDTOContext renderStructDTOContext = new RenderStructDTOContext(structDTOContext.getProject(), structDTOContext.getPath(), structDTOContext.getPackageName(), entityDTOName, structDTOApp, structDTOContext.getModule());
        renderStructDTOContext.setData(dataCheck.isSelected());
        renderStructDTOContext.getImports().addAll(structDTOContext.getImports());
        PropAppendable base = structDTOApp;
        int i = 0;


        while (iterator.hasNext()) {
            TreeClassNode treeClassNode = iterator.next();
            ClassNode classNode = treeClassNode.getClassNode();
            if (treeClassNode.getPathCount() > 3) {
//                    StructDTOProp structDTOProp = base.getProps().stream().filter(o -> o.isEntity() && Objects.equals(o.getSelfEntityType(), classNode.getOwner())&&Objects.equals(o.getPropName(), classNode.getOwnerPropertyName())).findFirst().orElse(null);
//                    if (structDTOProp == null) {
//                        break;
//                    }
                PropAppendable propAppendable = renderStructDTOContext.getEntities().stream().filter(o -> (o.getPathCount() + 1) == treeClassNode.getPathCount() && Objects.equals(o.getSelfEntityType(), classNode.getOwner()) && Objects.equals(o.getPropName(), classNode.getOwnerPropertyName())).findFirst().orElse(null);
                if (propAppendable == null) {
                    break;
                }
                base = propAppendable;
            }
            StructDTOProp structDTOProp = new StructDTOProp(classNode.getName(), classNode.getPropText(), classNode.getOwner(), classNode.isEntity(), classNode.getSelfEntityType(), classNode.getSort(), treeClassNode.getPathCount(),classNode.getOwnerFullName(),classNode.getSelfFullEntityType());
            structDTOProp.setClassNode(classNode);
            if (structDTOProp.isEntity()) {
                structDTOProp.setDtoName(entityDTOName + "_" + StrUtil.upperFirst(classNode.getName()));
                if (StringUtils.isNotBlank(structDTOProp.getPropText())) {
                    if (structDTOProp.getPropText().contains("<") && structDTOProp.getPropText().contains(">")) {
                        String regex = "<\\s*" + structDTOProp.getSelfEntityType() + "\\s*>";
                        String newPropText = structDTOProp.getPropText().replaceAll(regex, "<" + structDTOProp.getDtoName() + ">");
                        structDTOProp.setPropText(newPropText);
                    } else {

                        String regex = "private\\s+" + structDTOProp.getSelfEntityType();
                        String newPropText = structDTOProp.getPropText().replaceAll(regex, "private " + structDTOProp.getDtoName());
                        structDTOProp.setPropText(newPropText);
                    }
                    if (structDTOProp.getPropText().contains("@Navigate(") && StringUtils.isNotBlank(classNode.getRelationType())) {
                        String regex = "@Navigate\\(.*?\\)";

                        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
                        Matcher matcher = pattern.matcher(structDTOProp.getPropText());

                        if (matcher.find()) {
                            String replacement = "@Navigate(value = " + classNode.getRelationType() + ")";
                            String newPropText = matcher.replaceAll(replacement);
//                            String newPropText = structDTOProp.getPropText().replaceAll(regex, "@Navigate(value = " + classNode.getRelationType() + ")");
                            structDTOProp.setPropText(newPropText);
                        }
//                        String newPropText = structDTOProp.getPropText().replaceAll(regex, "@Navigate(value = " + classNode.getRelationType() + ")");
//                        structDTOProp.setPropText(newPropText);
                    }
                }
                renderStructDTOContext.getEntities().add(structDTOProp);
            }
            if (structDTOProp.getPropText().contains("@Column(")) {
                String regex = "@Column\\(.*?\\)";
                if (StringUtils.isNotBlank(classNode.getConversion())||StringUtils.isNotBlank(classNode.getColumnValue())) {
                    String columnText="@Column(";
                    if(StringUtils.isNotBlank(classNode.getColumnValue())){
                        columnText+="value = \""+classNode.getColumnValue()+"\"";
                        if(StringUtils.isNotBlank(classNode.getConversion())){
                            columnText+=",";
                        }
                    }
                    if(StringUtils.isNotBlank(classNode.getConversion())){
                        columnText+="conversion = "+classNode.getConversion();
                    }
                    columnText+=")";
                    String newPropText = structDTOProp.getPropText().replaceAll(regex, columnText);
//                    (conversion = " + classNode.getConversion() + ")

                    structDTOProp.setPropText(newPropText);
                } else {
                    String newPropText = structDTOProp.getPropText().replaceAll(regex, "");
                    structDTOProp.setPropText(newPropText);
                }
            }
            base.addProp(structDTOProp);
        }

        boolean b = RenderEasyQueryTemplate.renderStructDTOType(renderStructDTOContext);
        if (!b) {
            return;
        }
        NotificationUtils.notifySuccess("生成成功", structDTOContext.getProject());
        structDTOContext.setSuccess(true);
        dispose();
    }


    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    //    public static void main(String[] args) {
//        StructDTODialog dialog = new StructDTODialog();
//        dialog.pack();
//        dialog.setVisible(true);
//        System.exit(0);
//    }
    private void createUIComponents() {
        this.treeModel = initTree(classNodes);
        entityProps = new JCheckBoxTree(treeModel);
    }


//    protected static TreeModel getDefaultTreeModel() {
//        DefaultMutableTreeNode root = new DefaultMutableTreeNode("JTree");
//        DefaultMutableTreeNode      parent;
//
//        parent = new DefaultMutableTreeNode("colors");
//        root.add(parent);
//        parent.add(new DefaultMutableTreeNode("blue"));
//        parent.add(new DefaultMutableTreeNode("violet"));
//        parent.add(new DefaultMutableTreeNode("red"));
//        parent.add(new DefaultMutableTreeNode("yellow"));
//
//        parent = new DefaultMutableTreeNode("sports");
//        root.add(parent);
//        parent.add(new DefaultMutableTreeNode("basketball"));
//        parent.add(new DefaultMutableTreeNode("soccer"));
//        parent.add(new DefaultMutableTreeNode("football"));
//        parent.add(new DefaultMutableTreeNode("hockey"));
//
//        parent = new DefaultMutableTreeNode("food");
//        root.add(parent);
//        parent.add(new DefaultMutableTreeNode("hot dogs"));
//        parent.add(new DefaultMutableTreeNode("pizza"));
//        parent.add(new DefaultMutableTreeNode("ravioli"));
//        parent.add(new DefaultMutableTreeNode("bananas"));
//        return new DefaultTreeModel(root);
//    }
}
