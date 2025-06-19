package com.jins.logging.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jins.logging.domain.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色数据访问层
 */
@Mapper
public interface LogMapper extends BaseMapper<OperationLog> {

}