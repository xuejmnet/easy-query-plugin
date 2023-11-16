package com.easy.query.plugin.windows;

import com.easy.query.plugin.core.util.BasicFormatter;
import com.easy.query.plugin.core.util.DialogUtil;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SQLPreviewDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonCancel;
    private JTextArea selectSQLText;
    private JTextArea previewSQLText;
    private JButton convertButton;
    private static final char MARK = '?';
    private static final BasicFormatter FORMATTER = new BasicFormatter();

    private static final Set<String> NEED_BRACKETS;


    static {
        Set<String> types = new HashSet<>(8);
        types.add("String");
        types.add("Date");
        types.add("Time");
        types.add("LocalDate");
        types.add("LocalTime");
        types.add("LocalDateTime");
        types.add("BigDecimal");
        types.add("Timestamp");
        NEED_BRACKETS = Collections.unmodifiableSet(types);
    }

    public SQLPreviewDialog(String selectSQL) {
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(convertButton);
        setTitle("SQL Preview");
        setSize(500, 800);
        DialogUtil.centerShow(this);

        convertButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onConvert();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        selectSQLText.setText(selectSQL);


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
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
//
//    public static void main(String[] args) {
//        SQLPreviewDialog dialog = new SQLPreviewDialog();
//        dialog.pack();
//        dialog.setVisible(true);
//        System.exit(0);
//    }

    private void onConvert() {
        try {
            onConvert0();
        } catch (Exception ex) {
            previewSQLText.setText("转换出现异常:" + ex.getMessage());
        }
    }

    private void onConvert0() {
        String selectSQL = selectSQLText.getText();
        if (StringUtils.isBlank(selectSQL)) {
            previewSQLText.setText("");
        } else {
            StringBuilder sqlBuilder = new StringBuilder();
            String[] selectSQLs = selectSQL.split("\n");
            List<String> sqlList = Arrays.stream(selectSQLs)
                    .filter(o -> StringUtils.contains(o, "Preparing:"))
                    .map(o -> o.replaceAll(".*(Preparing[\\s]*(?=:)): ", ""))
                    .collect(Collectors.toList());
            List<Queue<Map.Entry<String, String>>> parameterList = Arrays.stream(selectSQLs)
                    .filter(o -> StringUtils.contains(o, "Parameters:"))
                    .map(o -> parseParams(o))
                    .collect(Collectors.toList());
            int min = Math.min(sqlList.size(), parameterList.size());
            for (int i = 0; i < min; i++) {
                String sql = sqlList.get(i);
                Queue<Map.Entry<String, String>> params = parameterList.get(i);

                sql = parseSql(sql, params).toString();

                String formatSQL = FORMATTER.format(sql);
                sqlBuilder.append("\n-- 第").append(i + 1).append("条sql数据\n");
                sqlBuilder.append(formatSQL);
            }

            previewSQLText.setText(sqlBuilder.toString());
            convertButton.setText("转换↓ 一共转换成" + min + "条sql");
        }
    }

    private StringBuilder parseSql(String sql, Queue<Map.Entry<String, String>> params) {

        final StringBuilder sb = new StringBuilder(sql);

        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) != MARK) {
                continue;
            }

            final Map.Entry<String, String> entry = params.poll();
            if (Objects.isNull(entry)) {
                continue;
            }


            sb.deleteCharAt(i);

            if (NEED_BRACKETS.contains(entry.getValue())) {
                if("LocalDateTime".equals(entry.getValue())){
                    sb.insert(i, String.format("'%s'", entry.getKey().replace("T"," ")));
                }else{
                    sb.insert(i, String.format("'%s'", entry.getKey()));
                }
            } else {
                sb.insert(i, entry.getKey());
            }


        }

        return sb;
    }

    private Queue<Map.Entry<String, String>> parseParams(String line) {
        if(StringUtils.isBlank(line)){
            return new ArrayDeque<>(0);
        }
        line = StringUtils.removeEnd(line, "\n").replaceAll(".*(Parameters[\\s]*(?=:)): ", "");

        final String[] strings = StringUtils.splitByWholeSeparator(line, ",");
        final Queue<Map.Entry<String, String>> queue = new ArrayDeque<>(strings.length);

        for (String s : strings) {
            String trim = StringUtils.trim(s);
            String value = StringUtils.substringBeforeLast(trim, "(");
            String type = StringUtils.substringBetween(trim, "(", ")");
            if (StringUtils.isEmpty(type)) {
                queue.offer(new AbstractMap.SimpleEntry<>(value, null));
            } else {
                queue.offer(new AbstractMap.SimpleEntry<>(value, type));
            }
        }

        return queue;
    }
}
