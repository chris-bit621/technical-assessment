package com.eatclub.model.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Deal(
        String restaurantObjectId,
        String restaurantName,
        String restaurantAddress1,
        @JsonProperty("restarantSuburb") String restaurantSuburb,
        String restaurantOpen,
        String restaurantClose,
        String dealObjectId,
        String discount,
        String dineIn,
        String lightning,
        String qtyLeft
) {
}
