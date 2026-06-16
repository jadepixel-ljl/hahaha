package com.example.personalsite.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AppException.class)
  public ResponseEntity<Map<String, Object>> handleAppException(AppException exception) {
    return ResponseEntity.status(exception.getStatus()).body(error(exception.getStatus(), exception.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
    Map<String, Object> body = error(HttpStatus.BAD_REQUEST, "请求参数不完整");
    Map<String, String> fields = new HashMap<>();
    for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
      fields.put(fieldError.getField(), fieldError.getDefaultMessage());
    }
    body.put("fields", fields);
    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleException(Exception exception) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(error(HttpStatus.INTERNAL_SERVER_ERROR, "服务器暂时有点忙"));
  }

  private Map<String, Object> error(HttpStatus status, String message) {
    Map<String, Object> body = new HashMap<>();
    body.put("timestamp", Instant.now().toString());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);
    return body;
  }
}
