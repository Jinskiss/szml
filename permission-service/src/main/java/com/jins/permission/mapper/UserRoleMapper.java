package com.jins.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jins.permission.domain.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/**
 * 用户-角色关系数据访问仓库
 */
@Mapper
public interface UserRoleMapper extends BaseMapper<UserRole> {

    /**
     * 根据用户ID查找用户角色关系
     * @param userId 用户ID
     * @return 用户角色关系对象
     */
    Optional<UserRole> findByUserId(Long userId);
    
    /**
     * 检查用户是否已分配角色
     * @param userId 用户ID
     * @return 是否已分配
     */
    boolean existsByUserId(Long userId);
    
    /**
     * 根据用户ID删除角色关系
     * @param userId 用户ID
     */
    void deleteByUserId(Long userId);
}