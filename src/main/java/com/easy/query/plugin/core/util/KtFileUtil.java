package com.easy.query.plugin.core.util;

import org.jetbrains.kotlin.psi.KtFile;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class KtFileUtil {

    public static Set<String> getImportSet(KtFile ktFile) {
        return Objects.requireNonNull(ktFile.getImportList()).getImports().stream()
                .map(el -> Objects.requireNonNull(el.getImportedFqName()).asString())
                .collect(Collectors.toSet());
    }
}
