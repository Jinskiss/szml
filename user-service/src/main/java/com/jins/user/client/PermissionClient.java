package com.jins.user.client;

import com.jins.common.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "permission-service", path = "/permission")
public interface PermissionClient {

    @PostMapping("/bindDefaultRole")
    R bindDefaultRole(@RequestParam("userId") Long userId);

    @GetMapping("/getRoleCode")
    R<String> getUserRoleCode(@RequestParam("userId") Long userId);

    @GetMapping("/getUserId")
    R<List<Long>> getUserIdByRoleCode(@RequestParam String roleCode);

    @PostMapping("/upgradeToAdmin")
    R<Void> upgradeToAdmin(@RequestParam("userId") Long userId);

    @PostMapping("/downgradeToUser")
    R<Void> downgradeToUser(@RequestParam("userId") Long userId);
}