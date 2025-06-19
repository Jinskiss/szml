package com.jins.permission.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-角色关系实体
 */
@TableName("user_roles")
@Data
public class UserRole {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（来自用户服务）
     */
    private Long userId;

    /**
     * 角色ID（关联roles表）
     */
    private Long roleId;
}