package com.interview.king_exercise.domain;

import java.util.List;

public record CampaignRules(
        String campaignId,
        int minimumLevel,
        List<String> countries) {

    public boolean isPlayerEligible(PlayerProfile player) {
        if (player.level() < this.minimumLevel)
            return false;
        if (!countries.contains(player.country()))
            return false;

        return true;
    }
}
