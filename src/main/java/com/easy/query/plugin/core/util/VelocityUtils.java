package com.easy.query.plugin.core.util;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.jetbrains.kotlin.idea.KotlinFileType;

import java.io.StringWriter;

public class VelocityUtils {
    private static final VelocityEngine VELOCITY_ENGINE = new VelocityEngine();

    public static PsiFile render(Project project, VelocityContext context, String template, String fileName) {
        PsiFileFactory factory = PsiFileFactory.getInstance(project);
        StringWriter sw = new StringWriter();
        VELOCITY_ENGINE.evaluate(context, sw, "easy-query", template);
        LanguageFileType languageFileType = (fileName == null || fileName.endsWith(".java")) ? JavaFileType.INSTANCE : KotlinFileType.INSTANCE;
        return factory.createFileFromText(fileName,languageFileType , sw.toString());

    }
}
