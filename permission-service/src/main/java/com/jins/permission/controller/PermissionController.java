package com.jins.permission.controller;

import com.jins.permission.common.R;
import com.jins.permission.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 权限服务RPC接口控制器
 */
@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * 绑定默认角色（普通用户）
     */
    @PostMapping("/bindDefaultRole")
    public R bindDefaultRole(@RequestParam Long userId) {
        permissionService.bindDefaultRole(userId);

        return R.success("角色绑定成功");
    }

    /**
     * 查询用户角色代码
     */
    @GetMapping("/getRoleCode")
    public R<String> getUserRoleCode(@RequestParam Long userId) {
        String roleCode = permissionService.getUserRoleCode(userId);

        return R.success(roleCode);
    }

    /**
     * 升级用户为管理员（仅限超管操作）
     */
    @PostMapping("/upgradeToAdmin")
    public R upgradeToAdmin(@RequestParam Long userId) {
        // TODO
        // 判断是否为超管

        permissionService.upgradeToAdmin(userId);

        return R.success("升级管理员成功");
    }

    /**
     * 降级用户为普通用户（仅限超管操作）
     */
    @PostMapping("/downgradeToUser")
    public R downgradeToUser(@RequestParam Long userId) {
        // TODO
        // 判断是否为超管

        permissionService.downgradeToUser(userId);

        return R.success("降级用户成功");
    }
}