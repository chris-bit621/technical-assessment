package com.eatclub.controller;

import com.eatclub.client.ChallengeDataClient;
import com.eatclub.exception.ChallengeDataException;
import com.eatclub.model.source.ChallengeData;
import com.eatclub.model.source.SourceDeal;
import com.eatclub.model.source.SourceRestaurant;
import com.eatclub.service.DealService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DealController.class)
@Import(DealService.class)
class DealControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChallengeDataClient dataClient;

    @ParameterizedTest
    @ValueSource(strings = {"15:00", "3:00pm", "3:00PM", " 3:00PM "})
    void returnsThePublicContractWithStringValues(String timeOfDay) throws Exception {
        var deal = new SourceDeal("d1", "30", "true", "false", "3", null, null, null, null);
        var restaurant = new SourceRestaurant("r1", "Test Kitchen", "1 Main Street", "Sydney",
                "3:00pm", "9:00pm", List.of(deal));
        when(dataClient.fetch()).thenReturn(new ChallengeData(List.of(restaurant)));

        mvc.perform(get("/deals").param("timeOfDay", timeOfDay))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "deals": [
                            {
                              "restaurantObjectId": "r1",
                              "restaurantName": "Test Kitchen",
                              "restaurantAddress1": "1 Main Street",
                              "restarantSuburb": "Sydney",
                              "restaurantOpen": "3:00pm",
                              "restaurantClose": "9:00pm",
                              "dealObjectId": "d1",
                              "discount": "30",
                              "dineIn": "true",
                              "lightning": "false",
                              "qtyLeft": "3"
                            }
                          ]
                        }
                        """, JsonCompareMode.STRICT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "25:00"})
    void sendsBadRequestForInvalidTimeBeforeFetchingTheFeed(String timeOfDay) throws Exception {
        mvc.perform(get("/deals").param("timeOfDay", timeOfDay))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(dataClient);
    }

    @Test
    void sendsBadRequestForMissingTime() throws Exception {
        mvc.perform(get("/deals")).andExpect(status().isBadRequest());
        verifyNoInteractions(dataClient);
    }

    @Test
    void sendsBadGatewayWithoutExposingTheUpstreamException() throws Exception {
        when(dataClient.fetch()).thenThrow(new ChallengeDataException("Internal feed error"));

        mvc.perform(get("/deals").param("timeOfDay", "15:00"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Restaurant deal data is currently unavailable."));
    }
}
