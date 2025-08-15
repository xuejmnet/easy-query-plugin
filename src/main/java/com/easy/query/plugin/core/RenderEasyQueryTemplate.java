package com.easy.query.plugin.core;

import cn.hutool.core.util.ReUtil;
import com.easy.query.plugin.core.config.EasyQueryConfig;
import com.easy.query.plugin.core.constant.EasyQueryConstant;
import com.easy.query.plugin.core.entity.*;
import com.easy.query.plugin.core.entity.struct.RenderStructDTOContext;
import com.easy.query.plugin.core.util.*;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jetbrains.annotations.NotNull;

import java.io.StringWriter;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
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


    public static boolean renderStructDTOType(RenderStructDTOContext renderContext) {
        Map<PsiDirectory, List<PsiElement>> templateMap = new HashMap<>();
        VelocityEngine velocityEngine = new VelocityEngine();
        VelocityContext velocityContext = new VelocityContext();
        Project project = renderContext.getProject();
        Module module = renderContext.getModule();
        PsiFileFactory factory = PsiFileFactory.getInstance(project);

        String className = renderContext.getDtoName();
        String suffix = "";
        String _package = renderContext.getPackageName();

        StringWriter dtoClassContent = new StringWriter();

        velocityContext.put("appContext", renderContext);
        velocityContext.put("className", className);

        String template = Template.getTemplateContent("StructDTOTemplate.java");
        velocityEngine.evaluate(velocityContext, dtoClassContent, "easy-query", template);

        PsiDirectory packageDirectory = VirtualFileUtils.getPsiDirectory(project, module, _package, EasyQueryConstant.ENTITY);
        DumbService.getInstance(project).runWhenSmart(() -> {
            String fileName = className + suffix + ".java";
            PsiFile psiFile = factory.createFileFromText(fileName, JavaFileType.INSTANCE, dtoClassContent.toString());
            if (Objects.nonNull(renderContext.getRootDtoPsiClass())) {
                // DTO PsiClass 不为空, 是修改
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    renderContext.getRootDtoPsiClass().replace(((PsiJavaFile) psiFile).getClasses()[0]);
                });
            } else {
                // 新增的仍然走原先的逻辑
                templateMap.computeIfAbsent(packageDirectory, k -> new ArrayList<>()).add(CodeReformatUtil.reformat(psiFile));
            }
        });

        ValueHolder<Boolean> booleanValueHolder = new ValueHolder<>();
        booleanValueHolder.setValue(true);
        flush(project, templateMap, renderContext.getDeleteExistsFile(), booleanValueHolder);
        return true;
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
        flush(project, templateMap, true, booleanValueHolder);
        for (Map.Entry<PsiDirectory, List<PsiElement>> entry : templateMap.entrySet()) {
            PsiDirectory psiDirectory = entry.getKey();
            List<PsiElement> value = entry.getValue();
            for (PsiElement psiElement : value) {
                PsiFile psiFile = (PsiFile) psiElement;
                PsiFile file = psiDirectory.findFile(psiFile.getName());
                VirtualFile virtualFile = file.getVirtualFile();
                PsiJavaFileUtil.createAptCurrentFile(virtualFile, project);
            }
        }
    }

    public static String getAnonymousLambdaTemplate(AnonymousParseContext anonymousParseContext) {

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

    private static void override(Project project, Map<PsiDirectory, List<PsiElement>> templateMap, ValueHolder<Boolean> valueHolder) {

        DumbService.getInstance(project).runWhenSmart(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                for (Map.Entry<PsiDirectory, List<PsiElement>> entry : templateMap.entrySet()) {
                    List<PsiElement> list = entry.getValue();
                    for (PsiElement psiFileElement : list) {
                        if (psiFileElement instanceof PsiFile) {
                            PsiFile psiFile = (PsiFile) psiFileElement;
                            PsiClassOwner newFile = (PsiClassOwner) psiFile;
                            PsiClass[] classes = newFile.getClasses();
                            if (classes.length == 0) {
                                Messages.showErrorDialog("未找到对应类，文件：" + psiFile.getName(), "错误");
                                continue;
                            }
                            PsiClass psiClass = classes[0];
                            //差异化覆盖
                        }
                    }
                }
            });
        });
    }

    private static void flush(Project project, Map<PsiDirectory, List<PsiElement>> templateMap, boolean deleteIfExists, ValueHolder<Boolean> valueHolder) {

        DumbService.getInstance(project).runWhenSmart(() -> {
            WriteCommandAction.runWriteCommandAction(project, () -> {
                for (Map.Entry<PsiDirectory, List<PsiElement>> entry : templateMap.entrySet()) {
                    List<PsiElement> list = entry.getValue();
                    PsiDirectory directory = entry.getKey();
                    if (deleteIfExists) {
                        // 删除原有文件
                        for (PsiElement psiFile : list) {
                            if (psiFile instanceof PsiFile) {
                                PsiFile file = (PsiFile) psiFile;
                                VirtualFile virtualFile = file.getVirtualFile();
                                if (virtualFile != null) {

                                    PsiFile psiFile1 = VirtualFileUtils.getPsiFile(project, virtualFile);
                                    if (psiFile1 != null) {
                                        PsiClassOwner newFile = (PsiClassOwner) psiFile1;
                                        PsiClass[] classes = newFile.getClasses();
                                        if (classes.length == 0) {
                                            continue;
                                        }
                                        PsiClass psiClass = classes[0];
                                        PsiAnnotation easyAnonymous = psiClass.getAnnotation("com.easy.query.core.annotation.EasyAnonymous");
                                        if (easyAnonymous == null) {
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
