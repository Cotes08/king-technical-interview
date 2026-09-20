package com.interview.king_exercise.api.dto;

import java.util.List;
import java.util.Locale;

import com.interview.king_exercise.domain.CampaignRules;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CampaignRulesDto(
                @NotBlank(message = "campaignId is required") String campaignId,
                @NotNull(message = "minimumLevel is required") @Min(value = 1, message = "minimumLevel must be at least 1") Integer minimumLevel,
                @NotEmpty(message = "countries cannot be empty") List<@NotBlank(message = "country code cannot be blank") String> countries) {

        public CampaignRules toDomain() {
                List<String> normalizedCountries = countries.stream()
                                .map(country -> country.trim().toUpperCase(Locale.ROOT))
                                .toList();
                return new CampaignRules(campaignId, minimumLevel, normalizedCountries);
        }

}
