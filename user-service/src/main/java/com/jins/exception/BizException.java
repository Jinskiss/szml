package com.jins.exception;


import com.jins.constants.Status;

/**
 * 业务异常
 * 在业务运行中，抛出的异常错误
 *
 * @author: sunny
 * @date: 2023-11-10
 */
public class BizException extends RuntimeException {

    /**
     * 错误码
     */
    private String code;

    public BizException(String code, String msg) {
        super(msg);
        this.code = code;
    }

    public BizException(String msg) {
        super(msg);
        this.code = Status.CODE_500;
    }

    public String getCode() {
        return code;
    }
}
