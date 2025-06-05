package com.tradegenie.platform.tradegenie_backend_api.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
  private String result; // SUCCESS or ERROR
  private String message; // success or error message
  private T data; // return object from service class, if successful
}