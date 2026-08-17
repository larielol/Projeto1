package com.vitral.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import java.util.List;

@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final List<String> fields;

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.code = null;
        this.fields = List.of();
    }

    public BusinessException(String message, HttpStatus status, String code, List<String> fields) {
        super(message);
        this.status = status;
        this.code = code;
        this.fields = List.copyOf(fields);
    }
}
