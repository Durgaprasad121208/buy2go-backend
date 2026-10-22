package com.buy2go.api.service.impl;

import com.buy2go.api.exception.SystemException;
import com.buy2go.api.service.FileStorageService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageServiceImpl() {
        this.fileStorageLocation = Paths.get("static/uploads").toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new SystemException("Could not create the directory where the uploaded files will be stored.");
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            if (originalFileName.contains("..")) {
                throw new SystemException("Filename contains invalid path sequence: " + originalFileName);
            }

            String fileExtension = "";
            int extIndex = originalFileName.lastIndexOf(".");
            if (extIndex > 0) {
                fileExtension = originalFileName.substring(extIndex);
            }

            String newFileName = UUID.randomUUID() + fileExtension;
            Path targetLocation = this.fileStorageLocation.resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "/v1.0/buy2go-service/uploads/" + newFileName;
        } catch (IOException ex) {
            throw new SystemException("Could not store file " + originalFileName + ". Please try again!");
        }
    }
}
