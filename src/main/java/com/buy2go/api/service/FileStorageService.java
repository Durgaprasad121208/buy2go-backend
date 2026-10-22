package com.buy2go.api.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

public interface FileStorageService {
    String storeFile(MultipartFile file);
    String storeFile(MultipartFile file, String subFolder);
    InputStream downloadFile(String objectName);
    void deleteFile(String fileUrlOrObjectName);
}
