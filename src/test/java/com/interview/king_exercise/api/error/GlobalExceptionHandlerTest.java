package com.interview.king_exercise.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.interview.king_exercise.service.exception.EvaluationConflictException;
import com.interview.king_exercise.service.exception.EvaluationNotFoundException;
import com.interview.king_exercise.service.exception.UnknownPlayerException;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void shouldHandleUnknownPlayer() {
        ResponseEntity<ErrorResponse> response = handler.handleUnknownPlayer(
                new UnknownPlayerException(List.of("p-999")));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().message()).isEqualTo("Players not found in fixture data: p-999");
    }

    @Test
    void shouldHandleMissingEvaluation() {
        ResponseEntity<ErrorResponse> response = handler.handleEvaluationNotFoundException(
                new EvaluationNotFoundException("missing"));

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().message()).isEqualTo("Campaign evaluation not found for ID: missing");
    }

    @Test
    void shouldHandleEvaluationConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleEvaluationConflictException(
                new EvaluationConflictException("eval-1"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().message())
                .isEqualTo("An evaluation with ID eval-1 already exists with different rules or player IDs.");
    }
}