package com.xjzai1.xjzai1picturebackend.service;

import com.xjzai1.xjzai1picturebackend.common.MongoPage;
import com.xjzai1.xjzai1picturebackend.model.domain.Picture;
import com.xjzai1.xjzai1picturebackend.model.domain.User;
import com.xjzai1.xjzai1picturebackend.model.dto.picture.*;
import com.xjzai1.xjzai1picturebackend.model.vo.PictureVo;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Administrator
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-02-15 17:03:23
 */
public interface PictureService {

    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVo uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);


    /**
     * 分页查询图片
     * @param pictureQueryRequest
     * @return
     */
    MongoPage<Picture> page(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取vo类
     * @param picture
     * @param request
     * @return
     */
    PictureVo getPictureVo(Picture picture, HttpServletRequest request);

    MongoPage<PictureVo> getPictureVoPage(MongoPage<Picture> picturePage, HttpServletRequest request);

    /**
     * 根据ID获取图片
     * @param id
     * @return
     */
    Picture getById(String id);

    /**
     * 根据ID列表获取图片
     * @param ids
     * @return
     */
    List<Picture> listByIds(List<String> ids);

    /**
     * 根据查询条件获取图片列表
     * @param query
     * @return
     */
    List<Picture> list(Query query);

    /**
     * 保存图片
     * @param picture
     * @return
     */
    boolean save(Picture picture);

    /**
     * 更新图片
     * @param picture
     * @return
     */
    boolean updateById(Picture picture);

    boolean deletePicture(String pictureId, User loginUser);

    @Transactional(rollbackFor = Exception.class)
    void doPictureDeleteByBatch(PictureDeleteByBatchRequest pictureDeleteByBatchRequest, User loginUser);

    void editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    @Transactional(rollbackFor = Exception.class)
    void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    void validPicture(Picture picture);

    @Async // 此处使用了异步注解，需要在启动类添加@EnableAsync注解才能生效
    void clearPictureFile(Picture oldPicture);

    // 先不使用异步注解，防止数据库删除了但Azure Blob Storage没有
    void clearPictureFiles(List<Picture> pictureList);
}
