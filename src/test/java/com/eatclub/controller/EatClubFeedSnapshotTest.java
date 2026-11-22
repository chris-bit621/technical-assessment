package com.eatclub.controller;

import com.eatclub.client.ChallengeDataClient;
import com.eatclub.model.source.ChallengeData;
import com.eatclub.service.DealService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DealController.class)
@Import(DealService.class)
class EatClubFeedSnapshotTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChallengeDataClient dataClient;

    @BeforeEach
    void loadFeedSnapshot() throws IOException {
        // Snapshot of https://eccdn.com.au/misc/challengedata.json
        var fixture = new ClassPathResource("fixtures/eatclub-challenge-data.json");
        try (var input = fixture.getInputStream()) {
            when(dataClient.fetch()).thenReturn(objectMapper.readValue(input, ChallengeData.class));
        }
    }

    @ParameterizedTest
    @MethodSource("expectedActiveDeals")
    void returnsActiveDealsFromTheSnapshot(
            String timeOfDay, Map<String, Integer> expectedByRestaurant) throws Exception {
        var result = mvc.perform(get("/deals").param("timeOfDay", timeOfDay))
                .andExpect(status().isOk())
                .andReturn();

        var deals = objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("deals");
        assertThat(deals.isArray()).isTrue();

        Map<String, Integer> actualByRestaurant = new HashMap<>();
        for (var deal : deals) {
            actualByRestaurant.merge(deal.path("restaurantName").asText(), 1, Integer::sum);
        }

        assertThat(actualByRestaurant).isEqualTo(expectedByRestaurant);
    }

    static Stream<Arguments> expectedActiveDeals() {
        return Stream.of(
                Arguments.of("3:00pm", Map.of("Masala Kitchen", 2, "ABC Chicken", 2, "Kekou", 1)),
                Arguments.of("6:00pm", Map.of(
                        "Masala Kitchen", 2, "ABC Chicken", 2, "Vrindavan", 1, "Kekou", 2,
                        "Gyoza Gyoza Melbourne Central", 2)),
                Arguments.of("9:00pm", Map.of("ABC Chicken", 2, "Gyoza Gyoza Melbourne Central", 2))
        );
    }

    @Test
    void returnsTheInclusivePeakFromTheSnapshot() throws Exception {
        mvc.perform(get("/deals/peak"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"peakTimeStart": "18:00", "peakTimeEnd": "20:59"}
                        """, JsonCompareMode.STRICT));
    }
}
