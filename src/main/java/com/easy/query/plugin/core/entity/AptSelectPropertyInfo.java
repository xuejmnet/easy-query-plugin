package com.easy.query.plugin.core.entity;

import com.easy.query.plugin.core.enums.BeanPropTypeEnum;
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
    private final BeanPropTypeEnum beanPropType;

    public AptSelectPropertyInfo(String propertyName, String comment, String proxyPropertyName, BeanPropTypeEnum beanPropType){

        this.propertyName = propertyName;
        this.comment = comment;
        this.proxyPropertyName = proxyPropertyName;
        this.beanPropType = beanPropType;
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
        if(beanPropType==BeanPropTypeEnum.IS){
            return "is"+ StrUtil.toUpperCaseFirstOne(propertyName);
        }
        return "get"+ StrUtil.toUpperCaseFirstOne(propertyName);
    }

    public String getComment() {
        return comment;
    }
}
