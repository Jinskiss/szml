package com.jins.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jins.client.PermissionClient;
import com.jins.common.R;
import com.jins.constants.RedisConstants;
import com.jins.constants.RoleConstants;
import com.jins.constants.Status;
import com.jins.domain.entity.User;
import com.jins.domain.form.LoginForm;
import com.jins.domain.form.RegistForm;
import com.jins.domain.vo.UserVO;
import com.jins.entity.MessageLog;
import com.jins.exception.BizException;
import com.jins.mapper.UserMapper;
import com.jins.service.UserService;
import com.jins.utils.TokenUtils;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final RedisTemplate redisTemplate;

    private final PermissionClient permissionClient;

    private final RabbitTemplate rabbitTemplate;
    private final UserMapper userMapper;

    @Override
    //@Transactional(rollbackFor = Exception.class)
    @GlobalTransactional
    public User register(RegistForm registForm) {
        //查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registForm.getUsername());
        User user = getOne(queryWrapper);

        //用户已存在
        if (user != null) {
            throw new BizException(Status.CODE_403, "用户名已被使用");
        }

        user = new User();
        user.setUsername(registForm.getUsername());
        user.setPassword(registForm.getPassword());
        user.setEmail(registForm.getEmail());
        user.setPhone(registForm.getPhone());
        user.setGmtCreate(new Date());
        save(user);

        // rpc用户角色绑定
        permissionClient.bindDefaultRole(user.getUserId());

        //int x = 5 / 0;

        // rabbitmq记录日志
        sendRegisterLog(user, registForm);

        return user;
    }

    /**
     * 发送用户注册日志消息
     */
    private void sendRegisterLog(User user, RegistForm registForm) {
        // 获取客户端IP
        String clientIp = getClientIp();

        // 构建详细信息
        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("username", registForm.getUsername());
        detailMap.put("email", registForm.getEmail());
        detailMap.put("phone", registForm.getPhone());
        detailMap.put("registerTime", new Date());

        // 创建日志消息
        MessageLog messageLog = new MessageLog();
        messageLog.setUserId(user.getUserId());
        messageLog.setAction("user_register");
        messageLog.setIp(clientIp);
        messageLog.setDetail(JSONObject.toJSONString(detailMap));

        // 发送消息
        rabbitTemplate.convertAndSend("szml.fanout", "", messageLog);
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        String xffHeader = request.getHeader("X-Forwarded-For");
        if (xffHeader == null) {
            return request.getRemoteAddr();
        }
        return xffHeader.split(",")[0];
    }

    /**
     * 登录
     *
     * @param loginForm 登录表单
     * @return 用户信息
     */
    @Override
    public String login(LoginForm loginForm) {
        //查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(StringUtils.isNotBlank(loginForm.getUsername()), User::getUsername, loginForm.getUsername())
                .eq(StringUtils.isNotBlank(loginForm.getPassword()), User::getPassword, loginForm.getPassword());
        User user = getOne(queryWrapper);

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

    /**
     * 用户分页查询
     * @param page
     * @param rows
     * @param user
     * @return
     */
    @Override
    public Page<User> pageList(int page, int rows, User user) {
        R<String> result = permissionClient.getUserRoleCode(user.getUserId());

        if (result == null || !Objects.equals(result.getCode(), Status.CODE_200) || result.getData() == null) {
            // 处理错误情况
            throw new BizException(Status.CODE_500, "获取角色码失败");
        }

        String userRoleCode = result.getData();

        Page<User> pageList = new Page<>(page, rows);
        if (Objects.equals(userRoleCode, RoleConstants.USER_ROLE)) {
            List<User> userList = new ArrayList<>();
            userList.add(user);
            pageList.setRecords(userList);
            pageList.setTotal(1);
        } else if (Objects.equals(userRoleCode, RoleConstants.ADMIN_ROLE)) {
            R<List<Long>> result1 = permissionClient.getUserIdByRoleCode(RoleConstants.USER_ROLE);

            if (result1 == null || !Objects.equals(result1.getCode(), Status.CODE_200)) {
                // 处理错误情况
                throw new BizException(Status.CODE_500, "获取普通用户失败");
            }

            List<Long> userIdList = result1.getData();

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(User::getUserId, userIdList);
            List<User> userList = userMapper.selectList(queryWrapper);

            pageList.setRecords(userList);
            pageList.setTotal(userList.size());
        } else {
            R<List<Long>> result1 = permissionClient.getUserIdByRoleCode(RoleConstants.USER_ROLE);
            if (result1 == null || !Objects.equals(result1.getCode(), Status.CODE_200)) {
                // 处理错误情况
                throw new BizException(Status.CODE_500, "获取普通用户失败");
            }
            R<List<Long>> result2 = permissionClient.getUserIdByRoleCode(RoleConstants.ADMIN_ROLE);
            if (result2 == null || !Objects.equals(result2.getCode(), Status.CODE_200)) {
                // 处理错误情况
                throw new BizException(Status.CODE_500, "获取管理员用户失败");
            }
            R<List<Long>> result3 = permissionClient.getUserIdByRoleCode(RoleConstants.SUPER_ADMIN_ROLE);
            if (result3 == null || !Objects.equals(result3.getCode(), Status.CODE_200)) {
                // 处理错误情况
                throw new BizException(Status.CODE_500, "获取超级管理员用户失败");
            }

            List<Long> userIdList1 = result1.getData();
            List<Long> userIdList2 = result2.getData();
            List<Long> userIdList3 = result3.getData();

            List<Long> userIdList = new ArrayList<>();
            userIdList.addAll(userIdList1);
            userIdList.addAll(userIdList2);
            userIdList.addAll(userIdList3);

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper
                    .in(User::getUserId, userIdList);

            List<User> userList = userMapper.selectList(queryWrapper);

            pageList.setRecords(userList);
            pageList.setTotal(userList.size());
        }

        return pageList;
    }

    /**
     * 通过userId查询用户信息
     * @param user
     * @param queryUserId
     * @return
     */
    @Override
    public UserVO getUserInfo(User user, Long queryUserId) {
        R<String> result = permissionClient.getUserRoleCode(user.getUserId());

        if (result == null || !Objects.equals(result.getCode(), Status.CODE_200) || result.getData() == null) {
            // 处理错误情况
            throw new BizException(Status.CODE_500, "获取角色码失败");
        }

        String userRoleCode = result.getData();

        UserVO userVO = new UserVO();
        if (Objects.equals(userRoleCode, RoleConstants.USER_ROLE)) {
            if (!user.getUserId().equals(queryUserId)) {
                throw new BizException(Status.CODE_500, "用户权限不足");
            }

            userVO.setUserId(user.getUserId());
            userVO.setUsername(user.getUsername());
            userVO.setEmail(user.getEmail());
            userVO.setPhone(user.getPhone());
        } else if (Objects.equals(userRoleCode, RoleConstants.ADMIN_ROLE)) {
            R<String> result1 = permissionClient.getUserRoleCode(queryUserId);

            if (result1 == null || !Objects.equals(result1.getCode(), Status.CODE_200) || result1.getData() == null) {
                // 处理错误情况
                throw new BizException(Status.CODE_500, "获取用户角色码失败");
            }

            String queryUserRoleCode = result1.getData();

            if (Objects.equals(queryUserRoleCode, RoleConstants.USER_ROLE)) {
                User userInfo = getById(queryUserId);

                userVO.setUserId(userInfo.getUserId());
                userVO.setUsername(userInfo.getUsername());
                userVO.setEmail(userInfo.getEmail());
                userVO.setPhone(userInfo.getPhone());
            } else {
                throw new BizException(Status.CODE_500, "获用户权限不足");
            }
        } else {
            User userInfo = getById(queryUserId);

            userVO.setUserId(userInfo.getUserId());
            userVO.setUsername(userInfo.getUsername());
            userVO.setEmail(userInfo.getEmail());
            userVO.setPhone(userInfo.getPhone());
        }

        return userVO;
    }

    /**
     * 修改用户信息
     * @param user
     * @param updateUser
     */
    @Override
    public void updateUser(User user, User updateUser) {
        R<String> result = permissionClient.getUserRoleCode(user.getUserId());

        if (result == null || !Objects.equals(result.getCode(), Status.CODE_200) || result.getData() == null) {
            // 处理错误情况
            throw new BizException(Status.CODE_500, "获取角色码失败");
        }

        String userRoleCode = result.getData();

        if (Objects.equals(userRoleCode, RoleConstants.USER_ROLE)) {
            if (!user.getUserId().equals(updateUser.getUserId())) {
                throw new BizException(Status.CODE_500, "用户权限不足");
            }

            //查询用户名是否存在
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, updateUser.getUsername());
            User userTemp = getOne(queryWrapper);
            if (userTemp != null) {
                throw new BizException(Status.CODE_500, "用户名已存在");
            }

            User userInfo = new User();
            userInfo.setUserId(updateUser.getUserId());
            userInfo.setUsername(updateUser.getUsername());
            userInfo.setPassword(updateUser.getPassword());
            userInfo.setEmail(updateUser.getEmail());
            userInfo.setPhone(updateUser.getPhone());

            userMapper.updateById(userInfo);
        } else if (Objects.equals(userRoleCode, RoleConstants.ADMIN_ROLE)) {
            R<String> result1 = permissionClient.getUserRoleCode(updateUser.getUserId());

            if (result1 == null || !Objects.equals(result1.getCode(), Status.CODE_200) || result1.getData() == null) {
                // 处理错误情况
                throw new BizException(Status.CODE_500, "获取用户角色码失败");
            }

            //查询用户名是否存在
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, updateUser.getUsername());
            User userTemp = getOne(queryWrapper);
            if (userTemp != null) {
                throw new BizException(Status.CODE_500, "用户名已存在");
            }

            String queryUserRoleCode = result1.getData();

            if (Objects.equals(queryUserRoleCode, RoleConstants.USER_ROLE)) {
                User userInfo = new User();
                userInfo.setUserId(updateUser.getUserId());
                userInfo.setUsername(updateUser.getUsername());
                userInfo.setPassword(updateUser.getPassword());
                userInfo.setEmail(updateUser.getEmail());
                userInfo.setPhone(updateUser.getPhone());

                userMapper.updateById(userInfo);
            } else {
                throw new BizException(Status.CODE_500, "获用户权限不足");
            }
        } else {
            //查询用户名是否存在
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, updateUser.getUsername());
            User userTemp = getOne(queryWrapper);
            if (userTemp != null) {
                throw new BizException(Status.CODE_500, "用户名已存在");
            }

            User userInfo = getById(updateUser.getUserId());

            userInfo.setUserId(updateUser.getUserId());
            userInfo.setUsername(updateUser.getUsername());
            userInfo.setPassword(updateUser.getPassword());
            userInfo.setEmail(updateUser.getEmail());
            userInfo.setPhone(updateUser.getPhone());

            userMapper.updateById(userInfo);
        }
    }

    /**
     * 重置密码
     * @param user
     * @param newPassword
     */
    @Override
    public void resetPassword(User user, String newPassword) {
        R<String> result = permissionClient.getUserRoleCode(user.getUserId());

        if (result == null || !Objects.equals(result.getCode(), Status.CODE_200) || result.getData() == null) {
            // 处理错误情况
            throw new BizException(Status.CODE_500, "获取角色码失败");
        }

        String userRoleCode = result.getData();

        if (Objects.equals(userRoleCode, RoleConstants.USER_ROLE)) {
            user.setPassword(newPassword);
            userMapper.updateById(user);
        } else if (Objects.equals(userRoleCode, RoleConstants.ADMIN_ROLE)) {
            R<List<Long>> result1 = permissionClient.getUserIdByRoleCode(RoleConstants.USER_ROLE);

            if (result1 == null || !Objects.equals(result1.getCode(), Status.CODE_200)) {
                // 处理错误情况
                throw new BizException(Status.CODE_500, "获取普通用户失败");
            }

            List<Long> userIdList = result1.getData();

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(User::getUserId, userIdList);
            List<User> userList = userMapper.selectList(queryWrapper);

            for (User user2 : userList) {
                user2.setPassword(newPassword);

                userMapper.updateById(user2);
            }
        } else {
            R<List<Long>> result1 = permissionClient.getUserIdByRoleCode(RoleConstants.USER_ROLE);
            if (result1 == null || !Objects.equals(result1.getCode(), Status.CODE_200)) {
                // 处理错误情况
                throw new BizException(Status.CODE_500, "获取普通用户失败");
            }
            R<List<Long>> result2 = permissionClient.getUserIdByRoleCode(RoleConstants.ADMIN_ROLE);
            if (result2 == null || !Objects.equals(result2.getCode(), Status.CODE_200)) {
                // 处理错误情况
                throw new BizException(Status.CODE_500, "获取管理员用户失败");
            }
            R<List<Long>> result3 = permissionClient.getUserIdByRoleCode(RoleConstants.SUPER_ADMIN_ROLE);
            if (result3 == null || !Objects.equals(result3.getCode(), Status.CODE_200)) {
                // 处理错误情况
                throw new BizException(Status.CODE_500, "获取超级管理员用户失败");
            }

            List<Long> userIdList1 = result1.getData();
            List<Long> userIdList2 = result2.getData();
            List<Long> userIdList3 = result3.getData();

            List<Long> userIdList = new ArrayList<>();
            userIdList.addAll(userIdList1);
            userIdList.addAll(userIdList2);
            userIdList.addAll(userIdList3);

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper
                    .in(User::getUserId, userIdList);

            List<User> userList = userMapper.selectList(queryWrapper);

            for (User user2 : userList) {
                user2.setPassword(newPassword);

                userMapper.updateById(user2);
            }
        }
    }
}
