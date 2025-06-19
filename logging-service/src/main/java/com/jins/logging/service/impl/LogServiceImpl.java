package com.jins.logging.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jins.logging.domain.entity.OperationLog;
import com.jins.logging.mapper.LogMapper;
import com.jins.logging.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogServiceImpl extends ServiceImpl<LogMapper, OperationLog> implements LogService {

}