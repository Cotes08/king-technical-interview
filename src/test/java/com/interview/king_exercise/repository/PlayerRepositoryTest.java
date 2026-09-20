package com.interview.king_exercise.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.interview.king_exercise.domain.PlayerProfile;

class PlayerRepositoryTest {

    private PlayerRepository repository;

    @BeforeEach
    void setUp() {
        repository = new PlayerRepository();
    }

    @Test
    @DisplayName("Should find a existing player")
    void shouldFindExistingPlayer() {
        Optional<PlayerProfile> player = repository.findBy("p-42");

        assertThat(player).isPresent();
        assertThat(player.get().country()).isEqualTo("ES");
        assertThat(player.get().level()).isEqualTo(18);
    }

    @Test
    @DisplayName("Should return empty for a unknown player")
    void shouldReturnEmptyForUnknownPlayer() {
        Optional<PlayerProfile> player = repository.findBy("p-999");

        assertThat(player).isEmpty();
    }

    @Test
    @DisplayName("Should check if the 6 players are loaded at the start")
    void shouldLoadAllFixturePlayers() {
        Collection<PlayerProfile> players = repository.getPlayerProfiles();

        assertThat(players).hasSize(6);
    }

}
