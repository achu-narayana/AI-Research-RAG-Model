package com.researchassistant.ai.client;

import com.researchassistant.common.exception.AiServiceException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/**
 * Shared HTTP client for the Python FastAPI AI service.
 *
 * All request bodies are serialized and all responses parsed
 * with the Jackson JsonMapper. Failures are translated into
 * AiServiceException with a suitable HTTP status.
 */
@Component
public class AiHttpClient {

    private static final Logger log =
            LoggerFactory.getLogger(AiHttpClient.class);

    private static final String TOKEN_HEADER = "X-Internal-Token";

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final String baseUrl;
    private final Duration requestTimeout;
    private final String token;

    public AiHttpClient(
            JsonMapper jsonMapper,
            @Value("${app.ai.base-url:http://127.0.0.1:8000}") String baseUrl,
            @Value("${app.ai.timeout-seconds:180}") long timeoutSeconds,
            @Value("${app.ai.token:}") String token) {

        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        this.jsonMapper = jsonMapper;
        this.baseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        this.requestTimeout = Duration.ofSeconds(timeoutSeconds);
        this.token = token == null ? "" : token.trim();
    }

    // =========================================================
    // PUBLIC API
    // =========================================================

    /**
     * POST a JSON body (any object Jackson can serialize) and
     * return the parsed JSON response.
     */
    public JsonNode postJson(String path, Object body) {

        String json = jsonMapper.writeValueAsString(body);

        HttpRequest request = newRequest(path)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        return send(request, path, false);
    }

    /**
     * POST a pre-built multipart/form-data body.
     */
    public JsonNode postMultipart(
            String path,
            byte[] body,
            String boundary) {

        HttpRequest request = newRequest(path)
                .header("Content-Type",
                        "multipart/form-data; boundary=" + boundary)
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        return send(request, path, false);
    }

    /**
     * DELETE a resource. A 404 from the AI service is treated
     * as success (nothing left to delete).
     */
    public void delete(String path) {

        HttpRequest request = newRequest(path)
                .header("Accept", "application/json")
                .DELETE()
                .build();

        send(request, path, true);
    }

    // =========================================================
    // INTERNALS
    // =========================================================

    private HttpRequest.Builder newRequest(String path) {

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(requestTimeout);

        if (!token.isEmpty()) {
            builder.header(TOKEN_HEADER, token);
        }

        return builder;
    }

    private JsonNode send(
            HttpRequest request,
            String path,
            boolean notFoundIsOk) {

        HttpResponse<String> response;

        try {
            response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

        } catch (HttpConnectTimeoutException e) {
            log.warn("AI service connect timeout on {}", path);
            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI service is not running", e);

        } catch (HttpTimeoutException e) {
            log.warn("AI service timed out on {}", path);
            throw new AiServiceException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "AI service timed out", e);

        } catch (ConnectException e) {
            log.warn("AI service is not reachable at {}", baseUrl);
            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI service is not running", e);

        } catch (IOException e) {
            if (hasCause(e, ConnectException.class)) {
                log.warn("AI service is not reachable at {}", baseUrl);
                throw new AiServiceException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "AI service is not running", e);
            }
            log.warn("I/O error calling AI service {}: {}", path, e.getMessage());
            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to communicate with AI service", e);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiServiceException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI request was interrupted", e);
        }

        int status = response.statusCode();
        String body = response.body() == null ? "" : response.body();

        log.info("AI service {} {} -> {} ({} chars)",
                request.method(), path, status, body.length());

        if (notFoundIsOk && status == 404) {
            return null;
        }

        if (status < 200 || status >= 300) {
            throw new AiServiceException(
                    mapStatus(status),
                    extractErrorDetail(body, status));
        }

        if (body.isBlank()) {
            return null;
        }

        try {
            return jsonMapper.readTree(body);

        } catch (RuntimeException e) {
            throw new AiServiceException(
                    HttpStatus.BAD_GATEWAY,
                    "AI service returned an invalid response", e);
        }
    }

    /**
     * FastAPI errors look like {"detail": "..."} or, for validation
     * errors, {"detail": [{"msg": "...", ...}]}.
     */
    private String extractErrorDetail(String body, int status) {

        String fallback = "AI service returned status " + status;

        if (body.isBlank()) {
            return fallback;
        }

        try {
            JsonNode node = jsonMapper.readTree(body);
            JsonNode detail = node.get("detail");

            if (detail == null || detail.isNull()) {
                return fallback;
            }

            if (detail.isString()) {
                return detail.asString();
            }

            if (detail.isArray() && !detail.isEmpty()) {
                JsonNode first = detail.get(0);
                if (first.hasNonNull("msg")) {
                    return first.get("msg").asString();
                }
            }

            return detail.toString();

        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /**
     * Keeps statuses the user can act on (bad PDF, rate limit,
     * timeout); anything else from the AI service is a 502.
     */
    private static HttpStatus mapStatus(int status) {

        return switch (status) {
            case 400, 422 -> HttpStatus.BAD_REQUEST;
            case 404 -> HttpStatus.NOT_FOUND;
            case 413 -> HttpStatus.PAYLOAD_TOO_LARGE;
            case 503 -> HttpStatus.SERVICE_UNAVAILABLE;
            case 504 -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.BAD_GATEWAY;
        };
    }

    private static boolean hasCause(
            Throwable throwable,
            Class<? extends Throwable> type) {

        Throwable current = throwable;

        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }

        return false;
    }
}
