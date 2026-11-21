package com.eatclub.service;

import com.eatclub.exception.ChallengeDataException;
import com.eatclub.model.api.DealResponse;
import com.eatclub.model.source.ChallengeData;
import com.eatclub.model.source.SourceDeal;
import com.eatclub.model.source.SourceRestaurant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DealServiceTest {

    @Test
    void inheritsRestaurantHoursAndIncludesDealsWithZeroQtyLeft() {
        var deal = new SourceDeal("original", "30", "true", "false", "0", null, null, null, null);
        var service = service(restaurant("r1", "3:00pm", "9:00pm", deal));

        assertThat(active(service, "14:59")).isEmpty();
        assertThat(active(service, "15:00")).extracting(DealResponse::dealObjectId).containsExactly("original");
        assertThat(active(service, "20:59")).hasSize(1);
        assertThat(active(service, "21:00")).isEmpty();
    }

    @Test
    void appliesWindowPrecedenceAndClipsEachDealToItsRestaurant() {
        var byOpenClose = new SourceDeal("preferred", "30", "true", "false", "3",
                "3:00pm", "7:00pm", "7:00pm", "10:00pm");
        var byStartEnd = new SourceDeal("fallback", "30", "true", "false", "3",
                "3:00pm", null, "5:00pm", "8:00pm");
        var service = service(restaurant("r1", "6:00pm", "10:00pm", byOpenClose, byStartEnd));

        assertThat(active(service, "17:59")).isEmpty();
        assertThat(active(service, "18:00")).extracting(DealResponse::dealObjectId)
                .containsExactly("preferred", "fallback");
        assertThat(active(service, "19:00")).extracting(DealResponse::dealObjectId).containsExactly("fallback");
        assertThat(active(service, "20:00")).isEmpty();
    }

    @Test
    void preservesDuplicateDealIdsAcrossRestaurants() {
        var deal = new SourceDeal("shared", "30", "true", "false", "3", null, null, null, null);
        var service = service(restaurant("r1", "6:00pm", "9:00pm", deal),
                restaurant("r2", "5:00pm", "8:00pm", deal));

        assertThat(active(service, "18:00")).extracting(DealResponse::restaurantObjectId)
                .containsExactly("r1", "r2");
    }

    @Test
    void intersectsOvernightHoursOnBothSidesOfMidnight() {
        var deal = new SourceDeal("night", "30", "true", "false", "3", null, null, "11:00pm", "3:00am");
        var service = service(restaurant("r1", "10:00pm", "2:00am", deal));

        assertThat(active(service, "22:59")).isEmpty();
        assertThat(active(service, "23:00")).hasSize(1);
        assertThat(active(service, "00:00")).hasSize(1);
        assertThat(active(service, "02:00")).isEmpty();
    }

    @Test
    void treatsInvalidSourceTimesAsUpstreamErrors() {
        var service = service(restaurant("r1", "bad time", "9:00pm"));

        assertThatThrownBy(() -> active(service, "18:00")).isInstanceOf(ChallengeDataException.class);
    }

    private List<DealResponse> active(DealService service, String time) {
        return service.activeDeals(time).deals();
    }

    private DealService service(SourceRestaurant... restaurants) {
        return new DealService(() -> new ChallengeData(List.of(restaurants)));
    }

    private SourceRestaurant restaurant(String id, String open, String close, SourceDeal... deals) {
        return new SourceRestaurant(id, "Restaurant " + id, "1 Main Street", "Sydney", open, close, List.of(deals));
    }
}
