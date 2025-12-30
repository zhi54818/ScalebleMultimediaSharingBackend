package com.xjzai1.xjzai1picturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.xjzai1.xjzai1picturebackend.config.AzureBlobClientConfig;
import com.xjzai1.xjzai1picturebackend.exception.BusinessException;
import com.xjzai1.xjzai1picturebackend.exception.ErrorCode;
import com.xjzai1.xjzai1picturebackend.exception.ThrowUtils;
import com.xjzai1.xjzai1picturebackend.manager.AzureBlobManager;
import com.xjzai1.xjzai1picturebackend.model.dto.picture.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 文件服务
 * @Deprecated 已废弃，改为使用upload包的模板方法优化
 */
@Service
@Slf4j
@Deprecated // 服务已废弃注解
public class FileManager {
    @Resource
    private AzureBlobClientConfig azureBlobClientConfig;

    @Resource
    private AzureBlobManager azureBlobManager;

    /**
     * 上传图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        validPicture(multipartFile);
        // 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = multipartFile.getOriginalFilename();
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);
            // 上传图片
            azureBlobManager.putPictureObject(uploadPath, file);
            // 读取图片信息
            BufferedImage image = ImageIO.read(file);
            if (image == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "Unable to read image");
            }
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = image.getWidth();
            int picHeight = image.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            String format = FileUtil.getSuffix(originFilename).toLowerCase();
            uploadPictureResult.setName(FileUtil.mainName(originFilename));
            uploadPictureResult.setPictureWidth(picWidth);
            uploadPictureResult.setPictureHeight(picHeight);
            uploadPictureResult.setPictureFormat(format);
            uploadPictureResult.setPictureSize(FileUtil.size(file));
            uploadPictureResult.setUrl(azureBlobClientConfig.getHost() + "/" + uploadPath);
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Upload failed");
        } finally {
            this.deleteTempFile(file);
        }
    }

    /**
     * 校验文件
     *
     * @param multipartFile multipart 文件
     */
    public void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "File cannot be null");
        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > 15 * ONE_M, ErrorCode.PARAMS_ERROR, "File size must not exceed 15MB");
        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "Invalid file type");
    }

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }



}
