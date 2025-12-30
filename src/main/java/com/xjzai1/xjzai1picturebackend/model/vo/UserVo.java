package com.xjzai1.xjzai1picturebackend.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class UserVo implements Serializable {

    /**
     * id
     */
    @JsonProperty("uid")
    private String id;

    /**
     * 账号
     */
    @JsonProperty("account")
    private String userAccount;

    /**
     * 用户昵称
     */
    @JsonProperty("nickname")
    private String userName;

    /**
     * 创建时间
     */
    @JsonProperty("registerTime")
    private Date createTime;

    private static final long serialVersionUID = 1L;
}

