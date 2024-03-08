package com.easy.query.plugin.core;

import cn.hutool.core.util.ReUtil;
import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.constant.EasyQueryConstant;
import com.easy.query.plugin.core.entity.AnonymousParseContext;
import com.easy.query.plugin.core.entity.AnonymousParseResult;
import com.easy.query.plugin.core.entity.ColumnInfo;
import com.easy.query.plugin.core.entity.ColumnMetadata;
import com.easy.query.plugin.core.entity.MatchTypeMapping;
import com.easy.query.plugin.core.entity.TableInfo;
import com.easy.query.plugin.core.entity.TableMetadata;
import com.easy.query.plugin.core.entity.ValueHolder;
import com.easy.query.plugin.core.entity.struct.RenderStructDTOContext;
import com.easy.query.plugin.core.util.CodeReformatUtil;
import com.easy.query.plugin.core.util.GenUtils;
import com.easy.query.plugin.core.util.MyModuleUtil;
import com.easy.query.plugin.core.util.NotificationUtils;
import com.easy.query.plugin.core.util.PsiJavaFileUtil;
import com.intellij.openapi.module.Module;
import com.easy.query.plugin.core.util.ObjectUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.easy.query.plugin.core.util.TableUtils;
import com.easy.query.plugin.core.util.VirtualFileUtils;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * create time 2023/11/30 21:39
 * 文件说明
 *
 * @author xuejiaming
 */
public class RenderEasyQueryTemplate {
    public static class ImportAndClass {
        public final String importPackage;
        public final String className;

        public ImportAndClass(String importPackage, String className) {

            this.importPackage = importPackage;
            this.className = className;
        }
    }

    private static Set<String> getIgnoreColumns(String ignoreColumns) {
        if (StringUtils.isBlank(ignoreColumns)) {
            return new HashSet<>();
        }
        return Arrays.stream(StringUtils.split(ignoreColumns, ",")).collect(Collectors.toSet());
    }

    private static TableInfo transTo(TableMetadata tableMetadata, EasyQueryConfig config) {
        Map<String, List<MatchTypeMapping>> typeMapping = config.getTypeMapping() == null ? TableUtils.getDefaultTypeMappingMap() : config.getTypeMapping();
        TableInfo tableInfo = new TableInfo();
        tableInfo.setName(tableMetadata.getName());
        tableInfo.setComment(tableMetadata.getComment());
        if (StringUtils.isNotBlank(config.getModelSuperClass())) {
            ImportAndClass importAndClass = getImportAndClass(config.getModelSuperClass());
            if (importAndClass.importPackage != null) {
                tableInfo.addImportClassItem(importAndClass.importPackage);
            }
            tableInfo.setSuperClass(importAndClass.className);
        }
        ArrayList<ColumnInfo> columnInfos = new ArrayList<>();
        List<ColumnMetadata> columns = tableMetadata.getColumns();
        Set<String> ignoreColumns = getIgnoreColumns(config.getIgnoreColumns());
        for (ColumnMetadata column : columns) {
            if (ignoreColumns.contains(column.getName())) {
                continue;
            }
            ColumnInfo columnInfo = new ColumnInfo();
            columnInfo.setName(column.getName());
            columnInfo.setFieldName(StrUtil.toCamelCase(column.getName().toLowerCase()));
            String fieldType = getFieldType(column.getJdbcType(), tableInfo, column.getJdbcTypeName(), column.getSize(), column.getJdbcTypeStr().toLowerCase(), typeMapping);
            columnInfo.setFieldType(fieldType);
            columnInfo.setNotNull(column.isNotNull());
            columnInfo.setComment(column.getComment());
            columnInfo.setMethodName(StrUtil.upperFirst(columnInfo.getFieldName()));
            columnInfo.setType(column.getJdbcTypeName());
            columnInfo.setPrimaryKey(column.isPrimary());
            columnInfo.setAutoIncrement(column.isAutoIncrement());
            columnInfo.setSize(column.getSize());
            columnInfos.add(columnInfo);
        }
        tableInfo.setColumnList(columnInfos);

        return tableInfo;
    }

    private static ImportAndClass getImportAndClass(String fullName) {
        if (fullName == null) {
            return new ImportAndClass(null, "UNKNOWN");
        }
        if (fullName.contains(".")) {
            String className = fullName.substring(fullName.lastIndexOf(".") + 1);
            if (!fullName.startsWith("java.lang.")) {
                return new ImportAndClass(fullName, className);
            }
            return new ImportAndClass(null, className);
        }
        return new ImportAndClass(null, fullName);
    }

    private static String getFieldType(int jdbc, TableInfo tableInfo, String jdbcTypeName, int size, String jdbcTypeStr, Map<String, List<MatchTypeMapping>> typeMapping) {
        if (typeMapping.containsKey("ORDINARY")) {
            for (MatchTypeMapping mapping : typeMapping.get("ORDINARY")) {
                if (jdbcTypeStr.equals(mapping.getColumType())) {
                    ImportAndClass importAndClass = getImportAndClass(mapping.getJavaField());
                    if (importAndClass.importPackage != null) {
                        tableInfo.addImportClassItem(importAndClass.importPackage);
                    }
                    return importAndClass.className;
                }
            }
        }
        if (typeMapping.containsKey("REGEX")) {
            for (MatchTypeMapping mapping : typeMapping.get("REGEX")) {
                String group0 = ReUtil.getGroup0(mapping.getColumType(), jdbcTypeStr);
                if (StrUtil.isNotEmpty(group0)) {
                    ImportAndClass importAndClass = getImportAndClass(mapping.getJavaField());
                    if (importAndClass.importPackage != null) {
                        tableInfo.addImportClassItem(importAndClass.importPackage);
                    }
                    return importAndClass.className;
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


    public static boolean renderStructDTOType(RenderStructDTOContext renderStructDTOContext) {
        Map<PsiDirectory, List<PsiElement>> templateMap = new HashMap<>();
        VelocityEngine velocityEngine = new VelocityEngine();
        VelocityContext context = new VelocityContext();
        context.put("appContext", renderStructDTOContext);
        Project project = renderStructDTOContext.getProject();
        Module module = renderStructDTOContext.getModule();
        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        renderTemplate(Template.getTemplateContent("StructDTOTemplate.java"), context, renderStructDTOContext.getDtoName(), velocityEngine, templateMap, renderStructDTOContext.getPackageName(), "", factory, project, module);
        ValueHolder<Boolean> booleanValueHolder = new ValueHolder<>();
        booleanValueHolder.setValue(true);
        flush(project, templateMap,false,booleanValueHolder);
        return booleanValueHolder.getValue();
    }


    public static void renderAnonymousType(AnonymousParseContext anonymousParseContext) {
        Collection<AnonymousParseResult> values = anonymousParseContext.getAnonymousParseResultMap().values();
        Set<String> importClassList = new HashSet<>();
        for (AnonymousParseResult value : values) {
            importClassList.add(value.getPropertyFullType());
        }
        Map<PsiDirectory, List<PsiElement>> templateMap = new HashMap<>();
        VelocityEngine velocityEngine = new VelocityEngine();
        VelocityContext context = new VelocityContext();
        String anonymousName = anonymousParseContext.getAnonymousName();
        context.put("modelPackage", anonymousParseContext.getModelPackage());
        context.put("className", anonymousName);
        context.put("config", anonymousParseContext);
        context.put("properties", values);
        context.put("importClassList", importClassList);
        Project project = anonymousParseContext.getProject();
        Module module = MyModuleUtil.getModule(project, anonymousParseContext.getModuleName());
        if (module == null) {
            NotificationUtils.notifyError("无法获取模块[" + anonymousParseContext.getModuleName() + "]!", "", project);
            return;
        }
        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        renderTemplate(Template.getTemplateContent("AnonymousTypeTemplate.java"), context, anonymousName, velocityEngine, templateMap, anonymousParseContext.getModelPackage(), "", factory, project, module);
        ValueHolder<Boolean> booleanValueHolder = new ValueHolder<>();
        booleanValueHolder.setValue(true);
        flush(project, templateMap,true,booleanValueHolder);
        for (Map.Entry<PsiDirectory, List<PsiElement>> entry : templateMap.entrySet()) {
            PsiDirectory psiDirectory = entry.getKey();
            List<PsiElement> value = entry.getValue();
            for (PsiElement psiElement : value) {
                PsiFile psiFile = (PsiFile) psiElement;
                PsiFile file = psiDirectory.findFile(psiFile.getName());
                VirtualFile virtualFile = file.getVirtualFile();
                PsiJavaFileUtil.createAptCurrentFile(virtualFile,project);
            }
        }
    }
    public static String getAnonymousLambdaTemplate(AnonymousParseContext anonymousParseContext){

        VelocityEngine velocityEngine = new VelocityEngine();
        VelocityContext context = new VelocityContext();
        String anonymousName = anonymousParseContext.getAnonymousName();
        context.put("className", anonymousName);
        context.put("properties", anonymousParseContext.getAnonymousParseResultMap().values());

        StringWriter sw = new StringWriter();
        String templateContent = Template.getTemplateContent("AnonymousTypeLambdaTemplate.java");
        velocityEngine.evaluate(context, sw, "easy-query", templateContent);
        return sw.toString();
    }

    private static void flush(Project project, Map<PsiDirectory, List<PsiElement>> templateMap,boolean deleteIfExists,ValueHolder<Boolean> valueHolder) {

        DumbService.getInstance(project).runWhenSmart(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                for (Map.Entry<PsiDirectory, List<PsiElement>> entry : templateMap.entrySet()) {
                    List<PsiElement> list = entry.getValue();
                    PsiDirectory directory = entry.getKey();
                    if(deleteIfExists){
                        // 删除原有文件
                        for (PsiElement psiFile : list) {
                            if (psiFile instanceof PsiFile) {
                                PsiFile file = (PsiFile) psiFile;
                                VirtualFile virtualFile = file.getVirtualFile();
                                if(virtualFile!=null){

                                    PsiFile psiFile1 = VirtualFileUtils.getPsiFile(project, virtualFile);
                                    if(psiFile1!=null){
                                        PsiClassOwner newFile = (PsiClassOwner)psiFile1 ;
                                        PsiClass[] classes = newFile.getClasses();
                                        if (classes.length == 0) {
                                            continue;
                                        }
                                        PsiClass psiClass = classes[0];
                                        PsiAnnotation easyAnonymous = psiClass.getAnnotation("com.easy.query.core.annotation.EasyAnonymous");
                                        if(easyAnonymous==null){
                                            continue;
                                        }
                                    }
                                }
                                PsiFile directoryFile = directory.findFile(file.getName());
                                if (ObjectUtil.isNotNull(directoryFile)) {
                                    directoryFile.delete();
                                }
                            }
                        }
                    }

                    for (PsiElement psiFile : list) {
                        try {
                            directory.add(psiFile);
                        } catch (IncorrectOperationException e) {
                            if (e.getMessage().contains("already exists")) {
                                PsiFile file = (PsiFile) psiFile;
                                Messages.showErrorDialog("文件已存在：" + file.getName(), "错误");
                                valueHolder.setValue(false);
                            } else {
                                Messages.showErrorDialog(" 操作错误：" + e.getMessage(), "错误");
                                valueHolder.setValue(false);
                            }
                        } catch (Exception e) {
                            Messages.showErrorDialog("索引未更新:" + e.getMessage(), "错误");
                            valueHolder.setValue(false);
                        }
                    }
                }
            });
        });
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
            String className = GenUtils.tableToJava(tableInfo.getName(), new String[]{config.getTablePrefix()});
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
        ValueHolder<Boolean> booleanValueHolder = new ValueHolder<>();
        booleanValueHolder.setValue(true);
        flush(project, templateMap,false,booleanValueHolder);
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
            Map<PsiDirectory, List<PsiElement>> templateMap,
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
