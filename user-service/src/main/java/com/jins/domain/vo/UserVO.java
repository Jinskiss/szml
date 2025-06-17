package com.jins.domain.vo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class UserVO {
    /**
     * 用户名
     */
    private String username;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 手机号码
     */
    private String phone;
}
