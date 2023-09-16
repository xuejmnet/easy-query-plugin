package com.easy.query.plugin.core.util;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jetbrains.kotlin.idea.KotlinFileType;

import java.io.StringWriter;

public class VelocityUtils {
    private static final VelocityEngine VELOCITY_ENGINE = new VelocityEngine();

    public static PsiFile render(VelocityContext context, String template, String fileName) {
        PsiFileFactory factory = PsiFileFactory.getInstance(ProjectUtils.getCurrentProject());
        StringWriter sw = new StringWriter();
        VELOCITY_ENGINE.evaluate(context, sw, "easy-query", template);
        return factory.createFileFromText(fileName, KotlinFileType.INSTANCE, sw.toString());

    }
}
