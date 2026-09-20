package com.interview.king_exercise.domain;

import java.util.List;

public record CampaignRules(
        String campaignId,
        int minimumLevel,
        List<String> countries) {

    public boolean isPlayerEligible(PlayerProfile player) {
        return player.level() >= minimumLevel && countries.contains(player.country());
    }
}
