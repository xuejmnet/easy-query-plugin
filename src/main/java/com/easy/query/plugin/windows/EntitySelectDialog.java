package com.easy.query.plugin.windows;

import cn.hutool.core.util.NumberUtil;
import com.easy.query.plugin.config.EasyQueryPluginSetting;
import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.entity.ClassNode;
import com.easy.query.plugin.core.entity.struct.StructDTOContext;
import com.easy.query.plugin.core.entity.struct.StructDTOEntityContext;
import com.easy.query.plugin.core.persistent.EasyQueryQueryPluginConfigData;
import com.easy.query.plugin.core.render.EntityListCellRenderer;
import com.easy.query.plugin.core.util.DialogUtil;
import com.easy.query.plugin.core.util.EasyQueryConfigUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.StrUtil;
import com.easy.query.plugin.core.util.StructDTOUtil;
import com.easy.query.plugin.core.validator.InputAnyValidatorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.ui.DocumentAdapter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class EntitySelectDialog extends JDialog {
    private StructDTOEntityContext structDTOEntityContext;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JList<String> entityList;
    private JTextField searchEntity;
    private JButton settingBtn;
    EasyQueryPluginSetting pluginSetting;
    List<String> entityNameList;

    public EntitySelectDialog(StructDTOEntityContext structDTOEntityContext) {
        this.structDTOEntityContext = structDTOEntityContext;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
// 获取屏幕的大小
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(800, (int) screenSize.getWidth() - 50), (int) Math.min(900, (int) (screenSize.getHeight() * 0.9)));
        setTitle("Struct DTO Entity Select");
        DialogUtil.centerShow(this);
        Project project = structDTOEntityContext.getProject();
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
        this.pluginSetting = EasyQueryConfigUtil.getPluginSetting(project);
        settingBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String dtoColumnsIgnore = pluginSetting.getDTOColumnsIgnore();


                ModelTemplateEditorDialog modelTemplateEditorDialog = new ModelTemplateEditorDialog(project, dtoColumnsIgnore,true, newTemplate -> {
                    pluginSetting.saveDTOColumnsIgnore(newTemplate, project);
                    NotificationUtils.notifySuccess("保存成功", project);
                });
                modelTemplateEditorDialog.setVisible(true);
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
        Map<String, PsiClass> entityMap = structDTOEntityContext.getEntityClass();
        // tableNameSet按照字母降序
        this.entityNameList = new ArrayList<>(entityMap.keySet());
        Collections.sort(entityNameList);
        model.addAll(entityNameList);
        entityList.setModel(model);
        EntityListCellRenderer cellRenderer = new EntityListCellRenderer(entityMap);
        entityList.setCellRenderer(cellRenderer);
        entityList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {

                    String entityName = entityList.getSelectedValue();
                    ok0(entityName);
                } else {
                    super.mouseClicked(e);
                }
            }
        });


        searchEntity.getDocument().addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(@NotNull DocumentEvent e) {
                String entityName = searchEntity.getText();
                if (StringUtils.isNotBlank(entityName)) {
                    if (!entityList.isSelectionEmpty()) {
                        entityList.clearSelection();
                    }
                    String searchName = entityName.trim();
                    Set<String> search = entityNameList.stream().filter(o -> o.contains(searchName)).collect(Collectors.toSet());
                    model.removeAllElements();
                    model.addAll(search);
                } else {
                    model.removeAllElements();
                    model.addAll(entityNameList);
                }
            }
        });
    }

    private void onOK() {
        // add your code here

        List<String> selectedEntityList = entityList.getSelectedValuesList();
        if (CollectionUtils.isEmpty(selectedEntityList)) {
            Messages.showWarningDialog("请选择要生成的表", "提示");
            return;
        }
        if (selectedEntityList.size() != 1) {
            Messages.showWarningDialog("请选择要生成的单个对象", "提示");
            return;
        }

        String entityName = selectedEntityList.get(0);
        boolean oked = ok0(entityName);
        if (!oked) {
            return;
        }
        dispose();
    }

    public boolean ok0(String entityName) {
        Project project = structDTOEntityContext.getProject();
        Map<String, PsiClass> entityClass = structDTOEntityContext.getEntityClass();
        PsiClass psiClass = entityClass.get(entityName);
        if (psiClass == null) {
            Messages.showWarningDialog("无法找到对象的类型:" + entityName, "提示");
            return false;
        }
//        Set<String> ignoreColumns = getIgnoreColumns(project);

        Messages.InputDialog dialog = new Messages.InputDialog("请输入树形深度,无限级输入-1", "树形深度", Messages.getQuestionIcon(), "5", new InputAnyValidatorImpl());

        dialog.show();
        if (!dialog.isOK()) {
            return false;
        }
        String settingVal = dialog.getInputString();
        if (StrUtil.isBlank(settingVal)) {
            Messages.showWarningDialog("无法读取树形深度", "提示");
            return false;
        }
        boolean integer = NumberUtil.isInteger(settingVal);
        if (!integer) {
            Messages.showWarningDialog("树形深度:[" + settingVal + "]只能是数字", "提示");
            return false;
        }
        int deepMax = Integer.parseInt(settingVal);


        Map<String, Map<String, ClassNode>> entityProps = new HashMap<>();
        List<ClassNode> classNodes = new ArrayList<>();
        LinkedHashSet<String> imports = new LinkedHashSet<>();
        StructDTOUtil.parseClassList(deepMax, project, entityName, psiClass, structDTOEntityContext.getEntityClass(), entityProps, classNodes, imports, new HashSet<>());
        StructDTOContext structDTOContext = new StructDTOContext(project, structDTOEntityContext.getPath(), structDTOEntityContext.getPackageName(), structDTOEntityContext.getModule(), entityProps);
        structDTOContext.getImports().addAll(imports);

        // 传递 DTO className 到下一个窗口上下文
        String dtoClassName = structDTOEntityContext.getDtoClassName();
        PsiClass dtoPsiClass = structDTOEntityContext.getDtoPsiClass();
        structDTOContext.setDtoClassName(dtoClassName);
        structDTOContext.setDtoPsiClass(dtoPsiClass);

        StructDTODialog structDTODialog = new StructDTODialog(structDTOContext, classNodes);

        structDTODialog.setVisible(true);

        if (!structDTOContext.isSuccess()) {
            return false;
        }
        return true;
    }


    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
//    public static void main(String[] args) {
//        EntitySelectDialog dialog = new EntitySelectDialog();
//        dialog.pack();
//        dialog.setVisible(true);
//        System.exit(0);
//    }
}
