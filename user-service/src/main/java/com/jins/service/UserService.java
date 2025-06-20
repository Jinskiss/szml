package com.jins.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jins.domain.entity.User;
import com.jins.domain.form.LoginForm;
import com.jins.domain.form.RegistForm;

public interface UserService extends IService<User> {
    /**
     * 注册用户
     * @param registForm
     * @return
     */
    User register(RegistForm registForm);

    String login(LoginForm request);

    Page<User> pageList(int page, int rows, User user);
}
