package com.vitral.exception;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldErrorItem> fieldErrors) {

    public record FieldErrorItem(String field, String message) {
    }
}
