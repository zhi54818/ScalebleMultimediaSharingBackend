package com.xjzai1.xjzai1picturebackend.service;

import com.xjzai1.xjzai1picturebackend.common.MongoPage;
import com.xjzai1.xjzai1picturebackend.model.domain.User;
import com.xjzai1.xjzai1picturebackend.model.dto.user.UserQueryRequest;
import com.xjzai1.xjzai1picturebackend.model.vo.UserLoginVo;
import com.xjzai1.xjzai1picturebackend.model.vo.UserVo;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Administrator
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-02-11 00:32:35
*/
public interface UserService {

    /**
     * 用户注册
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    String userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    UserLoginVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);


    UserVo getUserVo(User user);

    List<UserVo> getUserVoList(List<User> userList);

    /**
     * 加密密码
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏的用户信息
     * @param user
     * @return
     */
    UserLoginVo getLoginUserVo(User user);

    /**
     * 分页查询用户
     * @param userQueryRequest
     * @return
     */
    MongoPage<User> page(UserQueryRequest userQueryRequest);

    /**
     * 根据ID获取用户
     * @param id
     * @return
     */
    User getById(String id);

    /**
     * 根据ID列表获取用户
     * @param ids
     * @return
     */
    List<User> listByIds(List<String> ids);

    /**
     * 保存用户
     * @param user
     * @return
     */
    boolean save(User user);

    /**
     * 更新用户
     * @param user
     * @return
     */
    boolean updateById(User user);

    boolean isAdmin(User user);
}
