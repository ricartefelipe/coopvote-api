package com.felipericarte.coopvote.shared.error;

import com.felipericarte.coopvote.shared.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Converte falhas de domínio e de entrada em respostas Problem Details consistentes.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ProblemDetail> handleApiException(ApiException exception, HttpServletRequest request) {
        LOGGER.atWarn()
                .addKeyValue("errorCode", exception.getCode())
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .log(exception.getMessage());

        return response(
                exception.getStatus(),
                exception.getCode(),
                exception.getMessage(),
                request,
                null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException exception, HttpServletRequest request) {
        LOGGER.atWarn()
                .addKeyValue("errorCode", "FORBIDDEN")
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .log("Access denied");
        return response(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                "O associado não tem permissão para esta operação.",
                request,
                null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (first, ignored) -> first,
                        LinkedHashMap::new));

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "One or more fields are invalid.",
                request,
                errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ProblemDetail> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getConstraintViolations().forEach(violation ->
                errors.putIfAbsent(violation.getPropertyPath().toString(), violation.getMessage()));

        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "One or more parameters are invalid.",
                request,
                errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ProblemDetail> handleUnreadableMessage(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_BODY",
                "The request body is missing or malformed.",
                request,
                null);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "The Content-Type header is not supported.",
                request,
                null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ProblemDetail> handleUnsupportedMethod(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request) {
        ResponseEntity<ProblemDetail> result = response(
                HttpStatus.METHOD_NOT_ALLOWED,
                "METHOD_NOT_ALLOWED",
                "The HTTP method is not supported for this resource.",
                request,
                null);

        var supportedMethods = exception.getSupportedHttpMethods();
        if (supportedMethods == null || supportedMethods.isEmpty()) {
            return result;
        }
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .allow(supportedMethods.toArray(HttpMethod[]::new))
                .body(result.getBody());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ProblemDetail> handleMissingResource(
            NoResourceFoundException exception,
            HttpServletRequest request) {
        return response(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "The requested resource was not found.",
                request,
                null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ProblemDetail> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {
        Map<String, String> errors = Map.of(exception.getName(), "has an invalid format");
        return response(
                HttpStatus.BAD_REQUEST,
                "INVALID_PARAMETER",
                "One or more parameters have an invalid format.",
                request,
                errors);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
        LOGGER.atError()
                .addKeyValue("method", request.getMethod())
                .addKeyValue("path", request.getRequestURI())
                .setCause(exception)
                .log("Unexpected request failure");

        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred.",
                request,
                null);
    }

    private ResponseEntity<ProblemDetail> response(
            HttpStatus status,
            String code,
            String detail,
            HttpServletRequest request,
            Map<String, String> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE));
        if (errors != null && !errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return ResponseEntity.status(status).body(problem);
    }
}
