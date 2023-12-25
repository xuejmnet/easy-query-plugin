package com.easy.query.plugin.core.entity;

import com.easy.query.plugin.core.util.StrUtil;
import org.apache.commons.lang3.StringUtils;

/**
 * create time 2023/12/6 10:00
 * 文件说明
 *
 * @author xuejiaming
 */
public class AptSelectPropertyInfo {
    private final String propertyName;
    private final String comment;
    private final String proxyPropertyName;

    public AptSelectPropertyInfo(String propertyName, String comment,String proxyPropertyName){

        this.propertyName = propertyName;
        this.comment = comment;
        this.proxyPropertyName = proxyPropertyName;
    }

    public String getPropertyName() {
        return propertyName;
    }
    public String getProxyPropertyName() {
        if(StringUtils.isNotBlank(proxyPropertyName)){
            return proxyPropertyName;
        }
        return propertyName;
    }
    public String getPropertyNameGetMethodName() {
        return "get"+ StrUtil.toUpperCaseFirstOne(propertyName);
    }

    public String getComment() {
        return comment;
    }
}
