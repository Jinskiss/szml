package com.jins.logging.listener;

import com.jins.constants.MQConstants;
import com.jins.entity.MessageLog;
import com.jins.logging.domain.entity.OperationLog;
import com.jins.logging.mapper.LogMapper;
import com.jins.logging.service.LogService;
import io.seata.spring.annotation.GlobalLock;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogMessageListener {

    private final LogService logService;
    private final LogMapper logMapper;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = MQConstants.QUEUE_FANOUT_NAME, durable = "true"),
            exchange = @Exchange(name = MQConstants.EXCHANGE_FANOUT_NAME)
    ))
    @GlobalLock
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