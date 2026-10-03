package com.teamflow.common;

import org.slf4j.MDC;
import java.time.Instant;
import java.util.Map;

public record ApiError(Instant timestamp, int status, String message, String requestId, Map<String, String> fields) {
    public static ApiError of(int status, String message, Map<String, String> fields) {
        return new ApiError(Instant.now(), status, message, MDC.get("requestId"), fields);
    }
}
