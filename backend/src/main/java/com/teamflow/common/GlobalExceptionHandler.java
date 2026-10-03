package com.teamflow.common;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.*;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> business(ApiException e) { return response(e.getStatus(), e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
        Map<String,String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(error -> fields.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(ApiError.of(400, "Проверьте заполнение полей", fields));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class,
        MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<ApiError> badRequest(Exception e) { return response(HttpStatus.BAD_REQUEST, "Некорректные параметры запроса"); }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> unauthorized(Exception e) { return response(HttpStatus.UNAUTHORIZED, "Неверный email или пароль"); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> forbidden(Exception e) { return response(HttpStatus.FORBIDDEN, "Недостаточно прав для этого действия"); }
    @ExceptionHandler(OptimisticLockingFailureException.class)
    ResponseEntity<ApiError> concurrent(Exception e) { return response(HttpStatus.CONFLICT, "Задача уже изменена. Обновите доску."); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> constraint(Exception e) { return response(HttpStatus.CONFLICT, "Запись конфликтует с существующими данными"); }
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> notFound(Exception e) { return response(HttpStatus.NOT_FOUND, "Ресурс не найден"); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e) {
        log.error("Unhandled request failure", e);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка. Сообщите администратору код запроса.");
    }
    private ResponseEntity<ApiError> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), message, Map.of()));
    }
}
