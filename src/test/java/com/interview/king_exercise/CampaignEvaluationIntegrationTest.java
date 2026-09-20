package com.interview.king_exercise;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CampaignEvaluationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldCreateAndGetCampaignEvaluationThroughApplication() throws Exception {
        String request = """
                {
                  "evaluationId": "integration-evaluation-1",
                  "campaignRules": {
                    "campaignId": "summer-boost",
                    "minimumLevel": 10,
                    "countries": ["ES", "SE"]
                  },
                  "playerIds": ["p-42", "p-77"]
                }
                """;

        mockMvc.perform(post("/campaign-evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").value("integration-evaluation-1"))
                .andExpect(jsonPath("$.campaignId").value("summer-boost"))
                .andExpect(jsonPath("$.results[0].playerId").value("p-42"))
                .andExpect(jsonPath("$.results[0].eligible").value(true))
                .andExpect(jsonPath("$.results[1].playerId").value("p-77"))
                .andExpect(jsonPath("$.results[1].eligible").value(false));

        mockMvc.perform(get("/campaign-evaluations/integration-evaluation-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.evaluationId").value("integration-evaluation-1"))
                .andExpect(jsonPath("$.results").isArray())
                .andExpect(jsonPath("$.results").isNotEmpty());
    }

    @Test
    void shouldReturnBadRequestForUnknownPlayerThroughApplication() throws Exception {
        String request = """
                {
                  "evaluationId": "integration-evaluation-unknown-player",
                  "campaignRules": {
                    "campaignId": "summer-boost",
                    "minimumLevel": 10,
                    "countries": ["ES", "SE"]
                  },
                  "playerIds": ["p-999"]
                }
                """;

        mockMvc.perform(post("/campaign-evaluations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message")
                        .value("Players not found in fixture data: p-999"));
    }
}
