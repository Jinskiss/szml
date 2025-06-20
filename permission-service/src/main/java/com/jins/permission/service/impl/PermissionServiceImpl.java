package com.jins.permission.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jins.constants.RoleConstants;
import com.jins.constants.Status;
import com.jins.permission.domain.entity.Role;
import com.jins.permission.domain.entity.UserRole;
import com.jins.permission.exception.BizException;
import com.jins.permission.mapper.RoleMapper;
import com.jins.permission.mapper.UserRoleMapper;
import com.jins.permission.service.PermissionService;
import io.seata.spring.annotation.GlobalLock;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionServiceImpl extends ServiceImpl<RoleMapper, Role> implements PermissionService {
    private final RoleMapper roleMapper;

    private final UserRoleMapper userRoleMapper;

    /**
     * 为用户绑定默认角色（普通用户）
     * @param userId 用户ID
     */
    @Override
    @GlobalLock
    public void bindDefaultRole(Long userId) {
        // 检查是否已绑定角色
        LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRole::getUserId, userId);
        if (userRoleMapper.selectCount(queryWrapper) == 1) {
            throw new BizException(Status.CODE_400, "用户已绑定角色");
        }

        // 获取普通用户角色
        LambdaQueryWrapper<Role> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(Role::getRoleCode, RoleConstants.USER_ROLE);
        Role role = roleMapper.selectOne(queryWrapper1);

        if (role == null) {
            throw new BizException(Status.CODE_404, "普通用户角色不存在");
        }

        // 创建用户角色关系
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getRoleId());

        if (userRoleMapper.insert(userRole) < 1) {
            throw new BizException(Status.CODE_500, "角色绑定失败");
        }
    }

    /**
     * 查询用户的角色代码
     * @param userId 用户ID
     * @return 角色代码
     */
    @Override
    public String getUserRoleCode(Long userId) {
        // 查询用户角色关系
        LambdaQueryWrapper<UserRole> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserRole::getUserId, userId);
        UserRole userRole = userRoleMapper.selectOne(queryWrapper);

        if (userRole == null) {
            throw new BizException(Status.CODE_404, "用户角色未分配");
        }

        // 查询角色信息
        Role role = roleMapper.selectById(userRole.getRoleId());
        if (role == null) {
            throw new BizException(Status.CODE_404, "角色不存在");
        }

        return role.getRoleCode();
    }

    /**
     * 将用户升级为管理员（仅限超管操作）
     * @param userId 用户ID
     */
    @Override
    public void upgradeToAdmin(Long userId) {
        // 获取管理员角色
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Role::getRoleCode, RoleConstants.ADMIN_ROLE);
        Role adminRole = roleMapper.selectOne(queryWrapper);

        if (adminRole == null) {
            throw new BizException(Status.CODE_404, "管理员角色不存在");
        }

        // 获取用户当前角色关系
        LambdaQueryWrapper<UserRole> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(UserRole::getUserId, userId);
        UserRole userRole = userRoleMapper.selectOne(queryWrapper1);

        if (userRole == null) {
            throw new BizException(Status.CODE_404, "用户角色未分配");
        }

        // 更新角色ID
        userRole.setRoleId(adminRole.getRoleId());

        if (userRoleMapper.updateById(userRole) < 1) {
            throw new BizException(Status.CODE_500, "升级管理员失败");
        }
    }

    /**
     * 将用户降级为普通用户（仅限超管操作）
     * @param userId 用户ID
     */
    @Override
    public void downgradeToUser(Long userId) {
        // 获取普通用户角色
        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Role::getRoleCode, RoleConstants.USER_ROLE);
        Role role = roleMapper.selectOne(queryWrapper);

        if (role == null) {
            throw new BizException(Status.CODE_404, "普通用户角色不存在");
        }

        // 获取用户当前角色关系
        LambdaQueryWrapper<UserRole> queryWrapper1 = new LambdaQueryWrapper<>();
        queryWrapper1.eq(UserRole::getUserId, userId);
        UserRole userRole = userRoleMapper.selectOne(queryWrapper1);

        if (userRole == null) {
            throw new BizException(Status.CODE_404, "用户角色未分配");
        }

        // 更新角色ID
        userRole.setRoleId(role.getRoleId());

        if (userRoleMapper.updateById(userRole) < 1) {
            throw new BizException(Status.CODE_500, "降级用户失败");
        }
    }
}