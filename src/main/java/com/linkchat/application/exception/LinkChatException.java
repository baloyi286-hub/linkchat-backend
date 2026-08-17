package com.linkchat.application.exception;

public abstract class LinkChatException extends RuntimeException {
    protected LinkChatException(String message) {
        super(message);
    }

    protected LinkChatException(String message, Throwable cause) {
        super(message, cause);
    }
}
