package com.example.demo.iface.handler.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.demo.infra.shared.exception.exception.ResourceNotFoundException;
import com.example.demo.infra.shared.exception.exception.ValidationException;
import com.example.demo.infra.shared.exception.response.BaseExceptionResponse;

/**
 * 全域例外處理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<BaseExceptionResponse> handleValidationException(ValidationException e) {
		return ResponseEntity.status(HttpStatus.OK).body(new BaseExceptionResponse(e.getCode(), e.getMessage()));
	}

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<BaseExceptionResponse> handleResourceNotFoundException(ResourceNotFoundException e) {
		return ResponseEntity.status(HttpStatus.OK).body(new BaseExceptionResponse(e.getCode(), e.getMessage()));
	}

}