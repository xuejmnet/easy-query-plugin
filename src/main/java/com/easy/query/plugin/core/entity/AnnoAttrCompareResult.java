package com.easy.query.plugin.core.entity;

import com.easy.query.plugin.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.intellij.psi.PsiNameValuePair;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 注解的属性比较结果
 * 用于两个相同注解的属性值比对
 *
 * @author link2fun
 */
@Data
public class AnnoAttrCompareResult {


    /**
     * 实体属性Map
     */
    private Map<String, PsiNameValuePair> entityAttrMap;

    /**
     * DTO属性Map
     */
    private Map<String, PsiNameValuePair> dtoAttrMap;

    /**
     * 公共不参与比较的值
     */
    private List<String> ignoredKeys;

    /**
     * 实体可以独享的key, 如果 实体有 而DTO没有, 则允许不报错
     */
    private List<String> entityOnlyKeysPermit;

    /**
     * DTO可以独享的key, 如果 比较有 而实体没有, 则允许不报错
     */
    private List<String> dtoOnlyKeysPermit;


    /**
     * DTO 需要移除的属性
     */
    private List<String> dtoRemoveKeys;


    /// ===== 以上是传入值, 下面是计算出来的值 */

    /**
     * 实体属性Map独有的 key
     */
    private List<String> entityOnlyKeys;

    /**
     * DTO属性Map独有的 key
     */
    private List<String> dtoOnlyKeys;

    /**
     * 实体属性Map和DTO属性Map都有的 key
     */
    private List<String> bothKeys;

    /**
     * 实体属性Map和DTO属性Map都有的 key, 但是值不同
     */
    private List<String> diffKeys;

    /**
     * 实体属性Map和DTO属性Map都有的 key, 但是值相同
     */
    private List<String> sameKeys;

    /**
     * 问题信息
     */
    private List<String> problemMsgList;

    /**
     * 修正后的属性
     */
    private Map<String, PsiNameValuePair> fixedAttrMap;


    /**
     * 创建一个比较对象
     * @param entityAttrMap 实体的注解属性Map
     * @param dtoAttrMap DTO的注解属性Map
     * @return 比较结果
     */
    public static AnnoAttrCompareResult newCompare(Map<String, PsiNameValuePair> entityAttrMap, Map<String, PsiNameValuePair> dtoAttrMap) {
        AnnoAttrCompareResult result = new AnnoAttrCompareResult();
        result.setEntityAttrMap(entityAttrMap);
        result.setDtoAttrMap(dtoAttrMap);
        result.setIgnoredKeys(Lists.newArrayList());
        result.setEntityOnlyKeysPermit(Lists.newArrayList());
        result.setDtoOnlyKeysPermit(Lists.newArrayList());
        result.setProblemMsgList(Lists.newArrayList());
        result.setDtoRemoveKeys(Lists.newArrayList());

        return result;
    }


    /** 设置忽略比较的key*/
    public AnnoAttrCompareResult withIgnoredKeys(List<String> ignoredKeys) {
        this.setIgnoredKeys(ignoredKeys);
        return this;
    }

    /** 设置允许实体独有的key*/
    public AnnoAttrCompareResult withEntityOnlyKeysPermit(List<String> baseOnlyKeysPermit) {
        this.setEntityOnlyKeysPermit(baseOnlyKeysPermit);
        return this;
    }

    /** 设置允许DTO独有的key*/
    public AnnoAttrCompareResult withDtoOnlyKeysPermit(List<String> compareOnlyKeysPermit) {
        this.setDtoOnlyKeysPermit(compareOnlyKeysPermit);
        return this;
    }

    /** 设置DTO需要移除的key*/
    public AnnoAttrCompareResult withDtoRemoveKeys(List<String> dtoRemoveKeys) {
        if (Objects.isNull(this.getDtoRemoveKeys())) {
            this.setDtoRemoveKeys(Lists.newArrayList());
        }
        this.getDtoRemoveKeys().addAll(dtoRemoveKeys);
        return this;
    }


    /**
     * 进行比较
     */
    public AnnoAttrCompareResult compare() {

        // 1. 计算出基准属性Map独有的 key
        entityOnlyKeys = entityAttrMap.keySet().stream()
                .filter(key -> !dtoAttrMap.containsKey(key))
                .collect(Collectors.toList());

        // 2. 计算出比较属性Map独有的 key
        dtoOnlyKeys = dtoAttrMap.keySet().stream()
                .filter(key -> !entityAttrMap.containsKey(key))
                .collect(Collectors.toList());

        // 3. 计算出基准属性Map和比较属性Map都有的 key
        bothKeys = entityAttrMap.keySet().stream()
                .filter(key -> dtoAttrMap.containsKey(key))
                .collect(Collectors.toList());

        // 4. 计算出基准属性Map和比较属性Map都有的 key, 但是值不同
        diffKeys = bothKeys.stream()
                .filter(key -> !StrUtil.equals(entityAttrMap.get(key).getText(), dtoAttrMap.get(key).getText()))
                .collect(Collectors.toList());

        // 5. 计算出基准属性Map和比较属性Map都有的 key, 但是值相同
        sameKeys = bothKeys.stream()
                .filter(key -> StrUtil.equals(entityAttrMap.get(key).getText(), dtoAttrMap.get(key).getText()))
                .collect(Collectors.toList());


        // 开始统计问题

        entityOnlyKeys.stream().filter(key -> !entityOnlyKeysPermit.contains(key))
                .filter(key -> !dtoRemoveKeys.contains(key)) // 如果实体独有的属性在DTO需要移除的属性里面, 则不报错
                .forEach(key -> problemMsgList.add("需要添加属性 " + entityAttrMap.get(key).getText()));

        dtoOnlyKeys.stream().filter(key -> !dtoOnlyKeysPermit.contains(key))
                .forEach(key -> problemMsgList.add("需要移除属性 " + dtoAttrMap.get(key).getText()));

        diffKeys.forEach(key -> {
            if (dtoRemoveKeys.contains(key)) {
                problemMsgList.add("需要移除属性 " + dtoAttrMap.get(key).getText());
            } else {
                problemMsgList.add("需要修改属性 " + dtoAttrMap.get(key).getText() + " 为 " + entityAttrMap.get(key).getText());
            }
        });

        // 如果是相同的属性, 但是在需要移除的里面, 那么也提示移除
        sameKeys.stream().filter(key -> dtoRemoveKeys.contains(key))
                .forEach(key -> problemMsgList.add("需要移除属性 " + dtoAttrMap.get(key).getText()));


        // 生成一个修正后的属性
        fixedAttrMap = Maps.newHashMap();

        // sameKeys 的保留
        sameKeys.forEach(key -> {
            if (dtoRemoveKeys.contains(key)) {
                return;
            }
            fixedAttrMap.put(key, entityAttrMap.get(key));
        });
        // diffKeys 的保留
        diffKeys.forEach(key -> {
            if (dtoRemoveKeys.contains(key)) {
                return;
            }
            fixedAttrMap.put(key, entityAttrMap.get(key));
        });

        // compareOnlyKeys 如果在 permit 里面, 则保留
        dtoOnlyKeys.stream().filter(key -> dtoOnlyKeysPermit.contains(key))
                .forEach(key -> {
                    if (dtoRemoveKeys.contains(key)) {
                        return;
                    }
                    fixedAttrMap.put(key, dtoAttrMap.get(key));
                });

        // baseOnlyKeys 如果如果不在 permit 里面, 则保留
        entityOnlyKeys.stream().filter(key -> !entityOnlyKeysPermit.contains(key))
                .forEach(key -> {
                    if (dtoRemoveKeys.contains(key)) {
                        return;
                    }
                    fixedAttrMap.put(key, entityAttrMap.get(key));
                });


        // 修正后的属性


        return this;


    }


}
