package com.interview.king_exercise.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.interview.king_exercise.api.dto.CampaignEvaluationRequest;
import com.interview.king_exercise.api.dto.CampaignEvaluationResponse;
import com.interview.king_exercise.api.dto.PlayerResultDto;
import com.interview.king_exercise.domain.CampaignRules;
import com.interview.king_exercise.domain.PlayerProfile;
import com.interview.king_exercise.domain.StoredEvaluation;
import com.interview.king_exercise.repository.CampaignEvaluationRepository;
import com.interview.king_exercise.repository.PlayerRepository;
import com.interview.king_exercise.service.exception.EvaluationConflictException;
import com.interview.king_exercise.service.exception.EvaluationNotFoundException;
import com.interview.king_exercise.service.exception.UnknownPlayerException;

@Service
public class CampaignEvaluationService {

    private final CampaignEvaluationRepository evaluationRepository;
    private final PlayerRepository playerRepository;

    public CampaignEvaluationService(CampaignEvaluationRepository evaluationRepository,
            PlayerRepository playerRepository) {

        this.evaluationRepository = evaluationRepository;
        this.playerRepository = playerRepository;
    }

    public CampaignEvaluationResponse createCampaignEvaluation(CampaignEvaluationRequest request) {
        CampaignRules campaignRules = request.campaignRules().toDomain();

        StoredEvaluation storedEvaluation = new StoredEvaluation(
                request.evaluationId(),
                campaignRules,
                request.playerIds(),
                Instant.now());

        validatePlayers(request.playerIds());

        Optional<StoredEvaluation> optionalStorage = evaluationRepository
                .saveIfAbsent(storedEvaluation);

        if (optionalStorage.isPresent()
                && !isSameEvaluation(storedEvaluation, optionalStorage.get())) {
            throw new EvaluationConflictException(request.evaluationId());
        }

        return toResponse(optionalStorage.orElse(storedEvaluation));
    }

    public CampaignEvaluationResponse getCampaignEvaluation(String evaluationId) {
        StoredEvaluation storedEvaluation = evaluationRepository.findByEvaluationId(evaluationId)
                .orElseThrow(() -> new EvaluationNotFoundException(evaluationId));

        return toResponse(storedEvaluation);
    }

    private void validatePlayers(List<String> playerIds) {
        List<String> unknownPlayerIds = playerIds.stream()
                .filter(playerId -> playerRepository.findBy(playerId).isEmpty())
                .toList();

        if (!unknownPlayerIds.isEmpty()) {
            throw new UnknownPlayerException(unknownPlayerIds);
        }
    }

    private boolean isSameEvaluation(StoredEvaluation requestedEvaluation,
            StoredEvaluation storedEvaluation) {
        return requestedEvaluation.campaignRules().equals(storedEvaluation.campaignRules())
                && requestedEvaluation.playerIds().equals(storedEvaluation.playerIds());
    }

    private CampaignEvaluationResponse toResponse(StoredEvaluation storedEvaluation) {
        List<PlayerResultDto> playerResults = evaluatePlayers(
                storedEvaluation.playerIds(),
                storedEvaluation.campaignRules());

        return new CampaignEvaluationResponse(
                storedEvaluation.evaluationId(),
                storedEvaluation.campaignRules().campaignId(),
                playerResults);
    }

    private List<PlayerResultDto> evaluatePlayers(List<String> playerIds, CampaignRules campaignRules) {
        return playerIds.stream().map(playerId -> {
            PlayerProfile playerProfile = playerRepository.findBy(playerId).orElseThrow();
            return new PlayerResultDto(
                    playerId,
                    campaignRules.isPlayerEligible(playerProfile));
        }).toList();
    }

}
