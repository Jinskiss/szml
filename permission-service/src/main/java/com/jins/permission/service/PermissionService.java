package com.jins.permission.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jins.permission.domain.entity.Role;

/**
 * 权限服务接口
 */
public interface PermissionService extends IService<Role> {

    /**
     * 为用户绑定默认角色（普通用户）
     * @param userId 用户ID
     */
    void bindDefaultRole(Long userId);

    /**
     * 查询用户的角色代码
     * @param userId 用户ID
     * @return 角色代码
     */
    String getUserRoleCode(Long userId);

    /**
     * 将用户升级为管理员（仅限超管操作）
     * @param userId 用户ID
     */
    void upgradeToAdmin(Long userId);

    /**
     * 将用户降级为普通用户（仅限超管操作）
     * @param userId 用户ID
     */
    void downgradeToUser(Long userId);
}