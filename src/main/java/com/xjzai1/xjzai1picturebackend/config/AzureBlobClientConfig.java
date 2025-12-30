package com.xjzai1.xjzai1picturebackend.config;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "azure.blob")
@Data
public class AzureBlobClientConfig {

    /**
     * 存储账户名称
     */
    private String accountName;

    /**
     * 存储账户密钥（可选，如果使用 DefaultAzureCredential 则不需要）
     */
    private String accountKey;

    /**
     * 容器名称（类似 COS 的 bucket）
     */
    private String containerName;

    /**
     * 访问域名（CDN 或自定义域名）
     */
    private String host;

    /**
     * 连接字符串（可选，如果提供了 accountName 和 accountKey，会自动构建）
     */
    private String connectionString;

    /**
     * 是否使用 DefaultAzureCredential（用于 Azure 环境中的身份验证）
     */
    private boolean useDefaultCredential = false;

    @Bean
    public BlobServiceClient blobServiceClient() {
        if (useDefaultCredential) {
            // 使用 DefaultAzureCredential（适用于 Azure 环境）
            TokenCredential credential = new DefaultAzureCredentialBuilder().build();
            return new BlobServiceClientBuilder()
                    .endpoint(String.format("https://%s.blob.core.windows.net", accountName))
                    .credential(credential)
                    .buildClient();
        } else if (connectionString != null && !connectionString.isEmpty()) {
            // 使用连接字符串
            return new BlobServiceClientBuilder()
                    .connectionString(connectionString)
                    .buildClient();
        } else {
            // 使用账户名称和密钥
            String connStr = String.format(
                    "DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net",
                    accountName, accountKey);
            return new BlobServiceClientBuilder()
                    .connectionString(connStr)
                    .buildClient();
        }
    }

    @Bean
    public BlobContainerClient blobContainerClient(BlobServiceClient blobServiceClient) {
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        // 如果容器不存在，创建它
        if (!containerClient.exists()) {
            containerClient.create();
        }
        return containerClient;
    }
}

