package com.jins.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jins.user.domain.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}
