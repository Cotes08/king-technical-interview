package com.interview.king_exercise.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.interview.king_exercise.domain.PlayerProfile;

@Repository
public class PlayerRepository {

    private final Map<String, PlayerProfile> playerProfiles = new HashMap<>();

    public PlayerRepository() {
        List.of(new PlayerProfile("p-42", "ES", 18, Instant.parse("2026-08-21T09:00:00Z")),
                new PlayerProfile("p-77", "SE", 6, Instant.parse("2026-08-17T12:15:00Z")),
                new PlayerProfile("p-88", "ES", 10, Instant.parse("2026-08-20T20:30:00Z")),
                new PlayerProfile("p-99", "SE", 11, Instant.parse("2026-08-22T08:45:00Z")),
                new PlayerProfile("p-101", "FR", 20, Instant.parse("2026-08-22T14:10:00Z")),
                new PlayerProfile("p-102", "DE", 9, Instant.parse("2026-08-19T16:25:00Z")))
                .forEach(player -> playerProfiles.put(player.playerId(), player));

    }

    public Optional<PlayerProfile> findBy(String playerId) {
        return Optional.ofNullable(playerProfiles.get(playerId));
    }

    public Collection<PlayerProfile> getPlayerProfiles() {
        return playerProfiles.values();
    }

}
