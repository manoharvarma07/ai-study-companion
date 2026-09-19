package com.aistudy.backend.common.exception;

import java.time.Instant;
import org.springframework.http.HttpStatus;

public class ApiError {
    private Instant timestamp = Instant.now();
    private int status;
    private String error;
    private String message;
    private String path;

    public ApiError() {}

    public ApiError(HttpStatus status, String error, String message, String path) {
        this.status = status.value();
        this.error = error;
        this.message = message;
        this.path = path;
    }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
}
