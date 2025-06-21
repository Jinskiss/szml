package com.jins.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.jins.user.domain.entity.User;
import com.jins.user.domain.form.LoginForm;
import com.jins.user.domain.form.RegistForm;
import com.jins.user.domain.vo.UserVO;

public interface UserService extends IService<User> {
    /**
     * 注册用户
     * @param registForm
     * @return
     */
    User register(RegistForm registForm);

    /**
     * 登录
     * @param request
     * @return
     */
    String login(LoginForm request);

    /**
     * 通过userId查询用户信息
     * @param page
     * @param rows
     * @param user
     * @return
     */
    Page<User> pageList(int page, int rows, User user);

    /**
     * 通过userId查询用户信息
     * @param user
     * @param queryUserId
     * @return
     */
    UserVO getUserInfo(User user, Long queryUserId);

    /**
     * 重置密码
     * @param user
     * @param newPassword
     */
    void resetPassword(User user, String newPassword);

    /**
     * 通过userId修改用户信息
     * @param user
     * @param updateUser
     */
    void updateUser(User user, User updateUser);

    /**
     * 将普通用户升级为管理员
     * @param user
     * @param userId
     */
    void upgradeToAdmin(User user, Long userId);

    /**
     * 将管理员降级为普通用户
     * @param user
     * @param userId
     */
    void downgradeToUser(User user, Long userId);
}
