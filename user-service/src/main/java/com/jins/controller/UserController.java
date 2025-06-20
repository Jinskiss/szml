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
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 注册
     * @param registForm
     * @return 用户信息
     */
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
    @GetMapping("/user/{userId}")
    public R<UserVO> getUserInfo(@PathVariable Long userId) {
        User user = userService.getById(userId);

        UserVO userVO = new UserVO();
        userVO.setUserId(user.getUserId());
        userVO.setUsername(user.getUsername());
        userVO.setEmail(user.getEmail());
        userVO.setPhone(user.getPhone());

        return R.success(userVO);
    }


//    @PutMapping("/{userId}")
//    public ResponseEntity<UserResponse> updateUser(
//            @PathVariable Long userId,
//            @RequestBody UserResponse request,
//            @RequestHeader("Authorization") String token) {
//        UserResponse response = userService.updateUser(userId, request, token);
//        return ResponseEntity.ok(response);
//    }
//
//    @PostMapping("/reset-password")
//    public ResponseEntity<Void> resetPassword(
//            @RequestParam Long userId,
//            @RequestParam String newPassword,
//            @RequestHeader("Authorization") String token) {
//        userService.resetPassword(userId, newPassword, token);
//        return ResponseEntity.ok().build();
//    }

}