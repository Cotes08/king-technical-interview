package com.interview.king_exercise.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.interview.king_exercise.api.dto.CampaignEvaluationResponse;
import com.interview.king_exercise.api.dto.PlayerResultDto;
import com.interview.king_exercise.api.error.GlobalExceptionHandler;
import com.interview.king_exercise.service.CampaignEvaluationService;
import com.interview.king_exercise.service.exception.EvaluationConflictException;
import com.interview.king_exercise.service.exception.EvaluationNotFoundException;
import com.interview.king_exercise.service.exception.UnknownPlayerException;

import static org.mockito.Mockito.mock;

class CampaignEvaluationControllerTest {

    private MockMvc mockMvc;
    private CampaignEvaluationService service;

    @BeforeEach
    void setUp() {
        service = mock(CampaignEvaluationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new CampaignEvaluationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldCreateEvaluation() throws Exception {
        CampaignEvaluationResponse response = response();
        when(service.createCampaignEvaluation(any())).thenReturn(response);

        mockMvc.perform(post("/campaign-evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.evaluationId").value("eval-1"))
                .andExpect(jsonPath("$.campaignId").value("summer-boost"))
                .andExpect(jsonPath("$.results[0].playerId").value("p-42"))
                .andExpect(jsonPath("$.results[0].eligible").value(true));
    }

    @Test
    void shouldGetEvaluation() throws Exception {
        when(service.getCampaignEvaluation("eval-1")).thenReturn(response());

        mockMvc.perform(get("/campaign-evaluations/eval-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").value("eval-1"))
                .andExpect(jsonPath("$.results[0].eligible").value(true));
    }

    @Test
    void shouldReturnBadRequestForInvalidRequest() throws Exception {
        String invalidJson = """
                {
                  "evaluationId": "",
                  "campaignRules": {
                    "campaignId": "summer-boost",
                    "minimumLevel": 10,
                    "countries": ["ES"]
                  },
                  "playerIds": []
                }
                """;

        mockMvc.perform(post("/campaign-evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void shouldReturnNotFoundWhenEvaluationDoesNotExist() throws Exception {
        when(service.getCampaignEvaluation("missing"))
                .thenThrow(new EvaluationNotFoundException("missing"));

        mockMvc.perform(get("/campaign-evaluations/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Campaign evaluation not found for ID: missing"));
    }

    @Test
    void shouldReturnBadRequestForUnknownPlayer() throws Exception {
        when(service.createCampaignEvaluation(any()))
                .thenThrow(new UnknownPlayerException(List.of("p-999")));

        mockMvc.perform(post("/campaign-evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Players not found in fixture data: p-999"));
    }

    @Test
    void shouldReturnConflictForDuplicateWithDifferentData() throws Exception {
        when(service.createCampaignEvaluation(any()))
                .thenThrow(new EvaluationConflictException("eval-1"));

        mockMvc.perform(post("/campaign-evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private CampaignEvaluationResponse response() {
        return new CampaignEvaluationResponse(
                "eval-1",
                "summer-boost",
                List.of(new PlayerResultDto("p-42", true)));
    }

    private String validRequestJson() {
        return """
                {
                  "evaluationId": "eval-1",
                  "campaignRules": {
                    "campaignId": "summer-boost",
                    "minimumLevel": 10,
                    "countries": ["ES", "SE"]
                  },
                  "playerIds": ["p-42"]
                }
                """;
    }
}
