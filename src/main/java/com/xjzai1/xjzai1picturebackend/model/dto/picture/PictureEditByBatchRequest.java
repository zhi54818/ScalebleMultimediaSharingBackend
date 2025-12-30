package com.xjzai1.xjzai1picturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureEditByBatchRequest implements Serializable {
    /**
     * 图片 id 列表
     */
    private List<String> pictureIdList;

    /**
     * 分类
     */
    private String category;

    /**
     * 命名规则
     */
    private String nameRule;


    private static final long serialVersionUID = 1L;
}
