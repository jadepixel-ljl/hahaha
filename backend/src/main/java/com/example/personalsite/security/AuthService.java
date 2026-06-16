package com.example.personalsite.security;

import com.example.personalsite.entity.User;
import com.example.personalsite.entity.UserStatus;
import com.example.personalsite.exception.AppException;
import com.example.personalsite.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final UserRepository userRepository;

  public AuthService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User requireUser() {
    Long userId = CurrentUser.get();
    if (userId == null) {
      throw AppException.unauthorized("请先登录");
    }
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> AppException.unauthorized("登录状态无效，请重新登录"));
    if (user.getStatus() == UserStatus.BANNED) {
      throw AppException.forbidden("账号已被封禁");
    }
    return user;
  }

  public User requireAdmin() {
    User user = requireUser();
    if (!user.isAdmin()) {
      throw AppException.forbidden("需要管理员权限");
    }
    return user;
  }
}
