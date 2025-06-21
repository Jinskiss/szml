package com.jins.user.domain.form;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 用户登录表单
 *
 * @author: sunny
 * @date: 2023-11-10
 */
@Data
public class LoginForm implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户名
     */
    @NotNull(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     */
    @NotNull(message = "密码不能为空")
    private String password;

}
