package com.xjzai1.xjzai1picturebackend.mapper;

import com.xjzai1.xjzai1picturebackend.model.domain.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
* @author Administrator
* @description 针对表【user(用户)】的数据库操作Repository
* @createDate 2025-02-11 00:32:35
*/
public interface UserMapper extends MongoRepository<User, String> {
    Optional<User> findByUserAccount(String userAccount);
    Optional<User> findByUserAccountAndUserPassword(String userAccount, String userPassword);
}




