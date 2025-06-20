package com.jins.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jins.common.R;
import com.jins.constants.Status;
import com.jins.domain.entity.User;
import com.jins.domain.form.LoginForm;
import com.jins.domain.form.RegistForm;
import com.jins.domain.vo.UserVO;
import com.jins.service.UserService;
import com.jins.utils.UserHolder;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Api(tags = "用户管理", description = "提供用户信息的接口")
public class UserController {

    private final UserService userService;

    /**
     * 注册
     * @param registForm
     * @return 用户信息
     */
    @ApiOperation("用户登录接口")
    @PostMapping("/user/register")
    public R<User> register(@Validated @RequestBody RegistForm registForm) {
        if (registForm.getPassword() == null || registForm.getPassword().isEmpty()) {
            registForm.setPassword("123456");
        }

        return R.success(userService.register(registForm));
    }

    /**
     * 登录
     * @param request
     * @return
     */
    @ApiOperation("用户注册接口")
    @PostMapping("/user/login")
    public R<String> login(@RequestBody LoginForm request) {
        String token = userService.login(request);

        return R.success(token);
    }

    /**
     * 分页查询用户列表
     * @param page
     * @param rows
     * @return
     */
    @ApiOperation("分页查询用户列表")
    @GetMapping("/users")
    public R<Page<UserVO>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int rows) {
        User user1 = UserHolder.getUser();

        if (user1 == null) {
            return R.error(Status.CODE_500, "用户未登录");
        }

        Page<User> userPage = userService.pageList(page, rows, user1);

        List<UserVO> userVOList = new ArrayList<>();
        if (!userPage.getRecords().isEmpty()) {
            for (User user : userPage.getRecords()) {
                UserVO userVO = new UserVO();
                userVO.setUserId(user.getUserId());
                userVO.setUsername(user.getUsername());
                userVO.setEmail(user.getEmail());
                userVO.setPhone(user.getPhone());
                userVOList.add(userVO);
            }
        }

        Page<UserVO> userVOPage = new Page<>(page, rows);
        userVOPage.setRecords(userVOList);
        userVOPage.setTotal(userPage.getTotal());
        userVOPage.setCurrent(userPage.getCurrent());

        return R.success(userVOPage);
    }

    /**
     * 通过userId查询用户信息
     * @param userId
     * @return
     */
    @ApiOperation("通过userId查询用户信息")
    @GetMapping("/user/{userId}")
    public R<UserVO> getUserInfo(@PathVariable Long userId) {
        User user = UserHolder.getUser();

        if (user == null) {
            return R.error(Status.CODE_500, "用户未登录");
        }

        UserVO userVO = userService.getUserInfo(user, userId);

        return R.success(userVO);
    }

    /**
     * 修改用户信息
     * @param updateUser
     * @return
     */
    @ApiOperation("修改用户信息")
    @PutMapping("/updateUser")
    public R updateUser(@RequestBody User updateUser) {
        User user = UserHolder.getUser();

        if (user == null) {
            return R.error(Status.CODE_500, "用户未登录");
        }

        userService.updateUser(user, updateUser);

        return R.success();
    }

    /**
     * 重置密码
     * @param newPassword
     * @return
     */
    @ApiOperation("重置密码")
    @PostMapping("/reset-password")
    public R resetPassword(
            @RequestParam String newPassword) {
        User user = UserHolder.getUser();

        if (user == null) {
            return R.error(Status.CODE_500, "用户未登录");
        }

        userService.resetPassword(user, newPassword);

        return R.success();
    }

    /**
     * 将普通用户升级为管理员
     * @param userId
     * @return
     */
    @ApiOperation("将普通用户升级为管理员")
    @PostMapping("/{userId}/upgrade")
    public R upgradeToAdmin(@PathVariable Long userId) {
        User user = UserHolder.getUser();

        if (user == null) {
            return R.error(Status.CODE_500, "用户未登录");
        }

        userService.upgradeToAdmin(user, userId);

        return R.success();
    }

    /**
     * 将管理员降级为普通用户
     * @param userId
     * @return
     */
    @ApiOperation("将管理员降级为普通用户")
    @PostMapping("/{userId}/downgrade")
    public R downgradeToUser(@PathVariable Long userId) {
        User user = UserHolder.getUser();

        if (user == null) {
            return R.error(Status.CODE_500, "用户未登录");
        }

        userService.downgradeToUser(user, userId);

        return R.success();
    }
}