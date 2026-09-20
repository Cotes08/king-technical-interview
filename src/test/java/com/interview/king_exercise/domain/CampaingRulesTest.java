package com.interview.king_exercise.domain;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CampaignRulesTest {

    private CampaignRules campaignRules;

    @BeforeEach
    void setUp() {
        campaignRules = new CampaignRules("summer-boost", 10, List.of("ES", "SE"));
    }

    private PlayerProfile player(String country, int level) {
        return new PlayerProfile("p-test", country, level, Instant.parse("2026-08-21T09:00:00Z"));
    }

    @Nested
    @DisplayName("Player contains a valid country")
    class WhenCountryIsValid {

        @Test
        @DisplayName("Player level is above the minimum required level")
        void shouldReturnTrueWhenLevelIsAboveMinimum() {
            PlayerProfile player = player("ES", 18);
            assertThat(campaignRules.isPlayerEligible(player)).isTrue();
        }

        @Test
        @DisplayName("Player level is exactly the minimum required level.")
        void shouldReturnTrueWhenLevelIsExactlyMinimum() {
            PlayerProfile player = player("ES", 10);
            assertThat(campaignRules.isPlayerEligible(player)).isTrue();
        }

        @Test
        @DisplayName("Player level is below the minimum required level.")
        void shouldReturnFalseWhenLevelIsBelowMinimum() {
            PlayerProfile player = player("SE", 6);
            assertThat(campaignRules.isPlayerEligible(player)).isFalse();
        }
    }

    @Nested
    @DisplayName("Player contains a not valid country")
    class WhenCountryIsNotValid {

        @Test
        @DisplayName("Player level is above the minimum required level")
        void shouldReturnFalseWhenLevelIsAboveMinimum() {
            PlayerProfile player = player("FR", 20);
            assertThat(campaignRules.isPlayerEligible(player)).isFalse();
        }

        @Test
        @DisplayName("Player level is below the minimum required level")
        void shouldReturnFalseWhenLevelIsBelowMinimum() {
            PlayerProfile p = player("DE", 9);
            assertThat(campaignRules.isPlayerEligible(p)).isFalse();
        }
    }
}