package com.jins.handler;

import com.jins.common.R;
import com.jins.constants.Status;
import com.jins.exception.BizException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 * <p>
 * 处理接口中抛出的业务异常 {@link BizException}，返回 {@link R} 结构
 * 全局异常，可以统一异常的处理，不需要每个地方都写相关处理逻辑
 */
@RestControllerAdvice
public class GlobalExceptionHandler {


    /**
     * 拦截业务异常
     *
     * @param e 业务异常
     * @return R
     */
    @ExceptionHandler(BizException.class)
    public R handle(BizException e) {
        return R.error(e.getCode(), e.getMessage());
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public R handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return R.error(Status.CODE_403, message);
    }

    /**
     * 其他异常
     * @param e
     * @return
     */
    @ExceptionHandler(Exception.class)
    public R handleAllException(Exception e) {
        return R.error(Status.CODE_500, "系统内部错误: " + e.getMessage());
    }
}
