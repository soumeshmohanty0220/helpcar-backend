package com.helpcar.backend.common.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates validation failures into RFC 7807 problem responses.
 *
 * <p>Handlers here are deliberately narrow. A catch-all {@code Exception} handler would
 * also swallow Spring Security's {@code AccessDeniedException} thrown from method-level
 * checks and turn a 403 into a 500; Spring Boot's own error handling already renders
 * unexpected failures as a problem document without leaking stack traces.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Bean-validation failures on {@code @Valid @RequestBody} arguments. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return problem("Request validation failed", "One or more fields are invalid.", errors);
    }

    /** Bean-validation failures on {@code @Validated} method parameters (path/query params). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.putIfAbsent(String.valueOf(violation.getPropertyPath()), violation.getMessage());
        }
        return problem("Constraint violation", "One or more parameters are invalid.", errors);
    }

    private ProblemDetail problem(String title, String detail, Map<String, String> errors) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problemDetail.setTitle(title);
        problemDetail.setProperty("errors", errors);
        return problemDetail;
    }
}
