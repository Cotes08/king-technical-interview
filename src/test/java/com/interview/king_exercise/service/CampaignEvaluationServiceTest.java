package com.interview.king_exercise.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.interview.king_exercise.api.dto.CampaignEvaluationRequest;
import com.interview.king_exercise.api.dto.CampaignEvaluationResponse;
import com.interview.king_exercise.api.dto.CampaignRulesDto;
import com.interview.king_exercise.repository.CampaignEvaluationRepository;
import com.interview.king_exercise.repository.PlayerRepository;
import com.interview.king_exercise.service.exception.EvaluationConflictException;
import com.interview.king_exercise.service.exception.EvaluationNotFoundException;
import com.interview.king_exercise.service.exception.UnknownPlayerException;

class CampaignEvaluationServiceTest {

    private CampaignEvaluationService service;

    @BeforeEach
    void setUp() {
        service = new CampaignEvaluationService(
                new CampaignEvaluationRepository(),
                new PlayerRepository());
    }

    @Test
    @DisplayName("Creates an evaluation with eligibility results")
    void shouldCreateEvaluation() {
        CampaignEvaluationResponse response = service.createCampaignEvaluation(request(
                "eval-1", List.of("p-42", "p-77")));

        assertThat(response.evaluationId()).isEqualTo("eval-1");
        assertThat(response.campaignId()).isEqualTo("summer-boost");
        assertThat(response.results()).hasSize(2);
        assertThat(response.results().get(0).playerId()).isEqualTo("p-42");
        assertThat(response.results().get(0).eligible()).isTrue();
        assertThat(response.results().get(1).playerId()).isEqualTo("p-77");
        assertThat(response.results().get(1).eligible()).isFalse();
    }

    @Test
    @DisplayName("Rejects unknown players")
    void shouldRejectUnknownPlayers() {
        CampaignEvaluationRequest request = request(
                "eval-1", List.of("p-42", "p-999", "p-404"));

        assertThatThrownBy(() -> service.createCampaignEvaluation(request))
                .isInstanceOf(UnknownPlayerException.class)
                .hasMessage("Players not found in fixture data: p-999, p-404");
    }

    @Test
    @DisplayName("Returns an existing evaluation")
    void shouldGetExistingEvaluation() {
        service.createCampaignEvaluation(request("eval-1", List.of("p-42")));

        CampaignEvaluationResponse response = service.getCampaignEvaluation("eval-1");

        assertThat(response.evaluationId()).isEqualTo("eval-1");
        assertThat(response.results()).singleElement()
                .extracting(com.interview.king_exercise.api.dto.PlayerResultDto::playerId)
                .isEqualTo("p-42");
    }

    @Test
    @DisplayName("Rejects an unknown evaluation id")
    void shouldRejectMissingEvaluation() {
        assertThatThrownBy(() -> service.getCampaignEvaluation("missing"))
                .isInstanceOf(EvaluationNotFoundException.class)
                .hasMessage("Campaign evaluation not found for ID: missing");
    }

    @Test
    @DisplayName("Accepts the same evaluation more than once")
    void shouldBeIdempotentForSameEvaluation() {
        CampaignEvaluationRequest request = request("eval-1", List.of("p-42"));

        CampaignEvaluationResponse first = service.createCampaignEvaluation(request);
        CampaignEvaluationResponse second = service.createCampaignEvaluation(request);

        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("Rejects the same id with different players")
    void shouldRejectDifferentPlayersForExistingId() {
        service.createCampaignEvaluation(request("eval-1", List.of("p-42")));
        CampaignEvaluationRequest differentPlayers = request("eval-1", List.of("p-77"));

        assertThatThrownBy(() -> service.createCampaignEvaluation(differentPlayers))
                .isInstanceOf(EvaluationConflictException.class)
                .hasMessage("An evaluation with ID eval-1 already exists with different rules or player IDs.");
    }

    @Test
    @DisplayName("Rejects the same id with different rules")
    void shouldRejectDifferentRulesForExistingId() {
        service.createCampaignEvaluation(request("eval-1", List.of("p-42")));
        CampaignEvaluationRequest differentRules = new CampaignEvaluationRequest(
                "eval-1",
                new CampaignRulesDto("winter-boost", 10, List.of("ES", "SE")),
                List.of("p-42"));

        assertThatThrownBy(() -> service.createCampaignEvaluation(differentRules))
                .isInstanceOf(EvaluationConflictException.class);
    }

    private CampaignEvaluationRequest request(String evaluationId, List<String> playerIds) {
        return new CampaignEvaluationRequest(
                evaluationId,
                new CampaignRulesDto("summer-boost", 10, List.of("ES", "SE")),
                playerIds);
    }
}
