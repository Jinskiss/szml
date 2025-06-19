package com.jins.logging.listener;

import com.jins.entity.MessageLog;
import com.jins.logging.domain.entity.OperationLog;
import com.jins.logging.mapper.LogMapper;
import com.jins.logging.service.LogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogMessageListener {

    private final LogService logService;
    private final LogMapper logMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "fanout.queue", durable = "true"),
            exchange = @Exchange(name = "szml.fanout")
    ))
    public void handleLogMessage(MessageLog messageLog) {
        // 转换事件对象为实体对象
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(messageLog.getUserId());
        operationLog.setAction(messageLog.getAction());
        operationLog.setIp(messageLog.getIp());
        operationLog.setDetail(messageLog.getDetail());

        // 保存日志
        logMapper.insert(operationLog);

        log.info("日志保存成功: ID={}, Action={}", operationLog.getLogId(), operationLog.getAction());
    }
}