package com.jins.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jins.permission.domain.entity.Role;
import org.apache.ibatis.annotations.Mapper;

import java.util.Optional;

/**
 * 角色数据访问仓库
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    /**
     * 根据角色代码查找角色
     * @param roleCode 角色代码
     * @return 角色对象
     */
    Optional<Role> findByRoleCode(String roleCode);
    
    /**
     * 检查角色代码是否存在
     * @param roleCode 角色代码
     * @return 是否存在
     */
    boolean existsByRoleCode(String roleCode);
}