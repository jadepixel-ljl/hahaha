package com.example.personalsite.controller;

import com.example.personalsite.dto.UploadResponse;
import com.example.personalsite.security.AuthService;
import com.example.personalsite.service.FileStorageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/upload")
public class UploadController {

  private final FileStorageService fileStorageService;
  private final AuthService authService;

  public UploadController(FileStorageService fileStorageService, AuthService authService) {
    this.fileStorageService = fileStorageService;
    this.authService = authService;
  }

  @PostMapping("/avatar")
  public UploadResponse avatar(@RequestParam("file") MultipartFile file) {
    return new UploadResponse(fileStorageService.saveAvatar(file));
  }

  @PostMapping("/image")
  public UploadResponse image(@RequestParam("file") MultipartFile file) {
    authService.requireUser();
    return new UploadResponse(fileStorageService.saveImage(file));
  }
}
