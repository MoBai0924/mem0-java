package cn.hsine.mem0.core.exception;

import lombok.Data;

import java.util.Map;

@Data
public class Mem0ValidationError extends RuntimeException {
    private String message;
    private String error_code;
    private Map<String, Object> details;
    private String suggestion;

    public Mem0ValidationError(String message, String error_code, Map<String, Object> details, String suggestion) {
        super(message);
        this.message = message;
        this.error_code = error_code;
        this.details = details;
        this.suggestion = suggestion;
    }
}
