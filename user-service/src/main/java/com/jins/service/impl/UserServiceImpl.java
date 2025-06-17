package com.jins.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jins.constants.RedisConstants;
import com.jins.constants.Status;
import com.jins.domain.entity.User;
import com.jins.domain.form.LoginForm;
import com.jins.domain.form.RegistForm;
import com.jins.domain.vo.UserVO;
import com.jins.exception.BizException;
import com.jins.mapper.UserMapper;
import com.jins.service.UserService;
import com.jins.utils.TokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private final RedisTemplate redisTemplate;

    @Override
    public User register(RegistForm registForm) {
        //查询用户
        User user = lambdaQuery()
                .eq(User::getUsername, registForm.getUsername())
                .one();

        //用户已存在
        if (user != null) {
            throw new BizException(Status.CODE_403, "用户名已被使用");
        }

        user = new User();
        Long p = new Random().nextLong(9999);
        user.setUserId(p);
        user.setUsername(registForm.getUsername());
        // TODO
        user.setPassword("123456");
        user.setEmail(registForm.getEmail());
        user.setPhone(registForm.getPhone());
        user.setGmtCreate(new Date());
        save(user);

        return user;
    }

    /**
     * 登录
     *
     * @param loginForm 登录表单
     * @return 用户信息
     */
    @Override
    public String login(LoginForm loginForm) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(StringUtils.isNotBlank(loginForm.getUsername()), User::getUsername, loginForm.getUsername())
                .eq(StringUtils.isNotBlank(loginForm.getPassword()), User::getPassword, loginForm.getPassword());

        //查询用户
        User user = lambdaQuery()
                .eq(User::getUsername, loginForm.getUsername())
                .eq(User::getPassword, loginForm.getPassword())
                .one();

        //用户不存在
        if (user == null) {
            throw new BizException(Status.CODE_403, "用户名或密码错误");
        }

        //生成token
        String token = TokenUtils.genToken(user.getUserId().toString(), user.getUsername());

        //把用户存到redis中
        redisTemplate.opsForValue().set(RedisConstants.USER_TOKEN_KEY + token, user);

        //jwt不设置过期时间，只设置redis过期时间。
        redisTemplate.expire(RedisConstants.USER_TOKEN_KEY + token, RedisConstants.USER_TOKEN_TTL, TimeUnit.MINUTES);

        return token;
    }
}
