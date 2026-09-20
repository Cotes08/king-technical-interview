package com.interview.king_exercise.repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.interview.king_exercise.domain.StoredEvaluation;

@Repository
public class CampaignEvaluationRepository {

    private final ConcurrentHashMap<String, StoredEvaluation> store = new ConcurrentHashMap<>();

    public Optional<StoredEvaluation> saveIfAbsent(StoredEvaluation storedEvaluation) {
        StoredEvaluation existing = store.putIfAbsent(storedEvaluation.evaluationId(),
                storedEvaluation);
        return Optional.ofNullable(existing);
    }

    public Optional<StoredEvaluation> findByEvaluationId(String evaluationId) {
        return Optional.ofNullable(store.get(evaluationId));
    }

}
