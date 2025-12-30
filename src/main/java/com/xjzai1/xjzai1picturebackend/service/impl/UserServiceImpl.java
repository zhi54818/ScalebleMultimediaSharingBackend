package com.xjzai1.xjzai1picturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.xjzai1.xjzai1picturebackend.common.MongoPage;
import com.xjzai1.xjzai1picturebackend.constant.UserConstant;
import com.xjzai1.xjzai1picturebackend.exception.BusinessException;
import com.xjzai1.xjzai1picturebackend.exception.ErrorCode;
import com.xjzai1.xjzai1picturebackend.model.domain.User;
import com.xjzai1.xjzai1picturebackend.model.dto.user.UserQueryRequest;
import com.xjzai1.xjzai1picturebackend.model.enums.UserRoleEnum;
import com.xjzai1.xjzai1picturebackend.model.vo.UserLoginVo;
import com.xjzai1.xjzai1picturebackend.model.vo.UserVo;
import com.xjzai1.xjzai1picturebackend.service.UserService;
import com.xjzai1.xjzai1picturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
* @author Administrator
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-02-11 00:32:35
*/
@Service
@Slf4j
public class UserServiceImpl implements UserService, UserConstant {

    @Resource
    private UserMapper userMapper;

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public String userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. Validate params
        // cannot be blank
        if(StrUtil.hasBlank(userAccount, userPassword, checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Parameters are empty");
        }
        // account length
        if(userAccount.length() < 6 || userAccount.length() > 16){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Account length must be between 6 and 16 characters");
        }
        // password length
        if(userPassword.length() < 8 || userPassword.length() > 32){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Password must be at least 8 characters");
        }
        // passwords match
        if(!checkPassword.equals(userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Passwords do not match");
        }
        // 2.检查是否重复
        Optional<User> existingUser = userMapper.findByUserAccount(userAccount);
        if (existingUser.isPresent()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Account already in use");
        }
        // 3.加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4.存入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        // 使用账号名作为默认昵称
        user.setUserName(userAccount);
        user.setUpdateTime(new Date());
        user.setCreateTime(new Date());
        user.setUserAvatar("https://xjzai1.blob.core.windows.net/pictureshare/public/6948161fbbb958089bc881c4/2025-12-23_mMJXPQyhvZiGKVZJ.jpg");
        user.setIsDelete(0);
        User savedUser = userMapper.save(user);
        if (savedUser == null || savedUser.getId() == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Registration failed due to database error");
        }
        return savedUser.getId();
    }

    @Override
    public UserLoginVo userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. Validate params
        if(StrUtil.hasBlank(userAccount, userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Parameters are empty");
        }
        // account length
        if(userAccount.length() < 6 || userAccount.length() > 16){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid account");
        }
        // password length
        if(userPassword.length() < 8 || userPassword.length() > 32){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Invalid password");
        }
        // 2.加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 3.用户是否存在
        Optional<User> userOpt = userMapper.findByUserAccountAndUserPassword(userAccount, encryptPassword);
        if (!userOpt.isPresent()) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "User not found or password incorrect");
        }
        User user = userOpt.get();
        // 4.记录用户的登录态
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, user);
        return this.getLoginUserVo(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request){
        // 先判断是否已登录
        User currentUser = (User) request.getSession().getAttribute(USER_LOGIN_STATE);
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NO_LOGIN);
        }
        String userId = currentUser.getId();
        Optional<User> userOpt = userMapper.findById(userId);
        if (!userOpt.isPresent()) {
            throw new BusinessException(ErrorCode.NO_LOGIN, "User no longer exists");
        }
        User user = userOpt.get();
        // 检查是否被逻辑删除
        if (user.getIsDelete() != null && user.getIsDelete() == 1) {
            throw new BusinessException(ErrorCode.NO_LOGIN, "User no longer exists");
        }
        return user;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "Not logged in");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVo getUserVo(User user) {
        if (user == null) {
            return null;
        }
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        return userVo;
    }

    @Override
    public List<UserVo> getUserVoList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVo).collect(Collectors.toList());
    }

    @Override
    public String getEncryptPassword(String userPassword){
        // 盐值
        final String SALT = "xjzai1";
        return DigestUtils.md5DigestAsHex((userPassword + SALT).getBytes());
    }

    @Override
    public UserLoginVo getLoginUserVo(User user){
        // 将 User 类的属性复制到 LoginUserVO 中，不存在的字段就被过滤掉了
        if (user == null) {
            return null;
        }
        UserLoginVo userLoginVo = new UserLoginVo();
        BeanUtils.copyProperties(user, userLoginVo);
        return userLoginVo;
    }

    @Override
    public MongoPage<User> page(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "Request parameters are empty");
        }
        Query query = new Query();
        Criteria criteria = new Criteria();
        
        String id = userQueryRequest.getId() != null ? userQueryRequest.getId().toString() : null;
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        
        // 逻辑删除过滤
        criteria.and("isDelete").is(0);
        
        if (StrUtil.isNotBlank(id)) {
            criteria.and("id").is(id);
        }
        if (StrUtil.isNotBlank(userAccount)) {
            criteria.and("userAccount").regex(userAccount, "i");
        }
        if (StrUtil.isNotBlank(userName)) {
            criteria.and("userName").regex(userName, "i");
        }
        if (StrUtil.isNotBlank(userProfile)) {
            criteria.and("userProfile").regex(userProfile, "i");
        }
        if (StrUtil.isNotBlank(userRole)) {
            criteria.and("userRole").is(userRole);
        }
        
        query.addCriteria(criteria);
        
        // 排序
        if (StrUtil.isNotBlank(sortField)) {
            Sort.Direction direction = "ascend".equals(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
            query.with(Sort.by(direction, sortField));
        }
        
        // 分页
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        long total = mongoTemplate.count(query, User.class);
        query.with(PageRequest.of((int)current - 1, (int)pageSize));
        
        List<User> users = mongoTemplate.find(query, User.class);
        return new MongoPage<>(users, total, current, pageSize);
    }

    @Override
    public User getById(String id) {
        Optional<User> userOpt = userMapper.findById(id);
        if (!userOpt.isPresent()) {
            return null;
        }
        User user = userOpt.get();
        // 检查逻辑删除
        if (user.getIsDelete() != null && user.getIsDelete() == 1) {
            return null;
        }
        return user;
    }

    @Override
    public List<User> listByIds(List<String> ids) {
        if (CollUtil.isEmpty(ids)) {
            return new ArrayList<>();
        }
        Query query = new Query();
        query.addCriteria(Criteria.where("id").in(ids).and("isDelete").is(0));
        return mongoTemplate.find(query, User.class);
    }

    @Override
    public boolean save(User user) {
        if (user.getIsDelete() == null) {
            user.setIsDelete(0);
        }
        User saved = userMapper.save(user);
        return saved != null;
    }

    @Override
    public boolean updateById(User user) {
        if (user.getId() == null) {
            return false;
        }
        User saved = userMapper.save(user);
        return saved != null;
    }

    @Override
    public boolean isAdmin(User user){
        return user != null && Objects.equals(user.getUserRole(), UserRoleEnum.ADMIN.getValue());
    }
}




