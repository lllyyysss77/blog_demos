package com.bolingcavalry.vo;

import lombok.Data;

/**
 * ClickHouse数据库连接信息
 */
@Data
public class IndexConfig {
    /**
     * ClickHouse数据库连接地址
     */
    private String ckURL;

    /**
     * ClickHouse数据库表名
     */
    private String ckTableName;

    /**
     * ClickHouse数据库用户名
     */
    private String ckUsername;

    /**
     * ClickHouse数据库密码
     */
    private String ckPassword;

    /**
     * 索引文件路径
     */
    private String ragFilePath;
}
