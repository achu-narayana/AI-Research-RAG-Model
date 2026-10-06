package com.researchassistant.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Failure while talking to the Python AI service.
 *
 * Defaults to 502 Bad Gateway. Use 503 when the service is not
 * reachable and 504 when it timed out.
 */
public class AiServiceException extends ApiException {

    public AiServiceException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }

    public AiServiceException(HttpStatus status, String message) {
        super(status, message);
    }

    public AiServiceException(HttpStatus status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
