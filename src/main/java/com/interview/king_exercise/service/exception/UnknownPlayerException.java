package com.interview.king_exercise.service.exception;

import java.util.List;

public class UnknownPlayerException extends RuntimeException {
    public UnknownPlayerException(List<String> playerIds) {
        super("Players not found in fixture data: " + String.join(", ", playerIds));
    }
}
