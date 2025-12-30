package com.xjzai1.xjzai1picturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.xjzai1.xjzai1picturebackend.config.AzureBlobClientConfig;
import com.xjzai1.xjzai1picturebackend.exception.BusinessException;
import com.xjzai1.xjzai1picturebackend.exception.ErrorCode;
import com.xjzai1.xjzai1picturebackend.manager.AzureBlobManager;
import com.xjzai1.xjzai1picturebackend.model.dto.picture.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Date;

@Slf4j
public abstract class PictureUploadTemplate {
    @Resource
    private AzureBlobClientConfig azureBlobClientConfig;

    @Resource
    private AzureBlobManager azureBlobManager;

    /**
     * 模板方法，定义上传流程
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片
        validPicture(inputSource);

        // 2. 图片上传地址（Azure Blob Storage 的 blob name 不应该以 / 开头）
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String suffix = FileUtil.getSuffix(originFilename);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, suffix);
        // 移除开头的 /，Azure Blob Storage 的路径不应该以 / 开头
        String uploadPathPrefixClean = uploadPathPrefix.startsWith("/") ? uploadPathPrefix.substring(1) : uploadPathPrefix;
        String uploadPath = String.format("%s/%s", uploadPathPrefixClean, uploadFilename);
        File file = null;
        File compressedFile = null;
        File thumbnailFile = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile("upload_", "." + suffix);
            // 处理文件来源
            processFile(inputSource, file);

            // 4. 读取图片信息
            BufferedImage originalImage = ImageIO.read(file);
            if (originalImage == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "Unable to read image");
            }
            int pictureWidth = originalImage.getWidth();
            int pictureHeight = originalImage.getHeight();
            String pictureFormat = suffix.toLowerCase();

            // 5. 生成压缩图（WebP 格式，如果原图不是 webp）
            String compressedKey;
            if ("webp".equalsIgnoreCase(pictureFormat)) {
                // 如果原图已经是 webp，直接使用原图
                compressedKey = uploadPath;
            } else {
                compressedKey = FileUtil.mainName(uploadPath) + ".webp";
                compressedFile = File.createTempFile("compressed_", ".webp");
                try {
                    Thumbnails.of(file)
                            .scale(1.0)
                            .outputFormat("webp")
                            .outputQuality(0.8)
                            .toFile(compressedFile);
                    azureBlobManager.putPictureObject(compressedKey, compressedFile);
                } catch (Exception e) {
                    log.warn("Failed to generate compressed image, using original: {}", e.getMessage());
                    // 如果压缩失败，使用原图
                    compressedKey = uploadPath;
                    this.deleteTempFile(compressedFile);
                    compressedFile = null;
                }
            }

            // 6. 生成缩略图（仅对大于 20KB 的图片）
            String thumbnailKey = uploadPath;
            if (file.length() > 20 * 1024) {
                thumbnailKey = FileUtil.mainName(uploadPath) + "_thumbnail." + suffix;
                thumbnailFile = File.createTempFile("thumbnail_", "." + suffix);
                try {
                    Thumbnails.of(file)
                            .size(256, 256)
                            .keepAspectRatio(true)
                            .toFile(thumbnailFile);
                    azureBlobManager.putPictureObject(thumbnailKey, thumbnailFile);
                } catch (Exception e) {
                    log.warn("Failed to generate thumbnail, using compressed image: {}", e.getMessage());
                    // 如果缩略图生成失败，使用压缩图
                    thumbnailKey = compressedKey;
                }
            } else {
                // 小图片直接使用压缩图作为缩略图
                thumbnailKey = compressedKey;
            }

            // 7. 上传原图
            azureBlobManager.putPictureObject(uploadPath, file);

            // 8. 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setName(FileUtil.mainName(originFilename));
            uploadPictureResult.setPictureWidth(pictureWidth);
            uploadPictureResult.setPictureHeight(pictureHeight);
            uploadPictureResult.setPictureFormat(pictureFormat);
            uploadPictureResult.setPictureSize(FileUtil.size(file));
            uploadPictureResult.setOriginalUrl(azureBlobClientConfig.getHost() + "/" + uploadPath);
            uploadPictureResult.setUrl(azureBlobClientConfig.getHost() + "/" + compressedKey);
            uploadPictureResult.setThumbnailUrl(azureBlobClientConfig.getHost() + "/" + thumbnailKey);
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("Failed to upload image to object storage", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Upload failed");
        } finally {
            this.deleteTempFile(file);
            this.deleteTempFile(compressedFile);
            this.deleteTempFile(thumbnailFile);
        }
    }

    /**
     * 校验输入源（本地文件或 URL）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

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
