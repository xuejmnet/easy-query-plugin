package com.easy.query.plugin.util;

import com.alibaba.druid.DbType;

/**
 * create time 2025/11/24 09:14
 * 文件说明
 *
 * @author xuejiaming
 */
public class DbTypeGuesserUtil {
    public static String guess(String sql) {
        if (sql == null) {
            return "generic";
        }
        String s = sql.trim().toLowerCase();

        // ---------- 第一优先级：根据引用风格判断 ----------
        // MySQL: 反引号 `id`
        if (s.contains("`")) {
            return DbType.mysql.name();
        }

        // SQL Server: 方括号 [id]
        if (s.contains("[") && s.contains("]")) {
            return DbType.sqlserver.name();
        }

        // PostgreSQL / SQLite / Oracle: "id"
        if (s.contains("\"")) {
            if (s.contains(" dual ")) {
                return DbType.oracle.name();
            }
            return DbType.postgresql.name(); // 默认优先 psql
        }


        if (s.contains(" nvl(")) {
            return DbType.oracle.name();
        }

        if (s.contains(" fetch first ")) {
            return DbType.db2.name();
        }


        return "generic";
    }
}
