package com.jins.logging.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("operation_logs")
public class OperationLog {
    private static final long serialVersionUID = 1L;

    /**
     * 日志id
     */
    @TableId(type = IdType.AUTO)
    private Long logId;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 操作类型，如 "update_user"
     */
    private String action;

    /**
     * 操作IP地址
     */
    private String ip;

    /**
     * 操作详情
     */
    private String detail;
}