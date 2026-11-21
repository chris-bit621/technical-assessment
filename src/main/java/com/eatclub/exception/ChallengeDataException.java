package com.eatclub.exception;

public class ChallengeDataException extends RuntimeException {

    public ChallengeDataException(String message) {
        super(message);
    }

    public ChallengeDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
