package com.easy.query.plugin.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.easy.query.plugin.core.config.CustomConfig;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndex;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.ArrayUtil;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模块
 *
 * @author daijunxiong
 * @date 2023/06/22
 */
public class Modules {
    private static Map<String, Module> moduleMap;
    private static Map<String, Map<String, String>> modulePackageMap;
    private static Boolean isManvenProject;

    /**
     * 得到包路径
     *
     * @param moduleName  模块名称
     * @param packageName 系统配置包名
     * @return {@code String}
     */
    public static String getPackagePath(String moduleName, String packageName) {
        Map<String, String> moduleMap = modulePackageMap.get(moduleName);
        if (CollUtil.isEmpty(moduleMap)) {
            NotificationUtils.notifyError("模块不存在!", "", ProjectUtils.getCurrentProject());
            throw new RuntimeException("模块不存在:" + moduleName);
        }
        return moduleMap.getOrDefault(packageName, "");
    }

    public static Module[] getModule(Project project) {
        return ModuleManager.getInstance(project).getModules();
    }

//    /**
//     * 获取模块
//     *
//     * @param project 项目
//     * @return {@code List<String>}
//     */
//    public static void addModulesItem(Project project, List<JComboBox> modulesComboxs) {
//        Module[] modules = ModuleManager.getInstance(project).getModules();
//        if (ArrayUtil.isEmpty(modules)) {
//            NotificationUtils.notifyError("目录层级有误!", "", project);
//            return;
//        }
//
//        boolean isManvenProject = isManvenProject(modules[0]);
//        for (JComboBox modulesCombox : modulesComboxs) {
//            modulesCombox.setRenderer(new ModuleComBoxRender());
//
//            moduleMap = Arrays.stream(modules)
//                    .filter(module -> {
//                        if (isManvenProject) {
//                            return ArrayUtil.isNotEmpty(ModuleRootManager.getInstance(module).getSourceRoots());
//                        }
//                        // 非maven项目只显示main模块,只有main模块才有java目录
//                        return module.getName().contains(".main");
//                    })
//                    .collect(Collectors.toMap(el -> {
//                        String name = el.getName();
//                        if (name.contains(".")) {
//                            String[] strArr = name.split("\\.");
//                            return strArr[strArr.length - 2];
//                        }
//                        return name;
//                    }, module -> module));
//            FilterComboBoxModel model = new FilterComboBoxModel(moduleMap.keySet().stream().collect(Collectors.toList()));
//            modulesCombox.setModel(model);
//
//            modulesCombox.setSelectedIndex(0);
//        }
//        getModulePackages();
//    }
//
//    public static void getModulePackages() {
//        modulePackageMap = new HashMap<>();
//        Project project = ProjectUtils.getCurrentProject();
//        for (Module module : moduleMap.values()) {
//            Map<String, String> moduleMap = new HashMap<>();
//            PsiManager psiManager = PsiManager.getInstance(project);
//            FileIndex fileIndex = module != null ? ModuleRootManager.getInstance(module).getFileIndex() : ProjectRootManager.getInstance(project).getFileIndex();
//            fileIndex.iterateContent(fileOrDir -> {
//                if (fileOrDir.isDirectory() && (fileIndex.isUnderSourceRootOfType(fileOrDir, JavaModuleSourceRootTypes.SOURCES) || fileIndex.isUnderSourceRootOfType(fileOrDir, JavaModuleSourceRootTypes.RESOURCES))) {
//                    PsiDirectory psiDirectory = psiManager.findDirectory(fileOrDir);
//                    PsiPackage aPackage = JavaDirectoryService.getInstance().getPackage(psiDirectory);
//                    if (aPackage != null) {
//                        moduleMap.put(aPackage.getName(), aPackage.getQualifiedName());
//                    }
//                }
//                return true;
//            });
//            String name = module.getName();
//            if (name.contains(".")) {
//                String[] strArr = name.split("\\.");
//                name = strArr[strArr.length - 2];
//            }
//            modulePackageMap.put(name, moduleMap);
//        }
//    }

    /**
     * 判断是否manven项目
     *
     * @return boolean
     */
    public static boolean isManvenProject(Module module) {
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        if (ArrayUtil.isEmpty(contentRoots)) {
            return false;
        }
        VirtualFile contentRoot = contentRoots[0];
        VirtualFile virtualFile = contentRoot.findChild("pom.xml");
        isManvenProject = Objects.nonNull(virtualFile);
        return isManvenProject;
    }

    public static String getProjectTypeSuffix(Module module) {
        return isManvenProject(module) ? ".java" : ".kt";
    }


    /**
     * 获取模块
     *
     * @param moduleName 模块名称
     * @return {@code Module}
     */
    public static Module getModule(String moduleName) {
        return moduleMap.get(moduleName);
    }

    /**
     * 获取模块路径
     *
     * @param moduleName 模块名称
     * @return {@code String}
     */
    public static String getModulePath(String moduleName) {
        Module module = moduleMap.get(moduleName);
        return getModulePath(module, JavaModuleSourceRootTypes.SOURCES);
    }

    public static String getModulePath(Module module, Set javaResourceRootTypes) {
        AtomicReference<String> path = new AtomicReference<>();

        ModuleFileIndex fileIndex = ModuleRootManager.getInstance(module).getFileIndex();
        fileIndex.iterateContent(fileOrDir -> {
            if (fileOrDir.isDirectory() && fileIndex.isUnderSourceRootOfType(fileOrDir, javaResourceRootTypes)) {
                String canonicalPath = fileOrDir.getCanonicalPath();
                path.set(canonicalPath);
                return false;
            }
            return true;
        });
        return path.get();
    }


    public static PsiDirectory getModuleDirectory(Module module, Set javaResourceRootTypes) {
        AtomicReference<PsiDirectory> directory = new AtomicReference<>();
        ModuleFileIndex fileIndex = ModuleRootManager.getInstance(module).getFileIndex();
        fileIndex.iterateContent(fileOrDir -> {
            if (fileOrDir.isDirectory() && fileIndex.isUnderSourceRootOfType(fileOrDir, javaResourceRootTypes)) {
                directory.set(VirtualFileUtils.getPsiDirectory(module.getProject(), fileOrDir));
                return false;
            }
            return true;
        });
        return directory.get();
    }


//    /**
//     * 初始化模块
//     *
//     * @param project 项目
//     * @param list    列表
//     */
//    public static void initModules(Project project, List<JComboBox> list) {
//        addModulesItem(project, list);
//    }

    /**
     * service联动
     *
     * @param serviceInteCombox 服务强度combox
     * @param serviceImplComBox 服务impl com盒子
     */
    public static void comBoxGanged(JComboBox serviceInteCombox, JComboBox serviceImplComBox) {
        serviceInteCombox.addActionListener(e -> {
            serviceImplComBox.setSelectedIndex(serviceInteCombox.getSelectedIndex());
            serviceImplComBox.revalidate();
            serviceImplComBox.repaint();
        });

        serviceImplComBox.addActionListener(e -> {
            serviceInteCombox.setSelectedIndex(serviceImplComBox.getSelectedIndex());
            serviceInteCombox.revalidate();
            serviceInteCombox.repaint();
        });
    }

    /**
     * 同步controller模块
     *
     * @param modulesComboxs
     * @param idx
     */
    public static void syncModules(List<JComboBox> modulesComboxs, int idx) {
        for (JComboBox modulesCombox : modulesComboxs) {
            modulesCombox.setSelectedIndex(idx);
            modulesCombox.revalidate();
            modulesCombox.repaint();
        }

    }

    public static String getModuleName(Module module) {
        return module.getName().replaceAll("\\.main", "");
    }

    public static String getPath(Module moduleForFile) {
        PsiDirectory moduleDirectory = getModuleDirectory(moduleForFile, JavaModuleSourceRootTypes.SOURCES);
        if (moduleDirectory != null) {
            String path = moduleDirectory.getVirtualFile().getPath();
            return StrUtil.subBefore(path, "src", false);
        }
        return "";
    }


    public static CustomConfig moduleConfig(Module module) {
        if (Objects.isNull(module)) {
            return new CustomConfig();
        }
        String path = getPath(module);
        PsiFile file = null;
        PsiDirectory psiDirectory = VirtualFileUtils.getPsiDirectory(module.getProject(), path);
        while (Objects.isNull(file) && Objects.nonNull(psiDirectory)) {
            file = psiDirectory.findFile("easy-query.config");
            if (Objects.isNull(file)) {
                // 往上找
                psiDirectory = psiDirectory.getParent();
            }
        }
        if (Objects.isNull(file)) {
            return new CustomConfig();
        }
        CustomConfig config = new CustomConfig();
        try {
            Arrays.stream(file.getText().split("\n"))
                    .filter(el -> el.startsWith("processor"))
                    .forEach(el -> {
                        String text = StrUtil.subAfter(el, ".", false);
                        if (StrUtil.count(el, ".") > 1) {
                            String[] split = text.split("\\.");
                            text = split[0];
                            if (split.length > 1) {
                                text += StrUtil.upperFirst(split[1]);
                            }
                        }
                        String prefix = StrUtil.toCamelCase(StrUtil.subBefore(text, "=", false)).trim();
                        String suffix = StrUtil.subAfter(text, "=", false).trim();
                        ReflectUtil.setFieldValue(config, prefix, suffix);
                    });
        } catch (Exception e) {

        }
        return config;
    }
}
