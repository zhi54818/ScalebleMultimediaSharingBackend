package com.xjzai1.xjzai1picturebackend.mapper;

import com.xjzai1.xjzai1picturebackend.model.domain.Picture;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
* @author Administrator
* @description 针对表【picture(图片)】的数据库操作Repository
* @createDate 2025-02-15 17:03:23
*/
public interface PictureMapper extends MongoRepository<Picture, String> {
    List<Picture> findByUserId(String userId);
    List<Picture> findByIdIn(List<String> ids);
}




