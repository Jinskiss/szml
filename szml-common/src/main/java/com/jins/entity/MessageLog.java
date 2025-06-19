package com.jins.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 用户id
     */
    private Long userId;      // 用户ID

    /**
     * 操作类型，如 "update_user"
     */
    private String action;    // 操作类型

    /**
     * 操作IP地址
     */
    private String ip;        // 操作IP

    /**
     * 操作详情
     */
    private String detail;    // 操作详情（JSON格式）
}