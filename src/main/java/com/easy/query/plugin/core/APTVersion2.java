package com.easy.query.plugin.core;

import com.easy.query.plugin.core.config.CustomConfig;
import com.easy.query.plugin.core.entity.AptFileCompiler;
import com.easy.query.plugin.core.entity.AptPropertyInfo;
import com.easy.query.plugin.core.entity.AptSelectPropertyInfo;
import com.easy.query.plugin.core.entity.AptSelectorInfo;
import com.easy.query.plugin.core.entity.AptValueObjectInfo;
import com.easy.query.plugin.core.entity.GenerateFileEntry;
import com.easy.query.plugin.core.entity.PropertyColumn;
import com.easy.query.plugin.core.entity.PropertyColumn2Impl;
import com.easy.query.plugin.core.entity.PropertyColumnImpl;
import com.easy.query.plugin.core.enums.BeanPropTypeEnum;
import com.easy.query.plugin.core.enums.FileTypeEnum;
import com.easy.query.plugin.core.util.ClassUtil;
import com.easy.query.plugin.core.util.MyModuleUtil;
import com.easy.query.plugin.core.util.ObjectUtil;
import com.easy.query.plugin.core.util.PsiUtil;
import com.easy.query.plugin.core.util.StrUtil;
import com.easy.query.plugin.core.util.VelocityUtils;
import com.easy.query.plugin.core.util.VirtualFileUtils;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiType;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.apache.velocity.VelocityContext;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * create time 2024/4/27 09:56
 * 文件说明
 *
 * @author xuejiaming
 */
public class APTVersion2 {
    private static final Map<String, PropertyColumn> TYPE_COLUMN_MAPPING = new HashMap<>();
    private static final Logger log = Logger.getInstance(APTVersion2.class);

    static {

        TYPE_COLUMN_MAPPING.put("java.lang.Float", new PropertyColumn2Impl("SQLFloatTypeColumn", "java.lang.Float"));
        TYPE_COLUMN_MAPPING.put("java.lang.Double", new PropertyColumn2Impl("SQLDoubleTypeColumn", "java.lang.Double"));
        TYPE_COLUMN_MAPPING.put("java.lang.Short", new PropertyColumn2Impl("SQLShortTypeColumn", "java.lang.Short"));
        TYPE_COLUMN_MAPPING.put("java.lang.Integer", new PropertyColumn2Impl("SQLIntegerTypeColumn", "java.lang.Integer"));
        TYPE_COLUMN_MAPPING.put("java.lang.Long", new PropertyColumn2Impl("SQLLongTypeColumn", "java.lang.Long"));
        TYPE_COLUMN_MAPPING.put("java.lang.Byte", new PropertyColumn2Impl("SQLByteTypeColumn", "java.lang.Byte"));
        TYPE_COLUMN_MAPPING.put("java.math.BigDecimal", new PropertyColumn2Impl("SQLBigDecimalTypeColumn", "java.math.BigDecimal"));
        TYPE_COLUMN_MAPPING.put("java.lang.Boolean", new PropertyColumn2Impl("SQLBooleanTypeColumn", "java.lang.Boolean"));
        TYPE_COLUMN_MAPPING.put("java.lang.String", new PropertyColumn2Impl("SQLStringTypeColumn", "java.lang.String"));
        TYPE_COLUMN_MAPPING.put("java.util.UUID", new PropertyColumn2Impl("SQLUUIDTypeColumn", "java.util.UUID"));
        TYPE_COLUMN_MAPPING.put("java.sql.Timestamp", new PropertyColumn2Impl("SQLTimestampTypeColumn", "java.sql.Timestamp"));
        TYPE_COLUMN_MAPPING.put("java.sql.Time", new PropertyColumn2Impl("SQLTimeTypeColumn", "java.sql.Time"));
        TYPE_COLUMN_MAPPING.put("java.sql.Date", new PropertyColumn2Impl("SQLDateTypeColumn", "java.sql.Date"));
        TYPE_COLUMN_MAPPING.put("java.util.Date", new PropertyColumn2Impl("SQLUtilDateTypeColumn", "java.util.Date"));
        TYPE_COLUMN_MAPPING.put("java.time.LocalDate", new PropertyColumn2Impl("SQLLocalDateTypeColumn", "java.time.LocalDate"));
        TYPE_COLUMN_MAPPING.put("java.time.LocalDateTime", new PropertyColumn2Impl("SQLLocalDateTimeTypeColumn", "java.time.LocalDateTime"));
        TYPE_COLUMN_MAPPING.put("java.time.LocalTime", new PropertyColumn2Impl("SQLLocalTimeTypeColumn", "java.time.LocalTime"));


        TYPE_COLUMN_MAPPING.put("float", new PropertyColumn2Impl("SQLFloatTypeColumn", "java.lang.Float"));
        TYPE_COLUMN_MAPPING.put("double", new PropertyColumn2Impl("SQLDoubleTypeColumn", "java.lang.Double"));
        TYPE_COLUMN_MAPPING.put("short", new PropertyColumn2Impl("SQLShortTypeColumn", "java.lang.Short"));
        TYPE_COLUMN_MAPPING.put("int", new PropertyColumn2Impl("SQLIntegerTypeColumn", "java.lang.Integer"));
        TYPE_COLUMN_MAPPING.put("long", new PropertyColumn2Impl("SQLLongTypeColumn", "java.lang.Long"));
        TYPE_COLUMN_MAPPING.put("byte", new PropertyColumn2Impl("SQLByteTypeColumn", "java.lang.Byte"));
        TYPE_COLUMN_MAPPING.put("boolean", new PropertyColumn2Impl("SQLBooleanTypeColumn", "java.lang.Boolean"));
    }

    public static void generateApt(Project project, Map<PsiDirectory, List<GenerateFileEntry>> psiDirectoryMap,
                                   PsiAnnotation entityFileProxy, PsiAnnotation entityProxy,
                                   PsiClassOwner psiFile, String moduleDirPath, CustomConfig config,
                                   Module moduleForFile, PsiClass psiClass,
                                   VirtualFile oldFile, boolean allCompileFrom) {

        //com.easy.query.core.enums.FileGenerateEnum.GENERATE_CURRENT_COMPILE_OVERRIDE
        String strategy = entityFileProxy == null ? null : PsiUtil.getPsiAnnotationValueIfEmpty(entityFileProxy, "strategy", "GENERATE_CURRENT_COMPILE_OVERRIDE");
//                if(entityFileProxy!=null){
//                    // todo写文件而不是写到类里面
//                    continue;
//                }


        FileTypeEnum fileType = PsiUtil.getFileType(psiFile);
        String path = moduleDirPath + CustomConfig.getConfig(config,config.getGenPath(),
                fileType, MyModuleUtil.isMavenProject(moduleForFile), entityFileProxy != null)
                + psiFile.getPackageName().replace(".", "/") + "/proxy";

        PsiDirectory psiDirectory = VirtualFileUtils.createSubDirectory(moduleForFile, path);
        // 等待索引准备好
        DumbService.getInstance(project).runWhenSmart(() -> {
            // 在智能模式下，执行需要等待索引准备好的操作，比如创建文件
            // 创建文件等操作代码
            oldFile.putUserData(EasyQueryDocumentChangeHandler.CHANGE, false);

            String entityName = psiClass.getName();
            String entityFullName = psiClass.getQualifiedName();
            //获取对应的代理对象名称
            String proxyEntityName = PsiUtil.getPsiAnnotationValueIfEmpty(entityProxy, "value", psiClass.getName() + "Proxy");
            //代理对象属性忽略
            Set<String> proxyIgnoreProperties = PsiUtil.getPsiAnnotationValues(entityProxy, "ignoreProperties", new HashSet<>());
            //是否是数据库对象
            PsiAnnotation entityTable = psiClass.getAnnotation("com.easy.query.core.annotation.Table");
            //获取对应的忽略属性
            Set<String> tableAndProxyIgnoreProperties = PsiUtil.getPsiAnnotationValues(entityTable, "ignoreProperties", proxyIgnoreProperties);

            PsiField[] fields = psiClass.getAllFields();

            AptValueObjectInfo aptValueObjectInfo = new AptValueObjectInfo(entityName);
            String packageName = psiFile.getPackageName() + "." + ObjectUtil.defaultIfEmpty(config.getAllInTablesPackage(), "proxy");
            AptFileCompiler aptFileCompiler = new AptFileCompiler(packageName, entityName, proxyEntityName, new AptSelectorInfo(proxyEntityName + "Fetcher"), psiFile instanceof KtFile);
            aptFileCompiler.addImports(entityFullName);
            for (PsiField field : fields) {
                boolean isStatic = PsiUtil.fieldIsStatic(field);
                if(isStatic){
                    continue;
                }
                PsiAnnotation columnIgnore = field.getAnnotation("com.easy.query.core.annotation.ColumnIgnore");
                if (columnIgnore != null) {
                    continue;
                }
                String name = field.getName();
                //是否存在忽略属性
                if (!tableAndProxyIgnoreProperties.isEmpty() && tableAndProxyIgnoreProperties.contains(name)) {
                    continue;
                }
                BeanPropTypeEnum beanPropType = ClassUtil.hasGetterAndSetter(psiClass, name);
                if (beanPropType == BeanPropTypeEnum.NOT) {
                    continue;
                }
                PsiAnnotation navigate = field.getAnnotation("com.easy.query.core.annotation.Navigate");
                String psiFieldPropertyType = PsiUtil.getPsiFieldPropertyType(field, navigate != null);
                String psiFieldComment = PsiUtil.getPsiFieldClearComment(field);
                PsiAnnotation valueObject = field.getAnnotation("com.easy.query.core.annotation.ValueObject");
                boolean isValueObject = valueObject != null;
                String fieldName = isValueObject ? psiFieldPropertyType.substring(psiFieldPropertyType.lastIndexOf(".") + 1) : entityName;

                PsiAnnotation proxyProperty = field.getAnnotation("com.easy.query.core.annotation.ProxyProperty");
                String proxyPropertyName = PsiUtil.getPsiAnnotationValue(proxyProperty, "value", null);
                String generateAnyType = PsiUtil.getPsiAnnotationValue(proxyProperty, "generateAnyType", null);
                Boolean anyType = StrUtil.isBlank(generateAnyType) ? null : Objects.equals("true", generateAnyType);
                PropertyColumn propertyColumn = getPropertyColumn(psiFieldPropertyType,anyType);

                boolean includeProperty = navigate != null;
                boolean includeManyProperty = false;
                if (!includeProperty) {
                    aptFileCompiler.getSelectorInfo().addProperties(new AptSelectPropertyInfo(name, psiFieldComment, proxyPropertyName, beanPropType));
                } else {
                    aptFileCompiler.addImports("com.easy.query.core.proxy.columns.SQLNavigateColumn");
                    String propertyType = propertyColumn.getPropertyType();

                    String propIsProxy = PsiUtil.getPsiAnnotationValue(navigate, "propIsProxy", "true");
                    String navigatePropertyProxyFullName = getNavigatePropertyProxyFullName(project, propertyType, !Objects.equals("false", propIsProxy));
                    if (navigatePropertyProxyFullName != null) {
                        propertyColumn.setNavigateProxyName(navigatePropertyProxyFullName);
                    } else {
                        psiFieldComment += "\n//插件提示无法获取导航属性代理:" + propertyType;
                    }
                    String psiAnnotationValue = PsiUtil.getPsiAnnotationValue(navigate, "value", "");
                    if (psiAnnotationValue.endsWith("ToMany")) {
                        includeManyProperty = true;
                        aptFileCompiler.addImports("com.easy.query.core.proxy.columns.SQLQueryable");
                    }
                }
                aptValueObjectInfo.addProperties(new AptPropertyInfo(name, propertyColumn, psiFieldComment, fieldName, isValueObject, entityName, includeProperty, includeManyProperty, proxyPropertyName, beanPropType));
                aptFileCompiler.addImports(propertyColumn.getImport());


                if (isValueObject) {
                    aptFileCompiler.addImports("com.easy.query.core.proxy.AbstractValueObjectProxyEntity");
                    aptFileCompiler.addImports(psiFieldPropertyType);
                    PsiType fieldType = field.getType();
                    PsiClass fieldClass = ((PsiClassType) fieldType).resolve();
                    if (fieldClass == null) {
                        log.warn("field [" + name + "] is value object,cant resolve PsiClass");
                        continue;
                    }
                    AptValueObjectInfo fieldAptValueObjectInfo = new AptValueObjectInfo(fieldClass.getName());
                    aptValueObjectInfo.getChildren().add(fieldAptValueObjectInfo);
                    addValueObjectClass(project, name, fieldAptValueObjectInfo, fieldClass, aptFileCompiler, tableAndProxyIgnoreProperties);
                }

            }

            VelocityContext context = new VelocityContext();
            context.put("aptValueObjectInfo", aptValueObjectInfo);
            context.put("aptFileCompiler", aptFileCompiler);
            String suffix = ".java"; //Modules.getProjectTypeSuffix(moduleForFile);
            PsiFile psiProxyFile = VelocityUtils.render(project, context, Template.getTemplateContent("AptTemplate2" + suffix), proxyEntityName + suffix);
            CodeStyleManager.getInstance(project).reformat(psiProxyFile);
            psiDirectoryMap.computeIfAbsent(psiDirectory, k -> new ArrayList<>()).add(new GenerateFileEntry(psiProxyFile, allCompileFrom, strategy));
        });
    }

    private static PropertyColumn getPropertyColumn(String fieldGenericType,Boolean anyType) {
        return TYPE_COLUMN_MAPPING.getOrDefault(fieldGenericType, new PropertyColumn2Impl("SQLAnyTypeColumn", fieldGenericType,anyType));
    }

    private static String getNavigatePropertyProxyFullName(Project project, String fullClassName, boolean propIsProxy) {
//        if(propertyColumn.getPropertyType().equals("com.easy.query.test.entity.school.MySchoolClass1")){
        if (!fullClassName.contains(".")) {
            return null;
        }
        PsiClass psiClass = getNavigatePropertyProxyClass(project, fullClassName);

        if (psiClass != null) {


            PsiAnnotation entityProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityProxy");
            if (entityProxy != null) {
                String psiAnnotationValue = PsiUtil.getPsiAnnotationValue(entityProxy, "value", "");
                if (StrUtil.isBlank(psiAnnotationValue)) {
                    return fullClassName.substring(0, fullClassName.lastIndexOf(".")) + ".proxy." + fullClassName.substring(fullClassName.lastIndexOf(".") + 1) + "Proxy";
                }
                return fullClassName.substring(0, fullClassName.lastIndexOf(".")) + ".proxy." + psiAnnotationValue;
            }
            PsiAnnotation entityFileProxy = psiClass.getAnnotation("com.easy.query.core.annotation.EntityFileProxy");
            if (entityFileProxy != null) {
                String psiAnnotationValue = PsiUtil.getPsiAnnotationValue(entityFileProxy, "value", "");
                if (StrUtil.isBlank(psiAnnotationValue)) {
                    return getDefaultClassProxyName(fullClassName);
                }
                return fullClassName.substring(0, fullClassName.lastIndexOf(".")) + ".proxy." + psiAnnotationValue;
            }
        }
        //todo 后续直接不支持别名强制转成classNameProxy
        if (propIsProxy) {
            return getDefaultClassProxyName(fullClassName);
        }
//        }
        return null;
    }

    private static String getDefaultClassProxyName(String fullClassName) {
        return fullClassName.substring(0, fullClassName.lastIndexOf(".")) + ".proxy." + fullClassName.substring(fullClassName.lastIndexOf(".") + 1) + "Proxy";
    }

    private static PsiClass getNavigatePropertyProxyClass(Project project, String fullClassName) {
        PsiClass psiClass = JavaPsiFacade.getInstance(project).findClass(fullClassName, GlobalSearchScope.projectScope(project));
        if (psiClass != null) {
            return psiClass;
        }
        return JavaPsiFacade.getInstance(project).findClass(fullClassName, GlobalSearchScope.allScope(project));
    }

    private static void addValueObjectClass(Project project, String parentProperty, AptValueObjectInfo aptValueObjectInfo, PsiClass fieldValueObjectClass, AptFileCompiler aptFileCompiler, Set<String> tableAndProxyIgnoreProperties) {
        PsiField[] allFields = fieldValueObjectClass.getAllFields();

        String entityName = fieldValueObjectClass.getName();
        aptFileCompiler.addImports(fieldValueObjectClass.getQualifiedName());
        for (PsiField field : allFields) {
            boolean isStatic = PsiUtil.fieldIsStatic(field);
            if(isStatic){
                continue;
            }
            PsiAnnotation columnIgnore = field.getAnnotation("com.easy.query.core.annotation.ColumnIgnore");
            if (columnIgnore != null) {
                continue;
            }
            String name = field.getName();
            //是否存在忽略属性
            if (!tableAndProxyIgnoreProperties.isEmpty() && tableAndProxyIgnoreProperties.contains(parentProperty + "." + name)) {
                continue;
            }
            BeanPropTypeEnum beanPropType = ClassUtil.hasGetterAndSetter(fieldValueObjectClass, name);
            if (beanPropType == BeanPropTypeEnum.NOT) {
                continue;
            }
            PsiAnnotation navigate = field.getAnnotation("com.easy.query.core.annotation.Navigate");
            String psiFieldPropertyType = PsiUtil.getPsiFieldPropertyType(field, navigate != null);
            String psiFieldComment = PsiUtil.getPsiFieldClearComment(field);
            PsiAnnotation valueObject = field.getAnnotation("com.easy.query.core.annotation.ValueObject");
            boolean isValueObject = valueObject != null;
            String fieldName = isValueObject ? psiFieldPropertyType.substring(psiFieldPropertyType.lastIndexOf(".") + 1) : entityName;


            PsiAnnotation proxyProperty = field.getAnnotation("com.easy.query.core.annotation.ProxyProperty");
            String proxyPropertyName = PsiUtil.getPsiAnnotationValue(proxyProperty, "value", null);
            String generateAnyType = PsiUtil.getPsiAnnotationValue(proxyProperty, "generateAnyType", null);
            Boolean anyType = StrUtil.isBlank(generateAnyType) ? null : Objects.equals("true", generateAnyType);

            PropertyColumn propertyColumn = getPropertyColumn(psiFieldPropertyType,anyType);
            aptFileCompiler.addImports(propertyColumn.getImport());

            boolean includeProperty = navigate != null;
            boolean includeManyProperty = false;
            if (includeProperty) {
                aptFileCompiler.addImports("com.easy.query.core.proxy.columns.SQLNavigateColumn");
                String propertyType = propertyColumn.getPropertyType();
                String propIsProxy = PsiUtil.getPsiAnnotationValue(navigate, "propIsProxy", "true");
                String navigatePropertyProxyFullName = getNavigatePropertyProxyFullName(project, propertyType, !Objects.equals("false", propIsProxy));
                if (navigatePropertyProxyFullName != null) {
                    propertyColumn.setNavigateProxyName(navigatePropertyProxyFullName);
                } else {
                    psiFieldComment += "\n//插件提示无法获取导航属性代理:" + propertyType;
                }
                String psiAnnotationValue = PsiUtil.getPsiAnnotationValue(navigate, "value", "");
                if (psiAnnotationValue.endsWith("ToMany")) {
                    includeManyProperty = true;
                    aptFileCompiler.addImports("com.easy.query.core.proxy.columns.SQLQueryable");
                }
            }
            aptValueObjectInfo.addProperties(new AptPropertyInfo(name, propertyColumn, psiFieldComment, fieldName, isValueObject, entityName, includeProperty, includeManyProperty, proxyPropertyName, beanPropType));

            if (valueObject != null) {
                aptFileCompiler.addImports(psiFieldPropertyType);
                PsiType fieldType = field.getType();
                PsiClass fieldClass = ((PsiClassType) fieldType).resolve();
                if (fieldClass == null) {
                    log.warn("field [" + name + "] is value object,cant resolve PsiClass");
                    continue;
                }
                AptValueObjectInfo innerValueObject = new AptValueObjectInfo(fieldClass.getName());
                aptValueObjectInfo.getChildren().add(innerValueObject);
                addValueObjectClass(project, parentProperty + "." + name, innerValueObject, fieldClass, aptFileCompiler, tableAndProxyIgnoreProperties);
            }
        }
    }

}
