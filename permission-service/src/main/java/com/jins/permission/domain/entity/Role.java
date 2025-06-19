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

    /**
     * 角色ID
     * 1: 超级管理员
     * 2: 普通用户
     * 3: 管理员
     */
    @TableId(type = IdType.AUTO)
    private Integer roleId;

    /**
     * 角色代码（唯一标识）
     * super_admin - 超级管理员
     * user - 普通用户
     * admin - 管理员
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色描述
     */
    private String description;
}