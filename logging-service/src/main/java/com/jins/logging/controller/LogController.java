package com.jins.logging.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jins.common.R;
import com.jins.constants.Status;
import com.jins.logging.domain.entity.OperationLog;
import com.jins.logging.mapper.LogMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
@Api(tags = "日志管理")
public class LogController {

    private final LogMapper logMapper;

    /**
     * 通过logId分页查询日志信息
     * @param logId
     * @return
     */
    @ApiOperation("通过logId分页查询日志信息")
    @GetMapping("/{logId}")
    public R<OperationLog> getLogById(@PathVariable Long logId) {
        log.info("通过logId分页查询日志信息");

        OperationLog operationLog = logMapper.selectById(logId);

        if (operationLog == null) {
            log.error("日志不存在，logId: {}", logId);
            return R.error(Status.CODE_404, "日志不存在");
        }

        return R.success(operationLog);
    }

    /**
     * 通过userId分页查询日志信息
     * @param userId
     * @param page
     * @param size
     * @return
     */
    @ApiOperation("通过userId分页查询日志信息")
    @GetMapping("/user/{userId}")
    public R<Page<OperationLog>> getUserLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("通过userId分页查询日志信息");
        
        LambdaQueryWrapper<OperationLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(OperationLog::getUserId, userId);
        
        Page<OperationLog> logPage = logMapper.selectPage(new Page<>(page, size), queryWrapper);

        return R.success(logPage);
    }

    /**
     * 查看全部日志消息
     * @param page
     * @param size
     * @return
     */
    @ApiOperation("查看全部日志消息")
    @GetMapping("/recent")
    public R<Page<OperationLog>> getRecentLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("查看全部日志消息");

        Page<OperationLog> logPage = logMapper.selectPage(new Page<>(page, size), null);

        return R.success(logPage);
    }
}