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
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
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
        log.info("用户注册，用户名: {}", registForm.getUsername());

        //查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, registForm.getUsername());
        User user = getOne(queryWrapper);

        //用户已存在
        if (user != null) {
            log.error("用户注册失败，用户名已存在: {}", registForm.getUsername());
            throw new BizException(Status.CODE_403, "用户名已存在");
        }

        user = new User();
        user.setUsername(registForm.getUsername());
        user.setPassword(registForm.getPassword());
        user.setEmail(registForm.getEmail());
        user.setPhone(registForm.getPhone());
        user.setGmtCreate(new Date());
        save(user);

        // rpc用户角色绑定
        log.info("绑定用户默认角色，用户ID: {}", user.getUserId());
        permissionClient.bindDefaultRole(user.getUserId());

        //int x = 5 / 0;

        // rabbitmq记录日志
        log.info("发送用户注册日志，用户ID: {}", user.getUserId());
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
    // 借鉴了ai
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
        log.info("用户登录，用户名: {}", loginForm.getUsername());

        //查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(StringUtils.isNotBlank(loginForm.getUsername()), User::getUsername, loginForm.getUsername())
                .eq(StringUtils.isNotBlank(loginForm.getPassword()), User::getPassword, loginForm.getPassword());
        User user = getOne(queryWrapper);

        //用户不存在
        if (user == null) {
            log.error("用户登录失败，用户名或密码错误，用户名: {}，密码：{}", loginForm.getUsername(), loginForm.getPassword());
            throw new BizException(Status.CODE_403, "用户名或密码错误");
        }

        //生成token
        String token = TokenUtils.genToken(user.getUserId().toString(), user.getUsername());
        log.info("用户登录成功，生成token，{}", token);

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
        log.info("用户分页查询，用户ID: {}, 页码: {}, 行数: {}", user.getUserId(), page, rows);

        R<String> result = permissionClient.getUserRoleCode(user.getUserId());

        if (result == null || !Objects.equals(result.getCode(), Status.CODE_200) || result.getData() == null) {
            log.error("获取用户角色码失败，用户ID: {}", user.getUserId());
            // 处理错误情况
            throw new BizException(Status.CODE_500, "获取角色码失败");
        }

        String userRoleCode = result.getData();

        Page<User> pageList = new Page<>(page, rows);
        if (Objects.equals(userRoleCode, RoleConstants.USER_ROLE)) {
            log.info("普通用户查询自己信息");

            List<User> userList = new ArrayList<>();
            userList.add(user);
            pageList.setRecords(userList);
            pageList.setTotal(1);
        } else if (Objects.equals(userRoleCode, RoleConstants.ADMIN_ROLE)) {
            log.info("管理员查询所有普通用户");

            R<List<Long>> result1 = permissionClient.getUserIdByRoleCode(RoleConstants.USER_ROLE);

            if (result1 == null || !Objects.equals(result1.getCode(), Status.CODE_200)) {
                // 处理错误情况
                log.error("获取普通用户列表失败，管理员ID: {}", user.getUserId());
                throw new BizException(Status.CODE_500, "获取普通用户失败");
            }

            List<Long> userIdList = result1.getData();

            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.in(User::getUserId, userIdList);
            List<User> userList = userMapper.selectList(queryWrapper);

            pageList.setRecords(userList);
            pageList.setTotal(userList.size());
        } else {
            log.info("超级管理员查询所有用户");

            List<User> userList = userMapper.selectList(null);

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
        log.info("查询用户信息，当前用户ID: {}, 查询目标用户ID: {}", user.getUserId(), queryUserId);

        R<String> result = permissionClient.getUserRoleCode(user.getUserId());

        if (result == null || !Objects.equals(result.getCode(), Status.CODE_200) || result.getData() == null) {
            // 处理错误情况
            log.error("获取用户角色码失败，用户ID: {}", user.getUserId());
            throw new BizException(Status.CODE_500, "获取角色码失败");
        }

        String userRoleCode = result.getData();

        UserVO userVO = new UserVO();
        if (Objects.equals(userRoleCode, RoleConstants.USER_ROLE)) {
            if (!user.getUserId().equals(queryUserId)) {
                log.error("普通用户尝试查看其他用户信息，权限不足");
                throw new BizException(Status.CODE_500, "用户权限不足");
            }

            userVO.setUserId(user.getUserId());
            userVO.setUsername(user.getUsername());
            userVO.setEmail(user.getEmail());
            userVO.setPhone(user.getPhone());
        } else if (Objects.equals(userRoleCode, RoleConstants.ADMIN_ROLE)) {
            log.info("管理员查询用户信息，当前管理员ID: {}, 目标用户ID: {}", user.getUserId(), queryUserId);

            R<String> result1 = permissionClient.getUserRoleCode(queryUserId);

            if (result1 == null || !Objects.equals(result1.getCode(), Status.CODE_200) || result1.getData() == null) {
                // 处理错误情况
                log.error("获取目标用户角色码失败，目标用户ID: {}", queryUserId);
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
                log.warn("管理员查看非普通用户信息，权限不足");
                throw new BizException(Status.CODE_500, "获用户权限不足");
            }
        } else {
            log.info("超级管理员查询所有用户信息，当前超级管理员ID: {}, 目标用户ID: {}", user.getUserId(), queryUserId);

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
        log.info("修改用户信息，当前用户ID: {}, 目标用户ID: {}", user.getUserId(), updateUser.getUserId());

        R<String> result = permissionClient.getUserRoleCode(user.getUserId());

        if (result == null || !Objects.equals(result.getCode(), Status.CODE_200) || result.getData() == null) {
            // 处理错误情况
            log.error("获取用户角色码失败，用户ID: {}", user.getUserId());
            throw new BizException(Status.CODE_500, "获取角色码失败");
        }

        String userRoleCode = result.getData();

        if (Objects.equals(userRoleCode, RoleConstants.USER_ROLE)) {
            if (!user.getUserId().equals(updateUser.getUserId())) {
                log.error("普通用户修改其他用户信息，权限不足");
                throw new BizException(Status.CODE_500, "用户权限不足");
            }

            //查询用户名是否存在
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, updateUser.getUsername());
            User userTemp = getOne(queryWrapper);
            if (userTemp != null) {
                log.error("修改失败，用户名已存在");
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
            log.info("管理员修改用户信息，当前管理员ID: {}, 目标用户ID: {}", user.getUserId(), updateUser.getUserId());

            R<String> result1 = permissionClient.getUserRoleCode(updateUser.getUserId());

            if (result1 == null || !Objects.equals(result1.getCode(), Status.CODE_200) || result1.getData() == null) {
                // 处理错误情况
                log.error("获取用户角色码失败，用户ID: {}", user.getUserId());
                throw new BizException(Status.CODE_500, "获取用户角色码失败");
            }

            //查询用户名是否存在
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, updateUser.getUsername());
            User userTemp = getOne(queryWrapper);
            if (userTemp != null) {
                log.error("修改失败，用户名已存在");
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
                log.error("管理员修改非普通用户信息，权限不足");
                throw new BizException(Status.CODE_500, "获用户权限不足");
            }
        } else {
            log.info("超级管理员修改用户信息，当前超级管理员ID: {}, 目标用户ID: {}", user.getUserId(), updateUser.getUserId());

            //查询用户名是否存在
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(User::getUsername, updateUser.getUsername());
            User userTemp = getOne(queryWrapper);
            if (userTemp != null) {
                log.error("修改失败，用户名已存在");
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
        log.info("重置用户密码");

        R<String> result = permissionClient.getUserRoleCode(user.getUserId());

        if (result == null || !Objects.equals(result.getCode(), Status.CODE_200) || result.getData() == null) {
            // 处理错误情况
            log.error("获取用户角色码失败，用户ID: {}", user.getUserId());
            throw new BizException(Status.CODE_500, "获取角色码失败");
        }

        String userRoleCode = result.getData();

        if (Objects.equals(userRoleCode, RoleConstants.USER_ROLE)) {
            log.info("普通用户重置自己密码，用户ID: {}", user.getUserId());
            user.setPassword(newPassword);
            userMapper.updateById(user);
        } else if (Objects.equals(userRoleCode, RoleConstants.ADMIN_ROLE)) {
            log.info("管理员重置所有普通用户密码，管理员ID: {}", user.getUserId());

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
            log.info("管理员重置所有用户密码，管理员ID: {}", user.getUserId());

            List<User> userList = userMapper.selectList(null);

            for (User user2 : userList) {
                user2.setPassword(newPassword);

                userMapper.updateById(user2);
            }
        }
    }


}
