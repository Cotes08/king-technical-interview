package com.interview.king_exercise.domain;

import java.time.Instant;

public record PlayerProfile(
                String playerId,
                String country,
                int level,
                Instant lastActiveAt) {
}
