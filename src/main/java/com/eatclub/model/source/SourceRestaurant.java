package com.eatclub.model.source;

import java.util.List;

public record SourceRestaurant(
        String objectId,
        String name,
        String address1,
        String suburb,
        String open,
        String close,
        List<SourceDeal> deals
) {
}
