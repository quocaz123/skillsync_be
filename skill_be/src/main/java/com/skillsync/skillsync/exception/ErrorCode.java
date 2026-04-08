package com.skillsync.skillsync.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    USER_EXISTS(409, "User already exists", HttpStatus.CONFLICT),
    USER_NOT_FOUND(404, "User not found", HttpStatus.NOT_FOUND),
    INVALID_CREDENTIALS(401, "Invalid password", HttpStatus.UNAUTHORIZED),
    NO_REFRESH_TOKEN(401, "No refresh token", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(401, "Invalid or expired token", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(401, "Unauthorized access", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Access denied", HttpStatus.FORBIDDEN),
    NOT_FOUND(404, "Resource not found", HttpStatus.NOT_FOUND),
    INTERNAL_SERVER_ERROR(500, "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST(400, "Invalid request parameters", HttpStatus.BAD_REQUEST),
    INVALID_GOOGLE_TOKEN(401, "Invalid Google token", HttpStatus.UNAUTHORIZED),
    SLOT_ALREADY_BOOKED(409, "Slot is no longer available", HttpStatus.CONFLICT),
    INSUFFICIENT_CREDITS(402, "Insufficient credits balance", HttpStatus.PAYMENT_REQUIRED),
    TOO_EARLY_TO_JOIN(400, "Session has not started yet (join 10 mins before)", HttpStatus.BAD_REQUEST),
    SESSION_EXPIRED(400, "Session join window has expired", HttpStatus.BAD_REQUEST),
    SKILL_NOT_FOUND(404, "Teaching skill not found", HttpStatus.NOT_FOUND),
    SESSION_ALREADY_DECIDED(409, "Session has already been approved or rejected", HttpStatus.CONFLICT),
    NOT_SESSION_TEACHER(403, "You are not the teacher of this session", HttpStatus.FORBIDDEN)
    ;

    private final int code;
    private final String message;
    private final HttpStatus statusCode;

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
