package com.jins.permission.service;

/**
 * 权限服务接口
 */
public interface PermissionService {

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

    /**
     * 检查用户是否拥有指定角色
     * @param userId 用户ID
     * @param requiredRole 需要检查的角色代码
     * @return 是否拥有
     */
    boolean hasRole(Long userId, String requiredRole);
}