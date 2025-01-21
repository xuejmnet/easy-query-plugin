package com.easy.query.plugin.action.navgen;

import lombok.Data;
/**
 * Nav 注解生成GUI
 * @author link2fun
 */
@Data
public class NavMappingRelation {
    private String sourceEntity;
    private String targetEntity;
    private String[] sourceFields;
    private String[] targetFields;
    private String relationType;
    private String mappingClass;
    private String[] selfMappingFields;
    private String[] targetMappingFields;
    private boolean propIsProxy;

    public NavMappingRelation(String sourceEntity, String targetEntity,
            String[] sourceFields, String[] targetFields,
                              String relationType, String mappingClass,
            String[] selfMappingFields, String[] targetMappingFields,
            boolean propIsProxy) {
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
        this.sourceFields = sourceFields;
        this.targetFields = targetFields;
        this.relationType = relationType;
        this.mappingClass = mappingClass;
        this.selfMappingFields = selfMappingFields;
        this.targetMappingFields = targetMappingFields;
        this.propIsProxy = propIsProxy;
    }

}