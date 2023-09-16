//package com.easy.query.plugin.core.util;
//
//import java.lang.reflect.Field;
//
///**
// * create time 2023/9/16 14:36
// * 文件说明
// *
// * @author xuejiaming
// */
//public class ReflectUtil {
//    /**
//     * 设置字段值
//     *
//     * @param obj       对象,static字段则此处传Class
//     * @param fieldName 字段名
//     * @param value     值，值类型必须与字段类型匹配，不会自动转换对象类型
//     */
//    public static void setFieldValue(Object obj, String fieldName, Object value) {
//        Assert.notNull(obj);
//        Assert.notBlank(fieldName);
//
//        final Field field = getField((obj instanceof Class) ? (Class<?>) obj : obj.getClass(), fieldName);
//        Assert.notNull(field, "Field [{}] is not exist in [{}]", fieldName, obj.getClass().getName());
//        setFieldValue(obj, field, value);
//    }
//
//    public static Field getField(Class<?> beanClass, String name) throws SecurityException {
//        final Field[] fields = getFields(beanClass);
//        return ArrayUtil.firstMatch((field) -> name.equals(getFieldName(field)), fields);
//    }public static Field[] getFields(Class<?> beanClass) throws SecurityException {
//        Assert.notNull(beanClass);
//        return FIELDS_CACHE.computeIfAbsent(beanClass, () -> getFieldsDirectly(beanClass, true));
//    }
//}
