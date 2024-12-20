package com.easy.query.plugin.core.provider;

import com.easy.query.plugin.core.icons.Icons;
import com.easy.query.plugin.core.util.PsiJavaClassUtil;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo;
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * DTO字段跳转到实体字段
 * @author link2fun
 */
public class NavToEntityFieldRelatedItemLineMarkerProvider extends RelatedItemLineMarkerProvider {

    @Override
    protected void collectNavigationMarkers(@NotNull PsiElement element, @NotNull Collection<? super RelatedItemLineMarkerInfo<?>> result) {

        // 如果元素不是字段, 则返回
        if (!(element instanceof PsiField)) {
            return;
        }

        // 如果元素是字段, 则添加导航标记
        PsiField field = (PsiField) element;

        if (field.getIdentifyingElement() == null) {
            return;
        }

        // 获取field所在类
        PsiClass containingClass = field.getContainingClass();

        // 获取类上面的 @link class
        PsiClass linkPsiClass = PsiJavaClassUtil.getLinkPsiClass(containingClass);

        // 获取 linkPsiClass 上同名的field
        if (linkPsiClass != null) {

            PsiField linkPsiField = linkPsiClass.findFieldByName(field.getName(), true);
            if (linkPsiField != null) {
                // 找到了同名字段， 创建导航
                RelatedItemLineMarkerInfo<PsiElement> navInfo = NavigationGutterIconBuilder.create(Icons.EQ)
                        .setTargets(linkPsiField)
                        .setTooltipText("Navigate to Entity Field")
                        .createLineMarkerInfo(field.getIdentifyingElement());
                result.add(navInfo);
            }
        }


        super.collectNavigationMarkers(element, result);
    }
}
