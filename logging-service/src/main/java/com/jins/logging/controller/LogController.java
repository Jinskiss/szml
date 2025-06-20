package com.jins.logging.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jins.common.R;
import com.jins.constants.Status;
import com.jins.logging.domain.entity.OperationLog;
import com.jins.logging.mapper.LogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogMapper logMapper;

    /**
     * 通过logId分页查询日志信息
     * @param logId
     * @return
     */
    @GetMapping("/{logId}")
    public R<OperationLog> getLogById(@PathVariable Long logId) {
        log.info("查询日志，logId: {}", logId);

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
    @GetMapping("/user/{userId}")
    public R<Page<OperationLog>> getUserLogs(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("查询用户日志，userId: {}, 页码: {}, 每页大小: {}", userId, page, size);
        
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
    @GetMapping("/recent")
    public R<Page<OperationLog>> getRecentLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("查询全部日志，页码: {}, 每页大小: {}", page, size);

        Page<OperationLog> logPage = logMapper.selectPage(new Page<>(page, size), null);

        return R.success(logPage);
    }
}