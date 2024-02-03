package com.easy.query.plugin.core.contributor;

import com.intellij.codeInsight.completion.PrefixMatcher;
import org.jetbrains.annotations.NotNull;

/**
 * create time 2024/2/3 16:57
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyComparePrefixMatcher extends PrefixMatcher {
    final PrefixMatcher inner;

    public EasyComparePrefixMatcher(PrefixMatcher inner) {
        super(inner.getPrefix());
        this.inner = inner;
    }
    @Override
    public boolean prefixMatches(@NotNull String name) {
        return name.startsWith(">");
    }

    @Override
    public @NotNull PrefixMatcher cloneWithPrefix(@NotNull String prefix) {
        return new EasyComparePrefixMatcher(inner.cloneWithPrefix(prefix));
    }
}
