package com.easy.query.plugin.core.provider;

import com.easy.query.plugin.core.provider.doctag.EasyQueryDTODocTagInfo;
import com.intellij.psi.javadoc.CustomJavadocTagProvider;
import com.intellij.psi.javadoc.JavadocTagInfo;
import org.apache.commons.compress.utils.Lists;

import java.util.List;


/**
 * 自定义JavaDoc标签提供者, 为后续自定义标签提供支持
 * @author link2fun
 */
public class EasyQueryJavaDocTagProvider implements CustomJavadocTagProvider {
    @Override
    public List<JavadocTagInfo> getSupportedTags() {
        List<JavadocTagInfo> tagInfoList = Lists.newArrayList();
        tagInfoList.add(new EasyQueryDTODocTagInfo());
        return tagInfoList;
    }
}
