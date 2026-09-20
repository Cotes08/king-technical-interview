package com.interview.king_exercise.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.interview.king_exercise.domain.CampaignRules;
import com.interview.king_exercise.domain.StoredEvaluation;

class CampaignEvaluationRepositoryTest {

    private CampaignEvaluationRepository repository;
    private CampaignRules defaultRules;

    @BeforeEach
    void setUp() {
        repository = new CampaignEvaluationRepository();
        defaultRules = new CampaignRules("summer-boost", 10, List.of("ES", "SE"));
    }

    private StoredEvaluation storedEvaluation(String evaluationId, CampaignRules campaignRules,
            List<String> playerList) {
        return new StoredEvaluation(evaluationId, campaignRules, playerList, Instant.parse("2026-08-21T09:00:00Z"));
    }

    @Test
    @DisplayName("Save a non existing evaluation")
    void shouldSaveWhenAbsent() {
        StoredEvaluation evaluation = storedEvaluation("eval-1", defaultRules, List.of("p-1"));
        Optional<StoredEvaluation> result = repository.saveIfAbsent(evaluation);

        assertThat(result).isEmpty();
        assertThat(repository.findByEvaluationId("eval-1")).contains(evaluation);
    }

    @Test
    @DisplayName("Should return a existing evaluation if present")
    void shouldReturnEvaluationWhenPresent() {
        StoredEvaluation original = storedEvaluation("eval-1", defaultRules, List.of("p-1"));
        StoredEvaluation duplicate = storedEvaluation("eval-1", defaultRules, List.of("p-2"));

        repository.saveIfAbsent(original);
        Optional<StoredEvaluation> result = repository.saveIfAbsent(duplicate);

        assertThat(result).isPresent().contains(original);
        assertThat(repository.findByEvaluationId("eval-1")).contains(original);
    }

    @Test
    @DisplayName("Ensures atomicity under concurrent saves with the same id")
    void shouldHandleConcurrentInsertsSafely() throws InterruptedException {
        int threadCount = 10;

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);
        List<Optional<StoredEvaluation>> results = new CopyOnWriteArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startGate.await();
                    StoredEvaluation eval = new StoredEvaluation(
                            "eval-concurrent", defaultRules, List.of("p-42"), Instant.now());
                    results.add(repository.saveIfAbsent(eval));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endGate.countDown();
                }
            });
        }

        startGate.countDown();
        boolean finished = endGate.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished)
                .as("all threads should finish within the timeout")
                .isTrue();

        long winners = results.stream().filter(Optional::isEmpty).count();
        assertThat(winners)
                .as("exactly one thread should have stored the evaluation")
                .isEqualTo(1);

        List<Optional<StoredEvaluation>> losers = results.stream()
                .filter(Optional::isPresent)
                .toList();
        assertThat(losers)
                .as("the remaining threads should have received the stored value")
                .hasSize(threadCount - 1);

        StoredEvaluation stored = repository.findByEvaluationId("eval-concurrent").orElseThrow();
        assertThat(losers)
                .allSatisfy(opt -> assertThat(opt).contains(stored));
    }

}
