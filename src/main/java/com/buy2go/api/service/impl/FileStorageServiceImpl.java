package com.buy2go.api.service.impl;

import com.buy2go.api.exception.SystemException;
import com.buy2go.api.service.FileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

    private final MinioClient minioClient;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.source-folder:buy2go/}")
    private String sourceFolder;

    @Override
    public String storeFile(MultipartFile file) {
        return storeFile(file, null);
    }

    @Override
    public String storeFile(MultipartFile file, String subFolder) {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            if (originalFileName.contains("..")) {
                throw new SystemException("Filename contains invalid path sequence: " + originalFileName);
            }

            // Ensure bucket exists
            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!isExist) {
                log.info("Bucket '{}' does not exist. Creating bucket...", bucket);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }

            String fileExtension = "";
            int extIndex = originalFileName.lastIndexOf(".");
            if (extIndex > 0) {
                fileExtension = originalFileName.substring(extIndex);
            }

            String newFileName = UUID.randomUUID() + fileExtension;

            // Normalize folder path
            String folder = sourceFolder != null ? sourceFolder.trim() : "buy2go/";
            if (!folder.endsWith("/")) {
                folder = folder + "/";
            }
            if (folder.startsWith("/")) {
                folder = folder.substring(1);
            }

            if (subFolder != null && !subFolder.trim().isEmpty()) {
                String sub = subFolder.trim();
                if (sub.startsWith("/")) sub = sub.substring(1);
                if (!sub.endsWith("/")) sub = sub + "/";
                folder = folder + sub;
            }

            String objectName = folder + newFileName;
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            try (InputStream is = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucket)
                                .object(objectName)
                                .stream(is, file.getSize(), -1)
                                .contentType(contentType)
                                .build()
                );
            }

            String baseUrl = minioUrl.replaceAll("/+$", "");
            String fileUrl = baseUrl + "/" + bucket + "/" + objectName;
            log.info("File successfully uploaded to MinIO: {}", fileUrl);
            return fileUrl;

        } catch (Exception ex) {
            log.error("Failed to upload file '{}' to MinIO bucket '{}': {}", originalFileName, bucket, ex.getMessage(), ex);
            throw new SystemException("Could not store file " + originalFileName + " to MinIO: " + ex.getMessage());
        }
    }

    @Override
    public InputStream downloadFile(String fileUrlOrObjectName) {
        if (fileUrlOrObjectName == null || fileUrlOrObjectName.trim().isEmpty()) {
            throw new SystemException("File URL or object name cannot be empty");
        }
        try {
            String objectName = fileUrlOrObjectName.trim();
            String prefix = minioUrl.replaceAll("/+$", "") + "/" + bucket + "/";
            if (objectName.startsWith(prefix)) {
                objectName = objectName.substring(prefix.length());
            } else if (objectName.startsWith(bucket + "/")) {
                objectName = objectName.substring(bucket.length() + 1);
            }

            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } catch (Exception ex) {
            log.error("Failed to download file '{}' from MinIO: {}", fileUrlOrObjectName, ex.getMessage(), ex);
            throw new SystemException("Could not download file from MinIO: " + ex.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrlOrObjectName) {
        if (fileUrlOrObjectName == null || fileUrlOrObjectName.trim().isEmpty()) {
            return;
        }

        try {
            String objectName = fileUrlOrObjectName;
            String prefix = minioUrl.replaceAll("/+$", "") + "/" + bucket + "/";
            if (objectName.startsWith(prefix)) {
                objectName = objectName.substring(prefix.length());
            }

            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            log.info("File deleted from MinIO: {}", objectName);
        } catch (Exception ex) {
            log.error("Failed to delete file '{}' from MinIO: {}", fileUrlOrObjectName, ex.getMessage(), ex);
        }
    }
}
