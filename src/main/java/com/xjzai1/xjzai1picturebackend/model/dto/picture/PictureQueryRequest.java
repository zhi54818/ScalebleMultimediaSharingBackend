package com.xjzai1.xjzai1picturebackend.model.dto.picture;

import com.xjzai1.xjzai1picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class PictureQueryRequest extends PageRequest implements Serializable{
    /**
     * id
     */
    private String id;

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
     * 搜索词
     */
    private String searchText;

    /**
     * 创建用户 id
     */
    private String userId;

    /**
     * 开始编辑时间
     */
    private Date startEditTime;

    /**
     * 结束编辑时间
     */
    private Date endEditTime;


    private static final long serialVersionUID = 1L;
}
