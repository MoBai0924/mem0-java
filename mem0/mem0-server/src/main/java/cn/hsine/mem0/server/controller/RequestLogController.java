package cn.hsine.mem0.server.controller;

import cn.hsine.mem0.server.domain.model.RequestLog;
import cn.hsine.mem0.server.repository.RequestLogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

/**
 * REST controller for request log access.
 *
 * @author MoBai

 */
@RestController
@RequestMapping("/requests")
public class RequestLogController {

    private final RequestLogRepository requestLogRepository;

    public RequestLogController(RequestLogRepository requestLogRepository) {
        this.requestLogRepository = requestLogRepository;
    }

    /**
     * Gets request logs with optional filters.
     */
    @GetMapping
    public ResponseEntity<List<RequestLog>> getLogs(
        @RequestParam(required = false) String method,
        @RequestParam(required = false) Integer statusCode,
        @RequestParam(required = false) Instant start,
        @RequestParam(required = false) Instant end,
        @RequestParam(defaultValue = "100") int limit
    ) {
        Instant startTime = start != null ? start : Instant.now().minusSeconds(3600);
        Instant endTime = end != null ? end : Instant.now();

        List<RequestLog> logs;
        if (method != null || statusCode != null) {
            logs = requestLogRepository.findByFilters(method, statusCode, startTime, endTime);
        } else {
            logs = requestLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(startTime, endTime);
        }

        if (logs.size() > limit) {
            logs = logs.subList(0, limit);
        }

        return ResponseEntity.ok(logs);
    }
}
