package com.example.personalsite.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class JsonSupport {

  private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

  private final ObjectMapper objectMapper;

  public JsonSupport(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String toJson(List<String> values) {
    try {
      return objectMapper.writeValueAsString(values == null ? List.of() : values);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to serialize json", exception);
    }
  }

  public List<String> fromJson(String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(json, STRING_LIST);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("Unable to parse json", exception);
    }
  }
}
