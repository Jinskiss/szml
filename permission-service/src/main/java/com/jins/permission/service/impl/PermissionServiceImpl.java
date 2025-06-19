package com.jins.permission.service.impl;

import com.jins.permission.domain.entity.Role;
import com.jins.permission.domain.entity.UserRole;
import com.jins.permission.mapper.RoleMapper;
import com.jins.permission.mapper.UserRoleMapper;
import com.jins.permission.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.management.relation.RoleNotFoundException;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    @Override
    public void bindDefaultRole(Long userId) {

    }

    @Override
    public String getUserRoleCode(Long userId) {
        return "";
    }

    @Override
    public void upgradeToAdmin(Long userId) {

    }

    @Override
    public void downgradeToUser(Long userId) {

    }

    @Override
    public boolean hasRole(Long userId, String requiredRole) {
        return false;
    }
}