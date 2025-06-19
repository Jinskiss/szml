package com.jins.permission.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色实体类
 */
@TableName("roles")
@Data
public class Role {
    private static final long serialVersionUID = 1L;

    /**
     * 角色ID
     * 1: 普通用户
     * 2: 管理员
     * 3: 超级管理员
     */
    @TableId(type = IdType.AUTO)
    private Long roleId;

    /**
     * 角色代码（唯一标识）
     * user - 普通用户
     * admin - 管理员
     * super_admin - 超级管理员
     */
    private String roleCode;
}