package com.example.personalsite.home;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HomeController {

  @GetMapping("/profile")
  public ProfileResponse profile() {
    return new ProfileResponse(
        "困困小羊",
        "把代码写进一朵软软的云里",
        List.of("Vue", "TypeScript", "Spring Boot", "MySQL"));
  }

  public record ProfileResponse(String nickname, String headline, List<String> tags) {}
}

