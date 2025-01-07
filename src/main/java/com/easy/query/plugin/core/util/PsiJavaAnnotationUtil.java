package com.easy.query.plugin.core.util;


import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationParameterList;
import com.intellij.psi.PsiNameValuePair;
import groovy.lang.Tuple3;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Java 注解工具类
 *
 * @author link2fun
 */
public class PsiJavaAnnotationUtil {


    /**
     * 将注解的属性转为Map
     *
     * @param anno 注解
     * @return Map key attrName value:attrValue
     */
    public static Map<String, PsiNameValuePair> attrToMap(PsiAnnotation anno) {
        return Optional.ofNullable(anno).map(PsiAnnotation::getParameterList)
                .map(PsiAnnotationParameterList::getAttributes)
                .map(Lists::newArrayList)
                .orElseGet(Lists::newArrayList)
                .stream()
                .collect(Collectors.toMap(PsiNameValuePair::getAttributeName, Function.identity()));
    }


    /**
     * 忽略指定属性的情况下, 检查新注解是否和基准注解一致
     *
     * @param baseAttrMap    基准注解属性, 比如 实体上的 @Column 注解属性
     * @param compareAttrMap 要与基准注解属性比对的新注解属性 比如 DTO上的  @Column 注解属性
     * @param ignoreAttrs    忽略的属性
     * @return Tuple3<Boolean, List, Map> 第一个元素表示是否一致 true 一致, false 一不致, 第二个元素表示不一致的信息, 第三个元素是精简后的属性值, 如果不一致, 则需要用精简后的属性值构建新的注解
     */
    public static Tuple3<Boolean, List<String>, Map<String, PsiNameValuePair>> compareAttrMap(Map<String, PsiNameValuePair> baseAttrMap, Map<String, PsiNameValuePair> compareAttrMap, String... ignoreAttrs) {
        List<String> diffList = Lists.newArrayList();
        Map<String, PsiNameValuePair> newAttrMap = baseAttrMap.entrySet().stream()
                .filter(entry -> !Lists.newArrayList(ignoreAttrs).contains(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // 合并 newAttrMap 和 compareAttrMap 的key 值
        HashSet<String> allKeySet = Sets.newHashSet(newAttrMap.keySet());
        allKeySet.addAll(compareAttrMap.keySet());

        // 排序
        List<String> keyList = allKeySet.stream().sorted().collect(Collectors.toList());

        for (String attrName : keyList) {
            PsiNameValuePair baseAttrValue = baseAttrMap.get(attrName);
            PsiNameValuePair compareAttrValue = compareAttrMap.get(attrName);

            if (Objects.isNull(compareAttrValue)) {
                diffList.add("需要设置" + baseAttrValue.getText());
                continue;
            }
            if (Objects.isNull(baseAttrValue)) {
                diffList.add("需要移除" + compareAttrValue.getText());
                continue;
            }
            if (!StrUtil.equals(baseAttrValue.getText(), compareAttrValue.getText())) {
                diffList.add("需要修改" + compareAttrValue.getText() + " 为 " + baseAttrValue.getText());
            }

        }


        return new Tuple3<>(diffList.isEmpty(), diffList, newAttrMap);
    }
}
