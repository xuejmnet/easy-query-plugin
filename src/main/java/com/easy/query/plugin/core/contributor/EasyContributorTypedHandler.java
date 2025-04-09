package com.easy.query.plugin.core.contributor;

import com.intellij.codeInsight.AutoPopupController;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.editorActions.TypedHandlerDelegate;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Conditions;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * create time 2024/2/3 21:16
 * 文件说明
 *
 * @author xuejiaming
 */
public class EasyContributorTypedHandler extends TypedHandlerDelegate {
    @Override
    public @NotNull Result checkAutoPopup(char charTyped, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
        if(charTyped == '>' || charTyped == '<'|| charTyped == '='|| charTyped == '!'){
            AutoPopupController.getInstance(project).autoPopupMemberLookup(editor, CompletionType.BASIC, Conditions.alwaysTrue());
            return Result.CONTINUE;
        }
        return super.checkAutoPopup(charTyped,project,editor,file);
    }

//
//    @Override
//    public @NotNull Result charTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
//        if(c == '>' || c == '<'|| c == '='|| c == '!'){
//            return Result.CONTINUE;
//        }
//        return super.charTyped(c, project, editor, file);
//    }
//
//    @Override
//    public @NotNull Result beforeCharTyped(char c, @NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file, @NotNull FileType fileType) {
//        if(c == '>' || c == '<'|| c == '='|| c == '!'){
//            return Result.CONTINUE;
//        }
//        return super.beforeCharTyped(c, project, editor, file, fileType);
//    }
}
