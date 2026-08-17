package com.vitral.exception;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException exception, WebRequest request) {
        List<ApiError.FieldErrorItem> fields = exception.getFields().stream()
                .map(field -> new ApiError.FieldErrorItem(field, "Campo ausente ou invalido"))
                .toList();
        return build(exception.getStatus(), exception.getCode(), exception.getMessage(), request, fields);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException exception, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Credenciais invalidas", request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception, WebRequest request) {
        List<ApiError.FieldErrorItem> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Erro de validacao", request, fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception, WebRequest request) {
        String message = causaMenciona(exception, "BookGenre") ? "bookGenre invalido" : "Corpo da requisicao invalido";
        return build(HttpStatus.BAD_REQUEST, message, request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException exception, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "Voce nao tem permissao para realizar esta acao", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception, WebRequest request) {
        log.warn("Conflito de integridade em {}", path(request));
        return build(HttpStatus.CONFLICT, "A operacao conflita com dados existentes", request, List.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(MaxUploadSizeExceededException exception, WebRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "A imagem deve ter no maximo 5MB", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, WebRequest request) {
        log.error("Erro inesperado em {}", path(request), exception);
        return build(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno. Tente novamente mais tarde.",
                request,
                List.of());
    }

    private ApiError.FieldErrorItem toFieldError(FieldError fieldError) {
        return new ApiError.FieldErrorItem(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private boolean causaMenciona(Throwable throwable, String texto) {
        for (Throwable atual = throwable; atual != null; atual = atual.getCause()) {
            if (atual.getMessage() != null && atual.getMessage().contains(texto)) return true;
        }
        return false;
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, WebRequest request,
            List<ApiError.FieldErrorItem> fieldErrors) {
        return build(status, null, message, request, fieldErrors);
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String code, String message, WebRequest request,
            List<ApiError.FieldErrorItem> fieldErrors) {
        ApiError body = new ApiError(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path(request),
                fieldErrors);
        return ResponseEntity.status(status).body(body);
    }

    private String path(WebRequest request) {
        return ((ServletWebRequest) request).getRequest().getRequestURI();
    }
}
