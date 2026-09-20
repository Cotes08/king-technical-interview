package com.interview.king_exercise.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.interview.king_exercise.api.dto.CampaignEvaluationRequest;
import com.interview.king_exercise.api.dto.CampaignEvaluationResponse;
import com.interview.king_exercise.service.CampaignEvaluationService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping(value = "/campaign-evaluations", produces = MediaType.APPLICATION_JSON_VALUE)
public class CampaignEvaluationController {

    private final CampaignEvaluationService campaignEvaluationService;

    public CampaignEvaluationController(CampaignEvaluationService campaignEvaluationService) {
        this.campaignEvaluationService = campaignEvaluationService;
    }

    @PostMapping
    public CampaignEvaluationResponse postCampaignEvaluation(
            @Valid @RequestBody CampaignEvaluationRequest evaluationRequest) {
        return campaignEvaluationService.createCampaignEvaluation(evaluationRequest);
    }

    @GetMapping("/{evaluationId}")
    public CampaignEvaluationResponse getCampaignEvaluation(@PathVariable String evaluationId) {
        return campaignEvaluationService.getCampaignEvaluation(evaluationId);
    }

}
