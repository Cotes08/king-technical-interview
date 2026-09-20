package com.interview.king_exercise.api.dto;

import java.util.List;

public record CampaignEvaluationResponse(
        String evaluationId,
        String campaignId,
        List<PlayerResultDto> results) {

}
