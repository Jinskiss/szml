package com.jins.logging.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jins.common.R;
import com.jins.constants.Status;
import com.jins.logging.domain.entity.OperationLog;
import com.jins.logging.mapper.LogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
        OperationLog log = logMapper.selectById(logId);

        if (log == null) {
            return R.error(Status.CODE_404, "日志不存在");
        }

        return R.success(log);
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
        Page<OperationLog> logPage = logMapper.selectPage(new Page<>(page, size), null);

        return R.success(logPage);
    }
}