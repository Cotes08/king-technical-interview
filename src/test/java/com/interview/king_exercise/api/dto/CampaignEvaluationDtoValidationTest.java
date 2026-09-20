package com.interview.king_exercise.api.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.interview.king_exercise.domain.CampaignRules;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

class CampaignEvaluationDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Valid evaluation request has no violations")
    void shouldAcceptValidEvaluationRequest() {
        CampaignEvaluationRequest request = new CampaignEvaluationRequest(
                "eval-1",
                validCampaignRules(),
                List.of("p-42", "p-77"));

        Set<ConstraintViolation<CampaignEvaluationRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Evaluation request rejects invalid top-level fields")
    void shouldRejectInvalidEvaluationRequestFields() {
        CampaignEvaluationRequest request = new CampaignEvaluationRequest(
                "",
                null,
                List.of(""));

        Set<ConstraintViolation<CampaignEvaluationRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder("evaluationId", "campaignRules", "playerIds[0].<list element>");
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactlyInAnyOrder(
                        "evaluationId is required",
                        "campaignRules is required",
                        "player id cannot be blank");
    }

    @Test
    @DisplayName("Evaluation request rejects an empty player list")
    void shouldRejectEmptyPlayerList() {
        CampaignEvaluationRequest request = new CampaignEvaluationRequest(
                "eval-1",
                validCampaignRules(),
                List.of());

        Set<ConstraintViolation<CampaignEvaluationRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsExactly("playerIds cannot be empty");
    }

    @Test
    @DisplayName("Nested campaign rules are validated")
    void shouldRejectInvalidNestedCampaignRules() {
        CampaignEvaluationRequest request = new CampaignEvaluationRequest(
                "eval-1",
                new CampaignRulesDto("", 0, List.of("")),
                List.of("p-42"));

        Set<ConstraintViolation<CampaignEvaluationRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactlyInAnyOrder(
                        "campaignRules.campaignId",
                        "campaignRules.minimumLevel",
                        "campaignRules.countries[0].<list element>");
    }

    @Test
    @DisplayName("Campaign rules DTO converts to domain rules")
    void shouldConvertCampaignRulesToDomain() {
        CampaignRulesDto dto = validCampaignRules();

        CampaignRules result = dto.toDomain();

        assertThat(result)
                .isEqualTo(new CampaignRules("summer-boost", 10, List.of("ES", "SE")));
    }

    private CampaignRulesDto validCampaignRules() {
        return new CampaignRulesDto("summer-boost", 10, List.of("ES", "SE"));
    }
}
