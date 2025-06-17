package com.jins.controller;

import com.jins.common.R;
import com.jins.domain.entity.User;
import com.jins.domain.form.LoginForm;
import com.jins.domain.form.RegistForm;
import com.jins.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 注册
     * @param registForm 注册信息表单
     * @return 用户信息StringUtils
     */
    @PostMapping("/register")
    public R<User> register(@Validated @RequestBody RegistForm registForm) {
        if (registForm.getPassword() == null || registForm.getPassword() == "") {
            registForm.setPassword("123456");
        }
        return R.success(userService.register(registForm));
    }

    /**
     * 登录
     * @param request
     * @return
     */
    @PostMapping("/login")
    public R<User> login(@RequestBody LoginForm request) {
        String token = userService.login(request);
        return R.success(token);
    }

}