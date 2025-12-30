package com.xjzai1.xjzai1picturebackend.controller;

import com.xjzai1.xjzai1picturebackend.common.MongoPage;
import com.xjzai1.xjzai1picturebackend.common.BaseResponse;
import com.xjzai1.xjzai1picturebackend.common.ResultUtils;
import com.xjzai1.xjzai1picturebackend.exception.BusinessException;
import com.xjzai1.xjzai1picturebackend.exception.ErrorCode;
import com.xjzai1.xjzai1picturebackend.exception.ThrowUtils;
import com.xjzai1.xjzai1picturebackend.model.domain.Picture;
import com.xjzai1.xjzai1picturebackend.model.domain.User;
import com.xjzai1.xjzai1picturebackend.model.dto.picture.*;
import com.xjzai1.xjzai1picturebackend.model.vo.PictureVo;
import com.xjzai1.xjzai1picturebackend.service.PictureService;
import com.xjzai1.xjzai1picturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private UserService userService;

    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVo> uploadPicture(
            @RequestPart("file") MultipartFile multipartFile,
            PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        log.info("接收到的数据: name={}", pictureUploadRequest.getPictureName());
        User loginUser = userService.getLoginUser(request);
        PictureVo pictureVo = pictureService.uploadPicture(multipartFile, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVo);
    }

    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVo> uploadPictureByUrl(
            @RequestBody PictureUploadRequest pictureUploadRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVo pictureVo = pictureService.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
        return ResultUtils.success(pictureVo);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody PictureDeleteRequest pictureDeleteRequest, HttpServletRequest request) {
        if (pictureDeleteRequest == null || pictureDeleteRequest.getId() == null || pictureDeleteRequest.getId().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        String pictureId = pictureDeleteRequest.getId();
        Boolean result = pictureService.deletePicture(pictureId, loginUser);
        return ResultUtils.success(result);
    }

    /**
     * 批量删除图片
     */
    @PostMapping("/delete/batch")
    public BaseResponse<Boolean> deletePictureByBatch(@RequestBody PictureDeleteByBatchRequest pictureDeleteByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureDeleteByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.doPictureDeleteByBatch(pictureDeleteByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 编辑图片
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody PictureEditRequest pictureEditRequest, HttpServletRequest request) {
        if (pictureEditRequest == null || pictureEditRequest.getId() == null || pictureEditRequest.getId().isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        pictureService.editPicture(pictureEditRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 批量编辑图片
     */
    @PostMapping("/edit/batch")
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody PictureEditByBatchRequest pictureEditByBatchRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(pictureEditByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        pictureService.editPictureByBatch(pictureEditByBatchRequest, loginUser);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片
     */
    @GetMapping("/get")
    public BaseResponse<Picture> getPictureById(String id, HttpServletRequest request) {
        if (id == null || id.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR, "Data not found");
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVo> getPictureVoById(String id, HttpServletRequest request) {
        if (id == null || id.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        PictureVo pictureVo = pictureService.getPictureVo(picture, request);
        return ResultUtils.success(pictureVo);
    }

    /**
     * 分页获取图片列表
     */
    @PostMapping("/list/page")
    public BaseResponse<MongoPage<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {
        MongoPage<Picture> picturePage = pictureService.page(pictureQueryRequest);
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<MongoPage<PictureVo>> listPictureVoByPage(@RequestBody PictureQueryRequest pictureQueryRequest,
                                                             HttpServletRequest request) {
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        MongoPage<Picture> picturePage = pictureService.page(pictureQueryRequest);
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVoPage(picturePage, request));
    }

}
