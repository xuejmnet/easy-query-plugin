package com.easy.query.plugin.core.util;

import com.alibaba.fastjson2.JSON;
import com.easy.query.plugin.core.config.CustomConfig;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleFileIndex;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 模块
 *
 * @author daijunxiong
 * @date 2023/06/22
 */
public class MyModuleUtil {
    private static Map<String, Module> moduleMap;
    private static Map<String, Map<String, String>> modulePackageMap;
    private static Boolean isManvenProject;

//    /**
//     * 得到包路径
//     *
//     * @param moduleName  模块名称
//     * @param packageName 系统配置包名
//     * @return {@code String}
//     */
//    public static String getPackagePath(String moduleName, String packageName) {
//        Map<String, String> moduleMap = modulePackageMap.get(moduleName);
//        if (CollUtil.isEmpty(moduleMap)) {
//            NotificationUtils.notifyError("模块不存在!", "", ProjectUtils.getCurrentProject());
//            throw new RuntimeException("模块不存在:" + moduleName);
//        }
//        return moduleMap.getOrDefault(packageName, "");
//    }

    public static boolean isMavenProject(Module module) {
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
        if (ArrayUtil.isEmpty(contentRoots)) {
            return false;
        }
        VirtualFile contentRoot = contentRoots[0];
        VirtualFile virtualFile = contentRoot.findChild("pom.xml");
        isManvenProject = Objects.nonNull(virtualFile);
        return isManvenProject;
    }

    public static Module[] getModules(Project project) {
        return ModuleManager.getInstance(project).getModules();
    }

    public static Module getModule(Project project, String moduleName) {
        Module[] modules = getModules(project);
        if (ArrayUtil.isEmpty(modules)) {
            NotificationUtils.notifyError("目录层级有误!", "", project);
            return null;
        }
        boolean isMavenProject = MyModuleUtil.isMavenProject(modules[0]);
        Map<String, Module> moduleMap = Arrays.stream(modules)
                .filter(module -> {
                    if (isMavenProject) {
                        @NotNull VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getSourceRoots();
                        return sourceRoots.length > 0;
                    }
                    // 非maven项目只显示main模块,只有main模块才有java目录
                    return module.getName().contains(".main");
                })
                .collect(Collectors.toMap(el -> {
                    String name = el.getName();
                    if (name.contains(".")) {
                        String[] strArr = name.split("\\.");
                        return strArr[strArr.length - 2];
                    }
                    return name;
                }, module -> module));
        return moduleMap.get(moduleName);
    }


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


    public static String getProjectTypeSuffix(Module module) {
        return isMavenProject(module) ? ".java" : ".kt";
    }

//
//    /**
//     * 获取模块
//     *
//     * @param moduleName 模块名称
//     * @return {@code Module}
//     */
//    public static Module getModule(String moduleName) {
//        return moduleMap.get(moduleName);
//    }

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
                String path = fileOrDir.getPath();
                if (path.contains("/src/main")) {
                    directory.set(VirtualFileUtils.getPsiDirectory(module.getProject(), fileOrDir));
                    return false;
                }
            }
            return true;
        });
        return directory.get();
    }


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
        String text = file.getText();
        try {
            return JSON.parseObject(text, CustomConfig.class);
        }catch (Exception ex){
            NotificationUtils.notifyError("配置文件有误:"+ex.getMessage(), "", module.getProject());
        }
        CustomConfig config = new CustomConfig();
//        try {
//            Arrays.stream(file.getText().split("\n"))
//                    .filter(el -> el.startsWith("processor"))
//                    .forEach(el -> {
//                        String text = StrUtil.subAfter(el, ".", false);
//                        if (StrUtil.count(el, ".") > 1) {
//                            String[] split = text.split("\\.");
//                            text = split[0];
//                            if (split.length > 1) {
//                                text += StrUtil.upperFirst(split[1]);
//                            }
//                        }
//                        String prefix = StrUtil.toCamelCase(StrUtil.subBefore(text, "=", false)).trim();
//                        String suffix = StrUtil.subAfter(text, "=", false).trim();
////                        ReflectUtil.setFieldValue(config, prefix, suffix);
//                    });
//        } catch (Exception e) {
//
//        }
        return config;
    }
}
