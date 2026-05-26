package cn.hsine.mem0.server.exception;

import cn.hsine.mem0.core.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST API.
 *
 * @author MoBai
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MemoryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleMemoryNotFound(MemoryNotFoundException ex, HttpServletRequest request) {
        log.debug("Memory not found: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ErrorResponse> handleInvalidInput(InvalidInputException ex, HttpServletRequest request) {
        log.debug("Invalid input: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(ConfigurationException.class)
    public ResponseEntity<ErrorResponse> handleConfiguration(ConfigurationException ex, HttpServletRequest request) {
        log.warn("Configuration error: {}", ex.getMessage());
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(EmbeddingException.class)
    public ResponseEntity<ErrorResponse> handleEmbedding(EmbeddingException ex, HttpServletRequest request) {
        log.error("Embedding error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(LLMException.class)
    public ResponseEntity<ErrorResponse> handleLLM(LLMException ex, HttpServletRequest request) {
        log.error("LLM error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(VectorStoreException.class)
    public ResponseEntity<ErrorResponse> handleVectorStore(VectorStoreException ex, HttpServletRequest request) {
        log.error("Vector store error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.debug("Validation error: {}", ex.getMessage());
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                details.put(error.getField(), error.getDefaultMessage())
        );
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR",
                "Input validation failed", request, details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.debug("Illegal argument: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String code,
                                                        String message, HttpServletRequest request) {
        return buildResponse(status, code, message, request, null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String code,
                                                        String message, HttpServletRequest request,
                                                        Map<String, String> details) {
        ErrorResponse error = new ErrorResponse(
                code, message, details, Instant.now(), request.getRequestURI()
        );
        return ResponseEntity.status(status).body(error);
    }

    /**
     * Error response DTO.
     *
     * @param code      the error code
     * @param message   the error message
     * @param details   additional details
     * @param timestamp the timestamp
     * @param path      the request path
     */
    public record ErrorResponse(
            String code,
            String message,
            Map<String, String> details,
            Instant timestamp,
            String path
    ) {
    }
}
