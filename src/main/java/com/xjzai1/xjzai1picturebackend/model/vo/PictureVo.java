package com.xjzai1.xjzai1picturebackend.model.vo;

import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.xjzai1.xjzai1picturebackend.model.domain.Picture;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
public class PictureVo implements Serializable {
    /**
     * id
     */
    @JsonProperty("picId")
    private String id;

    /**
     * 图片 url
     */
    @JsonProperty("imageUrl")
    private String url;

    /**
     * 缩略图 Url
     */
    @JsonProperty("thumbUrl")
    private String thumbnailUrl;

    /**
     * 原图 Url（不返回给前端，与 imageUrl 相同）
     */
    @JsonIgnore
    private String originalUrl;

    /**
     * 图片名称
     */
    @JsonProperty("title")
    private String name;

    /**
     * 简介
     */
    @JsonProperty("desc")
    private String introduction;

    /**
     * 分类
     */
    @JsonProperty("tag")
    private String category;

    /**
     * 图片体积
     */
    @JsonProperty("size")
    private Long pictureSize;

    /**
     * 图片宽度
     */
    @JsonProperty("width")
    private Integer pictureWidth;

    /**
     * 图片高度
     */
    @JsonProperty("height")
    private Integer pictureHeight;

    /**
     * 图片格式
     */
    @JsonProperty("format")
    private String pictureFormat;

    /**
     * 用户 id
     */
    @JsonProperty("uploaderId")
    private String userId;


    /**
     * 创建时间
     */
    @JsonProperty("uploadTime")
    private Date createTime;

    /**
     * 编辑时间（不返回给前端）
     */
    @JsonIgnore
    private Date editTime;

    /**
     * 更新时间（不返回给前端）
     */
    @JsonIgnore
    private Date updateTime;

    /**
     * 创建用户信息
     */
    @JsonProperty("uploader")
    private UserVo user;


    private static final long serialVersionUID = 1L;

    /**
     * 封装类转对象
     */
    public static Picture voToObj(PictureVo pictureVo) {
        if (pictureVo == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureVo, picture);
        return picture;
    }

    /**
     * 对象转封装类
     */
    public static PictureVo objToVo(Picture picture) {
        if (picture == null) {
            return null;
        }
        PictureVo pictureVo = new PictureVo();
        BeanUtils.copyProperties(picture, pictureVo);
        return pictureVo;
    }

}
