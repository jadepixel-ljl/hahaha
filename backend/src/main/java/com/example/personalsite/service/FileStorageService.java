package com.example.personalsite.service;

import com.example.personalsite.config.AppProperties;
import com.example.personalsite.exception.AppException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileStorageService {

  private final AppProperties properties;

  public FileStorageService(AppProperties properties) {
    this.properties = properties;
  }

  public String saveAvatar(MultipartFile file) {
    return save(file, "avatars");
  }

  public String saveImage(MultipartFile file) {
    return save(file, "images");
  }

  public String save(MultipartFile file, String folder) {
    if (file == null || file.isEmpty()) {
      throw AppException.badRequest("请选择头像文件");
    }
    String originalName = file.getOriginalFilename() == null ? "avatar" : file.getOriginalFilename();
    String extension = "";
    int dotIndex = originalName.lastIndexOf('.');
    if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
      extension = originalName.substring(dotIndex);
    }
    String fileName = UUID.randomUUID() + extension;
    Path root = Path.of(properties.upload().location()).toAbsolutePath().normalize();
    Path target = root.resolve(folder).resolve(fileName).normalize();
    try {
      Files.createDirectories(target.getParent());
      file.transferTo(target);
    } catch (IOException exception) {
      throw new IllegalStateException("无法保存文件", exception);
    }
    return "/uploads/" + folder + "/" + fileName;
  }
}
