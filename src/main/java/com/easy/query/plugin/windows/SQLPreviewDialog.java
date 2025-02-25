package com.easy.query.plugin.windows;

import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.util.BasicFormatter;
import com.easy.query.plugin.core.util.DialogUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.util.regex.Matcher;
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
//            int min = Math.min(sqlList.size(), parameterList.size());
            int j=0;
            for (int i = 0; i < sqlList.size(); i++) {
                String sql = sqlList.get(i);
                Queue<Map.Entry<String, String>> params = i >= parameterList.size() ? new ArrayDeque<>(0) : parameterList.get(i);

                sql = parseSql(sql, params).toString();

                String formatSQL = FORMATTER.format(sql);
                j++;
                sqlBuilder.append("\n-- 第").append(j).append("条sql数据\n");
                sqlBuilder.append(formatSQL);
            }

            previewSQLText.setText(sqlBuilder.toString());
            convertButton.setText("转换↓ 一共转换成" + j + "条sql");
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
                if ("LocalDateTime".equals(entry.getValue())) {
                    sb.insert(i, String.format("'%s'", entry.getKey().replace("T", " ")));
                }
                // 常见的不需要加引号的类型
                else if (StrUtil.equalsAnyIgnoreCase(entry.getValue(), "BigDecimal", "Integer", "Long", "Double", "Float", "Short", "Boolean")) {
                    sb.insert(i, entry.getKey());
                } else {
                    sb.insert(i, String.format("'%s'", entry.getKey()));
                }
            } else {
                sb.insert(i, entry.getKey());
            }


        }

        return sb;
    }

    private Queue<Map.Entry<String, String>> parseParams(String line) {
        if (StringUtils.isBlank(line)) {
            return new ArrayDeque<>(0);
        }
        line = StringUtils.removeEnd(line, "\n").replaceAll(".*(Parameters[\\s]*(?=:)): ", "");


        final Queue<Map.Entry<String, String>> queue = new ArrayDeque<>();
        // 正则表达式模式，用于匹配值和类型
        String pattern = "(.*?[^,]*)\\(([^)]+?)\\),";

        // 创建Pattern对象
        Pattern r = Pattern.compile(pattern);

        // 创建Matcher对象
        Matcher m = r.matcher(line + ",");


        // 查找所有匹配项
        while (m.find()) {
            String value = m.group(1);
            String type = m.group(2);
            if (StringUtils.isEmpty(type)) {
                queue.offer(new AbstractMap.SimpleEntry<>(StringUtils.trim(value), null));
            } else {
                queue.offer(new AbstractMap.SimpleEntry<>(StringUtils.trim(value), type));
            }
        }


        return queue;
    }
//    private Queue<Map.Entry<String, String>> parseParams(String line) {
//        if(StringUtils.isBlank(line)){
//            return new ArrayDeque<>(0);
//        }
//        line = StringUtils.removeEnd(line, "\n").replaceAll(".*(Parameters[\\s]*(?=:)): ", "");
//
//        final String[] strings = StringUtils.splitByWholeSeparator(line, ",");
//        final Queue<Map.Entry<String, String>> queue = new ArrayDeque<>(strings.length);
//
//        for (String s : strings) {
//            String trim = StringUtils.trim(s);
//            String value = StringUtils.substringBeforeLast(trim, "(");
//            String type = StringUtils.substringBetween(trim, "(", ")");
//            if (StringUtils.isEmpty(type)) {
//                queue.offer(new AbstractMap.SimpleEntry<>(value, null));
//            } else {
//                queue.offer(new AbstractMap.SimpleEntry<>(value, type));
//            }
//        }
//
//        return queue;
//    }
}
