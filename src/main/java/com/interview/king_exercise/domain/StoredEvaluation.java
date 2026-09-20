package com.interview.king_exercise.domain;

import java.time.Instant;
import java.util.List;

public record StoredEvaluation(
                String evaluationId,
                CampaignRules campaignRules,
                List<String> playerIds,
                Instant storedTime) {

}
