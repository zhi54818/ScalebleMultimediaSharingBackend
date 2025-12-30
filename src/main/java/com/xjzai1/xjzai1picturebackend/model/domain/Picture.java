package com.xjzai1.xjzai1picturebackend.model.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 图片
 */
@Document(collection = "picture")
@Data
public class Picture implements Serializable {
    /**
     * id
     */
    @Id
    private String id;

    /**
     * 图片 url
     */
    private String url;

    /**
     * 缩略图 Url
     */
    private String thumbnailUrl;

    /**
     * 原图 Url
     */
    private String originalUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 图片体积
     */
    private Long pictureSize;

    /**
     * 图片宽度
     */
    private Integer pictureWidth;

    /**
     * 图片高度
     */
    private Integer pictureHeight;

    /**
     * 图片格式
     */
    private String pictureFormat;

    /**
     * 创建用户 id
     */
    private String userId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;


    private static final long serialVersionUID = 1L;
}