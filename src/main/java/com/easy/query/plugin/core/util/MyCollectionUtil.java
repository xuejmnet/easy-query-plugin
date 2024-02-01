package com.easy.query.plugin.core.util;

import java.util.ArrayList;
import java.util.List;

/**
 * create time 2024/1/31 16:58
 * 文件说明
 *
 * @author xuejiaming
 */
public class MyCollectionUtil {
    public static <T> List<List<T>> partition(List<T> list, int size) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty.");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive.");
        }
        List<List<T>> partitions = new ArrayList<>();
        int numberOfPartitions = (int) Math.ceil((double) list.size() / size);
        for (int i = 0; i < numberOfPartitions; i++) {
            int start = i * size;
            int end = Math.min(start + size, list.size());
            partitions.add(new ArrayList<>(list.subList(start, end)));
        }
        return partitions;
    }
}
