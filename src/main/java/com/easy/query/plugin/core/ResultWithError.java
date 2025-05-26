package com.easy.query.plugin.core;

/**
 * create time 2025/5/26 21:18
 * 文件说明
 *
 * @author xuejiaming
 */
public class ResultWithError<T> {
    public final T result;
    public final String error;
    public ResultWithError(T result, String error){
        this.result = result;
        this.error = error;
    }
}
