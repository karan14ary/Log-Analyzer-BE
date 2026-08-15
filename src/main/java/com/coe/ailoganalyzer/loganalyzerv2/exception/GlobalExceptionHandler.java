package com.coe.ailoganalyzer.loganalyzerv2.exception;

import com.coe.ailoganalyzer.loganalyzerv2.model.ApiError;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handle(
            Exception exception) {

        ApiError error =
                new ApiError(
                        "LOG_ANALYSIS_ERROR",
                        exception.getMessage()
                );

        return ResponseEntity
                .internalServerError()
                .body(error);
    }
}