package com.easy.query.plugin.windows;

import com.easy.query.plugin.core.util.BasicFormatter;
import com.easy.query.plugin.core.util.DialogUtil;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class SQLPreviewDialog extends JDialog {
    private JPanel contentPane;
    private JButton buttonCancel;
    private JTextArea selectSQLText;
    private JTextArea previewSQLText;
    private JButton convertButton;
    private static final BasicFormatter FORMATTER = new BasicFormatter();

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

    private void onConvert(){
        String selectSQL = selectSQLText.getText();
        if(StringUtils.isBlank(selectSQL)){
            previewSQLText.setText("");
        }else{
            StringBuilder sqlBuilder = new StringBuilder();
            String[] selectSQLs = selectSQL.split("\n");
            List<String> sqlList = Arrays.stream(selectSQLs)
                    .filter(o -> StringUtils.contains(o, "Preparing:"))
                    .map(o -> o.replaceAll(".*(Preparing[\\s]*(?=:)):", ""))
                    .collect(Collectors.toList());
            List<List<String>> parameterList = Arrays.stream(selectSQLs)
                    .filter(o -> StringUtils.contains(o, "Parameters:"))
                    .map(o -> parseParas(o))
                    .collect(Collectors.toList());
            int min = Math.min(sqlList.size(), parameterList.size());
            for (int i = 0; i < min; i++) {
                String sql = sqlList.get(i);
                List<String> params = parameterList.get(i);
                for (int i1 = 0; i1 < params.size(); i1++) {
                    sql=sql.replace("?",params.get(i1));
                }

                String formatSQL = FORMATTER.format(sql);
                if(i!=0){
                    sqlBuilder.append("\n-- 第").append(i).append("条sql数据\n");
                }
                sqlBuilder.append(formatSQL);
            }
            if(min>1){
                sqlBuilder.insert(0,"-- 第1条sql数据\n");
            }

            previewSQLText.setText(sqlBuilder.toString());
            convertButton.setText("转换↓ 一共转换成"+min+"条sql");
        }
    }
    private static String parsePara(String p) {
        if ("null".equals(p)) {
            return p;
        }

        String result;
        String m = p.replaceAll(".*\\(([^\\)]+)\\)$", "$1");

        result = p.replaceAll("\\(" + m + "\\)", "");

        switch (m) {
            case "String":
            case "Timestamp":
            case "BigDecimal":
            case "Date":
            case "Time":
            case "LocalDate":
            case "LocalTime":
                result = "'" + result + "'";
                break;
            case "LocalDateTime":
                result = "'" + result.replace("T", " ") + "'";
                break;
//            case "Integer":
//                // Handle Integer case if needed
//                break;
//            case "Date":
//                result = "STR_TO_DATE('" + result + "','%Y-%m-%d %H:%i:%s')";
//                break;
            default:
                // Handle other cases if needed
                break;
        }

        return result;
    }
    private  List<String> parseParas(String para) {
        List<String> ps = new ArrayList<>();
        String p;
        String result = para.replaceAll(".*(Parameters[\\s]*(?=:)):", "");
        String[] t = result.split("(?=[\\s]*,)|(?<=[\\s]*\\$)");

        int len = t.length;
        if (len > 0) {
            for (String s : t) {
                p = s.replaceAll(",$", "").replaceAll("^[,\\s]+|[\\s]+$", "");
                ps.add(parsePara(p));
            }
        }
        return ps;
    }
}
