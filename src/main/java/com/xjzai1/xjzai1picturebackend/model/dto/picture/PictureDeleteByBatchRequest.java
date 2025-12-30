package com.xjzai1.xjzai1picturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureDeleteByBatchRequest implements Serializable {
    /**
     * 图片 id 列表
     */
    private List<String> pictureIdList;

    private static final long serialVersionUID = 1L;
}
