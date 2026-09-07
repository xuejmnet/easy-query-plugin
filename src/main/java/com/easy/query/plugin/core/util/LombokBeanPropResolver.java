package com.easy.query.plugin.core.util;

import com.easy.query.plugin.core.enums.BeanPropTypeEnum;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;

import com.intellij.psi.impl.source.PsiExtensibleClass;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Lombok 快速路径判定器。
 *
 * <p>背景：生成代理类时对每个属性调用 {@code findMethodsByName} 检查 getter/setter，
 * 对标注了 Lombok 注解的类首次成员查询会触发 Lombok 插件的 PsiAugmentProvider 增强，
 * 单个大实体类首触可达数秒，编译全部场景下整段阻塞 EDT 引发 Freeze 报告。</p>
 *
 * <p>方案：当“该属性是否同时存在公开 getter 与 setter”可以仅由当前类及字段上的
 * Lombok 注解语义确定时直接给出结论，完全绕开 PSI 方法解析；任何歧义一律返回 {@code null}，
 * 由调用方退回原 findMethodsByName 路径——回退路径即旧实现本身，因此保证生成结果与现状逐一一致。</p>
 */
public final class LombokBeanPropResolver {

    private static final Logger log = Logger.getInstance(LombokBeanPropResolver.class);

    /** 沿父类链扫描显式同名方法的最大深度，防御异常层级结构 */
    private static final int MAX_SUPER_DEPTH = 32;

    /** lombok @Getter/@Setter 的 AccessLevel 归纳档位 */
    enum Lv {ABSENT, NONE, NON_PUBLIC, PUBLIC}

    private LombokBeanPropResolver() {
    }

    /**
     * 仅当 getter 与 setter 的存在性都可确定为“真实生成且公开”时返回对应枚举；
     * 其余一切不确定情形返回 {@code null} 表示走原解析路径。
     */
    public static BeanPropTypeEnum resolveIfDeterministic(PsiClass psiClass, PsiField field) {
        try {
            if (psiClass == null || field == null) {
                return null;
            }
            // 快速路径的等价推导只对 Java 源码类成立（Kotlin 类不走 Lombok 增强）
            if (!JavaLanguage.INSTANCE.equals(psiClass.getLanguage())
                    || !(psiClass instanceof PsiExtensibleClass)) {
                return null;
            }
            PsiExtensibleClass javaClass = (PsiExtensibleClass) psiClass;
            PsiAnnotation data = psiClass.getAnnotation("lombok.Data");
            PsiAnnotation classGetter = psiClass.getAnnotation("lombok.Getter");
            PsiAnnotation classSetter = psiClass.getAnnotation("lombok.Setter");
            if (data == null && classGetter == null && classSetter == null) {
                // 非 Lombok 类成员查询不触发增强，旧路径本就很快
                return null;
            }

            String fieldName = field.getName();
            // final/static/$开头字段的生成规则与注解组合相关，保留原路径判定
            if (fieldName.startsWith("$")
                    || field.hasModifierProperty(PsiModifier.STATIC)
                    || field.hasModifierProperty(PsiModifier.FINAL)) {
                return null;
            }
            // @Accessors(fluent/prefix) 会改变方法命名规则，等价性无法静态保证，回退
            if (field.getAnnotation("lombok.experimental.Accessors") != null
                    || field.getAnnotation("lombok.Accessors") != null
                    || psiClass.getAnnotation("lombok.experimental.Accessors") != null
                    || psiClass.getAnnotation("lombok.Accessors") != null) {
                return null;
            }
            // 本类或父类链中已有同目标候选名时，Lombok 的跳过逻辑与可见性判断交织，
            // 且旧实现的 findMethodsByName 可能直接命中这些成员，回退
            String capitalizedPropertyName = capitalize(fieldName);
            Set<String> candidates = new HashSet<>();
            candidates.add("get" + capitalizedPropertyName);
            candidates.add("is" + capitalizedPropertyName);
            candidates.add("set" + capitalizedPropertyName);
            if (declaresAnyCandidate(javaClass, candidates) || superChainDeclaresAnyCandidate(javaClass, candidates)) {
                return null;
            }

            boolean primitiveBoolean = PsiType.BOOLEAN.equals(field.getType());
            return decideByTable(fieldName, primitiveBoolean,
                    effective(field, classGetter, data != null, "lombok.Getter"),
                    effective(field, classSetter, data != null, "lombok.Setter"));
        } catch (Exception e) {
            // 快速路径永远不抛异常；任何意外形态都交回旧实现
            log.debug("Lombok fast path skipped: " + e.getMessage());
            return null;
        }
    }

    /**
     * 等价判定表（纯逻辑，便于单元测试）。仅在“本类与父类链均无候选同名成员”前提下可达：
     * <ul>
     *   <li>原生布尔字段形如 isXxx：Lombok 生成的方法名是 isXxx/setXxx，
     *       旧实现的 getXxx 与 setIsXxx 查找必然落空，等价结论为 NOT；</li>
     *   <li>getter 与 setter 均为 PUBLIC 级且字段非 final/static：
     *       方法必然被生成且公开，与旧查找结果一致；</li>
     *   <li>其余组合（部分缺失、非公开）：老路径还要考虑继承成员等因素，交给原实现。</li>
     * </ul>
     */
    static BeanPropTypeEnum decideByTable(String fieldName, boolean primitiveBoolean, Lv getter, Lv setter) {
        boolean isShapeBoolean = primitiveBoolean && fieldName.length() > 2
                && fieldName.startsWith("is") && Character.isUpperCase(fieldName.charAt(2));
        if (isShapeBoolean) {
            return BeanPropTypeEnum.NOT;
        }
        if (primitiveBoolean && fieldName.startsWith("is")) {
            // 其余 is 开头的异常命名（如字面量 is）与 Lombok 前缀规则交互不明，交回原路径
            return null;
        }
        if (getter == Lv.PUBLIC && setter == Lv.PUBLIC) {
            return primitiveBoolean ? BeanPropTypeEnum.IS : BeanPropTypeEnum.GET;
        }
        return null;
    }

    /**
     * 字段级注解优先，其次类级注解，最后按 @Data 的默认值（PUBLIC）；全缺则视为未提供。
     */
    private static Lv effective(PsiField field, PsiAnnotation classLevelAnn, boolean dataPresent, String fqn) {
        PsiAnnotation fieldLevelAnn = field.getAnnotation(fqn);
        if (fieldLevelAnn != null) {
            return levelOf(fieldLevelAnn);
        }
        if (classLevelAnn != null) {
            return levelOf(classLevelAnn);
        }
        return dataPresent ? Lv.PUBLIC : Lv.ABSENT;
    }

    private static Lv levelOf(PsiAnnotation annotation) {
        PsiAnnotationMemberValue value = annotation.findAttributeValue("value");
        String text = value == null ? "" : value.getText();
        if (StrUtil.isBlank(text)) {
            // 属性读取失败时不能臆断，按未提供处理交回原路径
            return Lv.ABSENT;
        }
        if (text.contains("NONE")) {
            return Lv.NONE;
        }
        if (text.contains("PUBLIC")) {
            return Lv.PUBLIC;
        }
        return Lv.NON_PUBLIC;
    }

    /**
     * 只读结构子节点获取自身声明的方法名集合，不经过 ClassInnerStuffCache，不会触发 Lombok 增强。
     */
    private static boolean declaresAnyCandidate(PsiClass psiClass, Set<String> candidates) {
        if (!(psiClass instanceof PsiExtensibleClass)) {
            // 父类实现未暴露“仅源码声明”的成员视图时无法保证等价性
            return true;
        }
        List<PsiMethod> ownMethods = ((PsiExtensibleClass) psiClass).getOwnMethods();
        for (PsiMethod method : ownMethods) {
            if (candidates.contains(method.getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 父类的显式同名成员（无论可见性）都会让 Lombok 跳过生成并可能改变旧查找结果，
     * 因此沿父类链逐一检查其声明的方法；接口默认方法必为公有，不影响正判定，无需展开。
     */
    private static boolean superChainDeclaresAnyCandidate(PsiClass psiClass, Set<String> candidates) {
        PsiClass current = psiClass.getSuperClass();
        for (int depth = 0; current != null && depth < MAX_SUPER_DEPTH; depth++) {
            if (declaresAnyCandidate(current, candidates)) {
                return true;
            }
            current = current.getSuperClass();
        }
        return false;
    }

    private static String capitalize(String s) {
        if (s.length() == 0) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
