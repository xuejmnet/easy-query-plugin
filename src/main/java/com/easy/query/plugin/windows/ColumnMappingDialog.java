package com.easy.query.plugin.windows;

import cn.hutool.core.util.ObjectUtil;
import com.easy.query.plugin.core.entity.MatchTypeMapping;
import com.easy.query.plugin.core.util.DialogUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.ProjectUtils;
import com.easy.query.plugin.core.util.TableUtils;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.intellij.ui.components.fields.ExtendableTextField;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ColumnMappingDialog extends JDialog {
    private final Project project;
    private final Consumer<Map<String, List<MatchTypeMapping>>> saveMapping;
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton addBtn;
    private JButton removeBtn;
    private JButton reset;
    private JTable table;
    String[] HEADER = {"Match Type", "Column Type", "Java Type"};
    Object[][] TABLE_DATA = {
            {"Match Type", "Column Type", "Java Type"}
    };
    Map<String, List<MatchTypeMapping>> typeMapping;

    public ColumnMappingDialog(Project project, Map<String, List<MatchTypeMapping>> oldTypeMapping, Consumer<Map<String, List<MatchTypeMapping>>> saveMapping) {
        this.project = project;
        this.saveMapping = saveMapping;
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);
        setTitle("自定义类型映射");
        setSize(1000, 600);
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


        typeMapping =oldTypeMapping==null? TableUtils.getDefaultTypeMappingMap():new HashMap<>(oldTypeMapping);
        TABLE_DATA = new Object[typeMapping.size()][];

        addBtn.addActionListener(e -> {
            typeMapping = getTableData();
            typeMapping.computeIfAbsent("REGEX", k -> new ArrayList<>()).add(new MatchTypeMapping("REGEX", "", ""));
            initTableData();
            table.setModel(getDataModel());
            setColumnInput();
        });
        removeBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            TableModel model = table.getModel();
            if (selectedRow == -1 || ObjectUtil.isNull(model)) {
                return;
            }
            if (table.isEditing()) {
                table.getCellEditor().stopCellEditing();
            }
            String type = model.getValueAt(selectedRow, 0).toString();
            String valueAt = model.getValueAt(selectedRow, 1).toString();
            typeMapping = getTableData();
            typeMapping.get(type).removeIf(mapping -> mapping.getColumType().equals(valueAt));
            initTableData();
        });
        initTableData();

        reset.addActionListener(e -> {
            int flag = Messages.showYesNoDialog(project,"确定重置吗？", "提示", AllIcons.General.QuestionDialog);
            if (flag == 0) {
                NotificationUtils.notifySuccess(project,"重置成功");
                typeMapping = TableUtils.getDefaultTypeMappingMap();
                initTableData();
            }
        });
    }

    private void onOK() {
        // add your code here
        saveMapping.accept(getTableData());
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
    private Map<String, List<MatchTypeMapping>> getTableData() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        TableModel model = table.getModel();
        int rowCount = model.getRowCount();
        Map<String, List<MatchTypeMapping>> typeMappingMap = new HashMap<>();
        try {
            for (int row = 0; row < rowCount; row++) {
                String matchType = model.getValueAt(row, 0).toString();
                String column = model.getValueAt(row, 1).toString().toLowerCase();
                String javaField = model.getValueAt(row, 2).toString();
                MatchTypeMapping mapping = new MatchTypeMapping(matchType, javaField, column);
                // typeMappingMap.put(column, javaField);
                typeMappingMap.computeIfAbsent(matchType, k -> new ArrayList<>()).add(mapping);
            }
        } catch (Exception e) {

        }
        return typeMappingMap;
    }
    private void initTableData() {
        List<MatchTypeMapping> list = typeMapping.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        TABLE_DATA = new Object[list.size() > 0 ? list.size() : 1][];
        int idx = 0;
        for (MatchTypeMapping mapping : list) {
            TABLE_DATA[idx] = new Object[]{mapping.getType(), mapping.getColumType(), mapping.getJavaField()};
            idx++;
        }

        table.setModel(getDataModel());
        setColumnInput();
    }
    private void setColumnInput() {
        TableColumn comboBoxColumn = table.getColumnModel().getColumn(2);
        TableColumn type = table.getColumnModel().getColumn(0);

        ExtendableTextField textField = new ExtendableTextField();
        ExtendableTextComponent.Extension browseExtension =
                ExtendableTextComponent.Extension.create(AllIcons.Actions.Find, AllIcons.Actions.Find,
                        "选择java类型", () -> {
                            TreeClassChooserFactory chooserFactory = TreeClassChooserFactory.getInstance(project);
                            TreeClassChooser chooser = chooserFactory.createAllProjectScopeChooser("选择类");
                            chooser.showDialog();
                            PsiClass selected = chooser.getSelected();
                            if (ObjectUtil.isNull(selected)) {
                                return;
                            }
                            String qualifiedName = selected.getQualifiedName();
                            textField.setText(qualifiedName);
                            // 重新渲染 table 需要重新设置事件
                            setColumnInput();
                        });
        textField.addExtension(browseExtension);
        comboBoxColumn.setCellEditor(new DefaultCellEditor(textField));
        JComboBox<Object> box = new JComboBox<>();
        box.addItem("REGEX");
        box.addItem("ORDINARY");
        type.setCellEditor(new DefaultCellEditor(box));

    }
    private DefaultTableModel getDataModel() {
        return new DefaultTableModel(TABLE_DATA, HEADER) {
            boolean[] canEdit = {true, true, true};

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        };
    }
}
