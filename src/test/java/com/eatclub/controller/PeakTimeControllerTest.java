package com.eatclub.controller;

import com.eatclub.client.ChallengeDataClient;
import com.eatclub.model.source.ChallengeData;
import com.eatclub.model.source.SourceDeal;
import com.eatclub.model.source.SourceRestaurant;
import com.eatclub.service.DealService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DealController.class)
@Import(DealService.class)
class PeakTimeControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ChallengeDataClient dataClient;

    @Test
    void findsTheWindowWhereThreeDealsOverlap() throws Exception {
        givenRestaurants(
                restaurant("r1", "10:00am", "2:00pm", deal("a")),
                restaurant("r2", "12:00pm", "3:00pm", deal("b")),
                restaurant("r3", "1:00pm", "4:00pm", deal("c")));

        expectPeak("13:00", "14:00");
    }

    @Test
    void keepsThePeakContinuousWhenOneDealEndsAndAnotherStarts() throws Exception {
        givenRestaurants(
                restaurant("r1", "6:00pm", "9:00pm", deal("a")),
                restaurant("r2", "7:00pm", "8:00pm", deal("b")),
                restaurant("r3", "8:00pm", "9:00pm", deal("c")));

        expectPeak("19:00", "21:00");
    }

    @Test
    void picksTheEarliestStartWhenSeparatePeaksTie() throws Exception {
        givenRestaurants(
                restaurant("later", "6:00pm", "9:00pm", deal("a")),
                restaurant("earlier", "10:00am", "11:00am", deal("b")));

        expectPeak("10:00", "11:00");
    }

    @Test
    void limitsADealToTheRestaurantsHours() throws Exception {
        var deal = new SourceDeal("a", "30", "true", "false", "3",
                "3:00pm", "10:00pm", null, null);
        givenRestaurants(restaurant("r1", "6:00pm", "9:00pm", deal));

        expectPeak("18:00", "21:00");
    }

    @Test
    void joinsAnOvernightPeakAcrossMidnight() throws Exception {
        var deal = new SourceDeal("a", "30", "true", "false", "3",
                null, null, "11:00pm", "3:00am");
        givenRestaurants(restaurant("r1", "10:00pm", "2:00am", deal));

        expectPeak("23:00", "02:00");
    }

    @Test
    void excludesSoldOutDealsFromThePeakCount() throws Exception {
        var soldOut = new SourceDeal("sold-out", "30", "true", "false", "0",
                null, null, "7:00pm", "8:00pm");
        givenRestaurants(restaurant("r1", "6:00pm", "9:00pm", deal("available"), soldOut));

        expectPeak("18:00", "21:00");
    }

    @Test
    void countsIdenticalWindowsWithDuplicateDealIdsSeparately() throws Exception {
        givenRestaurants(
                restaurant("earlier", "10:00am", "11:00am", deal("a")),
                restaurant("r1", "6:00pm", "9:00pm", deal("shared")),
                restaurant("r2", "6:00pm", "9:00pm", deal("shared")));

        expectPeak("18:00", "21:00");
    }

    @Test
    void returnsNullBoundariesWhenThereAreNoDeals() throws Exception {
        givenRestaurants(restaurant("r1", "6:00pm", "9:00pm"));

        mvc.perform(get("/deals/peak"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"peakTimeStart": null, "peakTimeEnd": null}
                        """, JsonCompareMode.STRICT));
    }

    private void expectPeak(String start, String end) throws Exception {
        mvc.perform(get("/deals/peak"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {"peakTimeStart": "%s", "peakTimeEnd": "%s"}
                        """.formatted(start, end), JsonCompareMode.STRICT));
    }

    private void givenRestaurants(SourceRestaurant... restaurants) {
        when(dataClient.fetch()).thenReturn(new ChallengeData(List.of(restaurants)));
    }

    private SourceRestaurant restaurant(String id, String open, String close, SourceDeal... deals) {
        return new SourceRestaurant(id, "Restaurant " + id, "1 Main Street", "Sydney",
                open, close, List.of(deals));
    }

    private SourceDeal deal(String id) {
        return new SourceDeal(id, "30", "true", "false", "3", null, null, null, null);
    }
}
