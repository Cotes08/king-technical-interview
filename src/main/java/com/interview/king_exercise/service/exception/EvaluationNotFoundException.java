package com.interview.king_exercise.service.exception;

public class EvaluationNotFoundException extends RuntimeException {

    public EvaluationNotFoundException(String evaluationId) {
        super("Campaign evaluation not found for ID: " + evaluationId);
    }

}
