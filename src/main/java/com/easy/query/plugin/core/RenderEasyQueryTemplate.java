package com.easy.query.plugin.core;

import cn.hutool.core.util.ReUtil;
import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.constant.EasyQueryConstant;
import com.easy.query.plugin.core.entity.ColumnInfo;
import com.easy.query.plugin.core.entity.ColumnMetadata;
import com.easy.query.plugin.core.entity.MatchTypeMapping;
import com.easy.query.plugin.core.entity.TableInfo;
import com.easy.query.plugin.core.entity.TableMetadata;
import com.easy.query.plugin.core.util.CodeReformatUtil;
import com.intellij.openapi.module.Module;
import com.easy.query.plugin.core.util.ObjectUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.easy.query.plugin.core.util.TableUtils;
import com.easy.query.plugin.core.util.VirtualFileUtils;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.highlighter.XmlFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * create time 2023/11/30 21:39
 * 文件说明
 *
 * @author xuejiaming
 */
public class RenderEasyQueryTemplate {

    private static TableInfo transTo(TableMetadata tableMetadata, EasyQueryConfig config) {
        Map<String, List<MatchTypeMapping>> typeMapping = config.getTypeMapping() == null ? TableUtils.getDefaultTypeMappingMap() : config.getTypeMapping();
        TableInfo tableInfo = new TableInfo();
        tableInfo.setName(tableMetadata.getName());
        tableInfo.setComment(tableMetadata.getComment());
        tableInfo.setSuperClass(config.getModelSuperClass());
        ArrayList<ColumnInfo> columnInfos = new ArrayList<>();
        List<ColumnMetadata> columns = tableMetadata.getColumns();
        for (ColumnMetadata column : columns) {
            ColumnInfo columnInfo = new ColumnInfo();
            columnInfo.setName(column.getName());
            columnInfo.setFieldName(StrUtil.toCamelCase(column.getName()));
            String fieldType = getFieldType(column.getJdbcType(), tableInfo, column.getJdbcTypeName(), column.getSize(), column.getJdbcTypeStr().toLowerCase(), typeMapping);
            columnInfo.setFieldType(fieldType);
            columnInfo.setNotNull(column.isNotNull());
            columnInfo.setComment(column.getComment());
            columnInfo.setMethodName(StrUtil.upperFirst(columnInfo.getFieldName()));
            columnInfo.setType(column.getJdbcTypeName());
            columnInfo.setPrimaryKey(column.isPrimary());
            columnInfo.setAutoIncrement(column.isAutoIncrement());
            columnInfos.add(columnInfo);
        }
        tableInfo.setColumnList(columnInfos);

        return tableInfo;
    }

    private String getImport(String fieldType) {
        switch (fieldType) {
            case "String":
                return "java.lang.String";
            case "Integer":
                return "java.lang.Integer";
            case "Long":
                return "java.lang.Long";
            case "Double":
                return "java.lang.Double";
            case "Float":
                return "java.lang.Float";
            case "java.math.BigDecimal":
                return "java.math.BigDecimal";
            case "Date":
                return "java.util.Date";
        }
        return null;
    }

    private static String getFieldType(int jdbc, TableInfo tableInfo, String jdbcTypeName, int size, String jdbcTypeStr, Map<String, List<MatchTypeMapping>> typeMapping) {
        if (typeMapping.containsKey("ORDINARY")) {
            for (MatchTypeMapping mapping : typeMapping.get("ORDINARY")) {
                if (jdbcTypeStr.equals(mapping.getColumType())) {
                    String javaField = mapping.getJavaField();
                    if (!javaField.startsWith("java.lang.")) {
                        tableInfo.addImportClassItem(javaField);
                        return javaField.substring(javaField.lastIndexOf(".") + 1);
                    }
                    return javaField;
                }
            }
        }
        if (typeMapping.containsKey("REGEX")) {
            for (MatchTypeMapping mapping : typeMapping.get("REGEX")) {
                String group0 = ReUtil.getGroup0(mapping.getColumType(), jdbcTypeStr);
                if (StrUtil.isNotEmpty(group0)) {
                    String javaField = mapping.getJavaField();
                    if (!javaField.startsWith("java.lang.")) {
                        tableInfo.addImportClassItem(javaField);
                        return javaField.substring(javaField.lastIndexOf(".") + 1);
                    }
                    return javaField;
                }
            }
        }

        String className = convert(jdbc, size).getName();
//        if (Object.class.getName().equals(className)) {
//            String fieldType = MybatisFlexPluginConfigData.getFieldType(jdbcTypeName);
//            if (StrUtil.isNotBlank(fieldType)) {
//                className = fieldType;
//            }
//        }

        boolean flag = className.contains(";");
        if (flag) {
            className = className.replace(";", "").replace("[L", "");
        }
        tableInfo.addImportClassItem(className);
        String fieldType = className.substring(className.lastIndexOf(".") + 1);
        if (flag) {
            fieldType += "[]";
        }
        return fieldType;
    }

    public static Class<?> convert(int sqlType, int size) {
        switch (sqlType) {
            case Types.BIT:
                return Boolean.class;
            case Types.SMALLINT:
                return Short.class;
            case Types.INTEGER:
                return Integer.class;
            case Types.BIGINT:
                return Long.class;
            case Types.FLOAT:
            case Types.REAL:
                return Float.class;
            case Types.DOUBLE:
                return Double.class;
            case Types.NUMERIC:
            case Types.DECIMAL:
                return java.math.BigDecimal.class;
            case Types.CHAR:
            case Types.NCHAR:
            case Types.VARCHAR:
            case Types.NVARCHAR:
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.CLOB:
                return String.class;
            case Types.TINYINT:
                if (size == 1) {
                    return Boolean.class;
                } else if (size == 2) {
                    return Short.class;
                } else {
                    return Integer.class;
                }
            case Types.TIME:
                return LocalTime.class;
            case Types.TIMESTAMP:
//                return java.sql.Timestamp.class;
            case Types.DATE:
                return LocalDateTime.class;
            case Types.BINARY:
            case Types.VARBINARY:
            case Types.LONGVARBINARY:
            case Types.BLOB:
                return Byte[].class;
            // 返回对象，在点击生成代码是时候让用户自行选择
            // case Types.NCLOB:
            //     return java.sql.NClob.class;
            // case Types.ARRAY:
            //     return java.sql.Array.class;
            // case Types.STRUCT:
            //     return java.sql.Struct.class;
            // case Types.REF:
            //     return java.sql.Ref.class;
            // case Types.SQLXML:
            //     return java.sql.SQLXML.class;
            default:
                return Object.class;
        }
    }

    public static void assembleData(List<TableMetadata> selectedTableInfo, EasyQueryConfig config, @NotNull Project project, Module module) {

        VelocityEngine velocityEngine = new VelocityEngine();
        // 修复因velocity.log拒绝访问，导致Velocity初始化失败
//        高版本已经把这个方法废弃了，所以这里注释掉；优先支持高版本
//        try {
//            velocityEngine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, new NullLogChute());
//        } catch (Exception e) {
//        }

        VelocityContext context = new VelocityContext();
        HashMap<PsiDirectory, List<PsiElement>> templateMap = new HashMap<>();
//        Map<String, String> templates = new ConcurrentHashMap<>(config.getTemplates());
        String suffix = config.getSuffix();
//        Map<String, String> packages = new ConcurrentHashMap<>(config.getPackages());
//        removeEmptyPackage(packages, templates);
//        String modelModule = config.getModelModule();
//        Map<String, String> modules = config.getModules();
        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        for (TableMetadata tableMetadata : selectedTableInfo) {
            TableInfo tableInfo = transTo(tableMetadata, config);
            String className = TableUtils.getClassName(tableInfo.getName(), config.getTablePrefix());
            context.put("className", className);
            context.put("author", ObjectUtil.defaultIfEmpty(config.getAuthor(), "easy-query-plugin automatic generation"));
            context.put("since", ObjectUtil.defaultIfEmpty(config.getSince(), "1.0"));
            context.put("modelName", className + ObjectUtil.defaultIfNull(config.getModelSuffix(), "Entity"));
            context.put("config", config);
            context.put("importClassList", tableInfo.getImportClassList());
            context.put("table", tableInfo);
            renderTemplate(config.getModelTemplate(), context, className, velocityEngine, templateMap, config.getModelPackage(), suffix, factory, project, module);
//            // 自定义模版渲染
//            List<TabInfo> infoList = config.getTabList();
//            if (CollectionUtils.isNotEmpty(infoList)) {
//                for (TabInfo info : infoList) {
//                    String genPath = info.getGenPath();
//                    StringWriter sw = new StringWriter();
//                    velocityEngine.evaluate(context, sw, "mybatis-flex", info.getContent());
//                    File file = new File(genPath + File.separator + className + "." + info.getSuffix());
//                    if (!file.getParentFile().exists()) {
//                        Messages.showWarningDialog("自定义模板路径不存在：" + genPath, "警告");
//                        return;
//                    }
//                    try {
//                        FileOutputStream fileOutputStream = new FileOutputStream(file);
//                        IoUtil.write(fileOutputStream, true, sw.toString().getBytes(StandardCharsets.UTF_8));
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            }
        }

        DumbService.getInstance(project).runWhenSmart(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                for (Map.Entry<PsiDirectory, List<PsiElement>> entry : templateMap.entrySet()) {
                    List<PsiElement> list = entry.getValue();
                    PsiDirectory directory = entry.getKey();
//                    // 如果勾选了覆盖，则删除原有文件
//                    if (config.isOverrideCheckBox()) {
//                        for (PsiElement psiFile : list) {
//                            if (psiFile instanceof PsiFile) {
//                                PsiFile file = (PsiFile) psiFile;
//                                PsiFile directoryFile = directory.findFile(file.getName());
//                                if (ObjectUtil.isNotNull(directoryFile)) {
//                                    directoryFile.delete();
//                                }
//                            }
//                        }
//                    }

                    for (PsiElement psiFile : list) {
                        try {
                            directory.add(psiFile);
                        } catch (IncorrectOperationException e) {
                            if (e.getMessage().contains("already exists")) {
                                PsiFile file = (PsiFile) psiFile;
                                Messages.showErrorDialog("文件已存在：" + file.getName(), "错误");
                            } else {
                                Messages.showErrorDialog(" 操作错误：" + e.getMessage(), "错误");
                            }
                        } catch (Exception e) {
                            Messages.showErrorDialog("索引未更新", "错误");
                        }
                    }
                }
            });
        });
        //
        // // 生成代码之后，重新构建
        // CompilerManagerUtil.make(Modules.getModule(config.getModelModule()));
    }

    private static void removeEmptyPackage(Map<String, String> packages, Map<String, String> templates) {
        for (Map.Entry<String, String> entry : packages.entrySet()) {
            if (StrUtil.isEmpty(entry.getValue())) {
                packages.remove(entry.getKey());
                templates.remove(entry.getKey());
            }
        }
    }

    /**
     * 渲染模板
     */
    private static void renderTemplate(
            String template,
            VelocityContext context,
            String className,
            VelocityEngine velocityEngine,
            HashMap<PsiDirectory, List<PsiElement>> templateMap,
            String _package,
            String suffix,
            PsiFileFactory factory,
            Project project,
            Module module
    ) {

        StringWriter sw = new StringWriter();
        context.put("className", className);
        velocityEngine.evaluate(context, sw, "easy-query", template);
//            Module module = Modules.getModule(modules.get(entry.getKey()));
//            String key = entry.getKey();
//            if (StrUtil.isEmpty(key)) {
//                key = "resource".equals(Template.getConfigData(MybatisFlexConstant.MAPPER_XML_TYPE, "resource")) ? "" : "xml";
//            }
        PsiDirectory packageDirectory = VirtualFileUtils.getPsiDirectory(project, module, _package, EasyQueryConstant.ENTITY);
        DumbService.getInstance(project).runWhenSmart(() -> {
            String fileName = className + suffix + ".java";
            PsiFile file = factory.createFileFromText(fileName, JavaFileType.INSTANCE, sw.toString());
            templateMap.computeIfAbsent(packageDirectory, k -> new ArrayList<>()).add(CodeReformatUtil.reformat(file));
        });
    }

}
