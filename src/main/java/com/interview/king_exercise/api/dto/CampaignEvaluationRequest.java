package com.interview.king_exercise.api.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CampaignEvaluationRequest(
                @NotBlank(message = "evaluationId is required") String evaluationId,

                @NotNull(message = "campaignRules is required") @Valid CampaignRulesDto campaignRules,

                @NotEmpty(message = "playerIds cannot be empty") List<@NotBlank(message = "player id cannot be blank") String> playerIds) {
}
