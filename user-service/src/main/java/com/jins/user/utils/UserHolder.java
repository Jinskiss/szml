package com.jins.user.utils;


import com.jins.user.domain.entity.User;

/**
 * 用户信息
 */
public class UserHolder {

    private static final ThreadLocal<User> userThreadLocal = new ThreadLocal<>();

    public static void saveUser(User user) {
        userThreadLocal.set(user);
    }

    public static User getUser() {
        return userThreadLocal.get();
    }

    public static void removeUser() {
        userThreadLocal.remove();
    }

}
