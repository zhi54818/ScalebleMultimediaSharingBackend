package com.xjzai1.xjzai1picturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobHttpHeaders;
import com.azure.storage.blob.models.BlobItem;
import com.xjzai1.xjzai1picturebackend.config.AzureBlobClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AzureBlobManager {

    @Resource
    private AzureBlobClientConfig azureBlobClientConfig;

    @Resource
    private BlobContainerClient blobContainerClient;

    /**
     * 上传对象方法
     *
     * @param key  文件路径（blob name）
     * @param file 文件
     */
    public void putObject(String key, File file) {
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(key);
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                BinaryData binaryData = BinaryData.fromStream(fileInputStream, file.length());
                blobClient.upload(binaryData, true);
            }
        } catch (IOException e) {
            log.error("Failed to upload file to Azure Blob Storage: {}", key, e);
            throw new RuntimeException("File upload failed", e);
        }
    }

    /**
     * 下载对象
     *
     * @param key 唯一键
     * @return 输入流
     */
    public InputStream getObject(String key) {
        BlobClient blobClient = blobContainerClient.getBlobClient(key);
        if (!blobClient.exists()) {
            throw new RuntimeException("File not found: " + key);
        }
        return blobClient.openInputStream();
    }

    /**
     * 上传图片对象（附带图片信息）
     * 注意：Azure Blob Storage 不提供类似 COS 的图片处理功能
     * 这里只上传原图，缩略图和压缩图需要单独处理或使用 Azure 的图片处理服务
     *
     * @param key  唯一键
     * @param file 文件
     */
    public void putPictureObject(String key, File file) {
        try {
            BlobClient blobClient = blobContainerClient.getBlobClient(key);
            
            // 设置 Content-Type
            String contentType = getContentType(file);
            BlobHttpHeaders headers = new BlobHttpHeaders();
            headers.setContentType(contentType);
            
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                BinaryData binaryData = BinaryData.fromStream(fileInputStream, file.length());
                blobClient.upload(binaryData, true);
                // 设置 HTTP 头
                blobClient.setHttpHeaders(headers);
            }
        } catch (IOException e) {
            log.error("Failed to upload image to Azure Blob Storage: {}", key, e);
            throw new RuntimeException("Image upload failed", e);
        }
    }

    /**
     * 删除对象
     *
     * @param key 文件 key
     */
    public void deleteObject(String key) {
        BlobClient blobClient = blobContainerClient.getBlobClient(key);
        if (blobClient.exists()) {
            blobClient.delete();
        }
    }

    /**
     * 批量删除对象
     *
     * @param keys 文件 key 列表
     */
    public void deleteObjects(List<String> keys) {
        for (String key : keys) {
            try {
                deleteObject(key);
            } catch (Exception e) {
                log.error("删除文件失败: {}", key, e);
            }
        }
    }

    /**
     * 根据文件扩展名获取 Content-Type
     */
    private String getContentType(File file) {
        String suffix = FileUtil.getSuffix(file.getName()).toLowerCase();
        switch (suffix) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "webp":
                return "image/webp";
            case "gif":
                return "image/gif";
            default:
                return "application/octet-stream";
        }
    }
}

