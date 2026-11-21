package com.eatclub.service;

import com.eatclub.client.ChallengeDataClient;
import com.eatclub.model.api.DealsResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DealService {

    private final ChallengeDataClient dataClient;

    public DealService(ChallengeDataClient dataClient) {
        this.dataClient = dataClient;
    }

    public DealsResponse activeDeals(String timeOfDay) {
        // TODO: Parse timeOfDay, fetch the feed through dataClient, and return the matching deals.
        return new DealsResponse(List.of());
    }
}
