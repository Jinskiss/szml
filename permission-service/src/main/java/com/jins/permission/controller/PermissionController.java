package com.jins.permission.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jins.common.R;
import com.jins.permission.domain.entity.Role;
import com.jins.permission.domain.entity.UserRole;
import com.jins.permission.mapper.UserRoleMapper;
import com.jins.permission.service.PermissionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限服务RPC接口控制器
 */
@Slf4j
@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
@Api(tags = "权限管理", description = "提供权限信息的接口")
public class PermissionController {

    private final PermissionService permissionService;

    private final UserRoleMapper userRoleMapper;

    /**
     * 绑定默认角色（普通用户）
     */
    @ApiOperation("绑定默认角色（普通用户）")
    @PostMapping("/bindDefaultRole")
    @Transactional
    public R bindDefaultRole(@RequestParam Long userId) {
        log.info("绑定默认角色（普通用户）");

        permissionService.bindDefaultRole(userId);

        return R.success("角色绑定成功");
    }

    /**
     * 查询用户角色代码
     */
    @ApiOperation("查询用户角色代码")
    @GetMapping("/getRoleCode")
    public R<String> getUserRoleCode(@RequestParam Long userId) {
        log.info("查询用户角色代码");

        String roleCode = permissionService.getUserRoleCode(userId);

        return R.success(roleCode);
    }

    /**
     * 通过角色码查询用户id
     */
    @ApiOperation("通过角色码查询用户id")
    @GetMapping("/getUserId")
    public R<List<Long>> getUserIdByRoleCode(@RequestParam String roleCode) {
        log.info("通过角色码查询用户id");

        LambdaQueryWrapper<Role> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Role::getRoleCode, roleCode);
        Long roleId = permissionService.getOne(queryWrapper).getRoleId();

        LambdaQueryWrapper<UserRole> queryWrapper1 = new LambdaQueryWrapper();
        queryWrapper1.eq(UserRole::getRoleId, roleId);
        List<UserRole> userRoles = userRoleMapper.selectList(queryWrapper1);

        List<Long> userIdList = new ArrayList<>();
        for (UserRole userRole : userRoles) {
            userIdList.add(userRole.getUserId());
        }

        return R.success(userIdList);
    }

    /**
     * 升级用户为管理员（仅限超管操作）
     */
    @ApiOperation("升级用户为管理员（仅限超管操作）")
    @PostMapping("/upgradeToAdmin")
    public R upgradeToAdmin(@RequestParam Long userId) {
        log.info("升级用户为管理员（仅限超管操作）");

        permissionService.upgradeToAdmin(userId);

        return R.success("升级管理员成功");
    }

    /**
     * 降级用户为普通用户（仅限超管操作）
     */
    @ApiOperation("降级用户为普通用户（仅限超管操作）")
    @PostMapping("/downgradeToUser")
    public R downgradeToUser(@RequestParam Long userId) {
        log.info("降级用户为普通用户（仅限超管操作）");

        permissionService.downgradeToUser(userId);

        return R.success("降级用户成功");
    }
}