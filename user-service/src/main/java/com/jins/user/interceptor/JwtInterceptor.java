package com.jins.user.interceptor;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.jins.user.constants.RedisConstants;
import com.jins.constants.Status;
import com.jins.user.domain.entity.User;
import com.jins.user.exception.BizException;
import com.jins.user.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * jwt拦截器
 * 处理token检验等
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private RedisTemplate<String, User> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        String token = request.getHeader("Authorization");
        if (token != null) {
            token = token.substring("Bearer ".length());
        }
        //如果不是映射到方法，直接通过
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        //验证是否有token
        if (!StringUtils.hasLength(token)) {
            throw new BizException(Status.TOKEN_ERROR, "token失效,请重新登陆");
        }
        //通过token，将redis中的user存到threadlocal（UserHolder）
        User user = redisTemplate.opsForValue().get(RedisConstants.USER_TOKEN_KEY + token);

        if (user == null) {
            throw new BizException(Status.TOKEN_ERROR, "token失效,请重新登陆");
        }
        UserHolder.saveUser(user);
        //重置过期时间
        redisTemplate.expire(RedisConstants.USER_TOKEN_KEY + token, RedisConstants.USER_TOKEN_TTL, TimeUnit.MINUTES);

        //验证token
        JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(user.getUsername())).build();
        try {
            jwtVerifier.verify(token);
        } catch (JWTVerificationException e) {
            throw new BizException(Status.TOKEN_ERROR, "token验证失败，请重新登陆");
        }

        return true;
    }
}
