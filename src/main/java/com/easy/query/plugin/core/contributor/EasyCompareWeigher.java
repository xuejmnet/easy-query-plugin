package com.easy.query.plugin.core.contributor;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementWeigher;

import java.util.Objects;

/**
 * create time 2024/2/3 17:01
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyCompareWeigher extends LookupElementWeigher {
    public EasyCompareWeigher() {
        super("EasyCompareLookupElementWeigher", false, true);
    }

    @Override
    public Integer weigh(LookupElement element) {
        String lookupString = element.getLookupString();
        if(Objects.equals(">",lookupString)||Objects.equals("<",lookupString)){
            return 0;
        }
        if(Objects.equals(">=",lookupString)||Objects.equals("<=",lookupString)){
            return 1;
        }
        return Integer.MAX_VALUE;
    }
}