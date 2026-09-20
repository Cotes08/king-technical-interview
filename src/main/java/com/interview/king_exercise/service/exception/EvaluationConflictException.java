package com.interview.king_exercise.service.exception;

public class EvaluationConflictException extends RuntimeException {
    public EvaluationConflictException(String evaluationId) {
        super("An evaluation with ID " + evaluationId
                + " already exists with different rules or player IDs.");
    }
}