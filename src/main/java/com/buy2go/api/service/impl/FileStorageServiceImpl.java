package com.buy2go.api.service.impl;

import com.buy2go.api.exception.SystemException;
import com.buy2go.api.service.FileStorageService;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import jakarta.annotation.PostConstruct;
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

    @PostConstruct
    public void init() {
        ensureBucketPublicPolicy(bucket);
    }

    public void ensureBucketPublicPolicy(String bucketName) {
        if (bucketName == null || bucketName.trim().isEmpty()) return;
        try {
            boolean isExist = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!isExist) {
                log.info("Bucket '{}' does not exist. Creating bucket...", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }

            String policy = """
                    {
                        "Version": "2012-10-17",
                        "Statement": [
                            {
                                "Effect": "Allow",
                                "Principal": {"AWS": ["*"]},
                                "Action": ["s3:GetBucketLocation", "s3:ListBucket"],
                                "Resource": ["arn:aws:s3:::%s"]
                            },
                            {
                                "Effect": "Allow",
                                "Principal": {"AWS": ["*"]},
                                "Action": ["s3:GetObject"],
                                "Resource": ["arn:aws:s3:::%s/*"]
                            }
                        ]
                    }
                    """.formatted(bucketName, bucketName);

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucketName)
                            .config(policy)
                            .build()
            );
            log.info("Public read policy configured on bucket '{}'", bucketName);
        } catch (Exception e) {
            log.warn("Could not set public policy on bucket '{}': {}", bucketName, e.getMessage());
        }
    }

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

            // Normalize folder path without duplicating bucket name
            String folder = sourceFolder != null ? sourceFolder.trim() : "";
            if (bucket != null && bucket.equalsIgnoreCase("buy2go") && folder.toLowerCase().startsWith("buy2go")) {
                folder = folder.substring("buy2go".length());
            }
            if (folder.startsWith("/")) {
                folder = folder.substring(1);
            }
            if (!folder.isEmpty() && !folder.endsWith("/")) {
                folder = folder + "/";
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
            String raw = fileUrlOrObjectName.trim();
            String targetBucket = this.bucket;
            String objectName = raw;

            if (raw.startsWith("http://") || raw.startsWith("https://")) {
                try {
                    java.net.URI uri = java.net.URI.create(raw);
                    String path = uri.getPath();
                    if (path.startsWith("/")) path = path.substring(1);
                    int slashIndex = path.indexOf('/');
                    if (slashIndex > 0) {
                        targetBucket = path.substring(0, slashIndex);
                        objectName = path.substring(slashIndex + 1);
                    } else {
                        objectName = path;
                    }
                } catch (Exception e) {
                    String baseUrl = minioUrl.replaceAll("/+$", "");
                    if (objectName.startsWith(baseUrl + "/")) {
                        String path = objectName.substring(baseUrl.length() + 1);
                        int slashIndex = path.indexOf('/');
                        if (slashIndex > 0) {
                            targetBucket = path.substring(0, slashIndex);
                            objectName = path.substring(slashIndex + 1);
                        } else {
                            objectName = path;
                        }
                    }
                }
            } else {
                if (objectName.startsWith(targetBucket + "/")) {
                    objectName = objectName.substring(targetBucket.length() + 1);
                } else if (objectName.startsWith("enggis/")) {
                    targetBucket = "enggis";
                    objectName = objectName.substring("enggis/".length());
                } else if (objectName.startsWith("buy2go/")) {
                    targetBucket = "buy2go";
                    objectName = objectName.substring("buy2go/".length());
                }
            }

            try {
                return minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(targetBucket)
                                .object(objectName)
                                .build()
                );
            } catch (Exception ex) {
                // Try fallback bucket if file was stored in enggis or buy2go previously
                String fallbackBucket = "buy2go".equalsIgnoreCase(targetBucket) ? "enggis" : "buy2go";
                try {
                    return minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(fallbackBucket)
                                    .object(objectName)
                                    .build()
                    );
                } catch (Exception ignored) {
                    String folder = sourceFolder != null ? sourceFolder.trim() : "buy2go/";
                    if (!folder.endsWith("/")) folder += "/";
                    if (folder.startsWith("/")) folder = folder.substring(1);
                    if (!objectName.startsWith(folder)) {
                        try {
                            return minioClient.getObject(
                                    GetObjectArgs.builder()
                                            .bucket(targetBucket)
                                            .object(folder + objectName)
                                            .build()
                            );
                        } catch (Exception ignored2) {}
                        try {
                            return minioClient.getObject(
                                    GetObjectArgs.builder()
                                            .bucket(fallbackBucket)
                                            .object(folder + objectName)
                                            .build()
                            );
                        } catch (Exception ignored3) {}
                    }
                    throw ex;
                }
            }
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
