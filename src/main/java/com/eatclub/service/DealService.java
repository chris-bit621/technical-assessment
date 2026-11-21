package com.eatclub.service;

import com.eatclub.client.ChallengeDataClient;
import com.eatclub.exception.ChallengeDataException;
import com.eatclub.model.api.Deal;
import com.eatclub.model.api.DealsResponse;
import com.eatclub.model.source.SourceDeal;
import com.eatclub.model.source.SourceRestaurant;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

import static com.eatclub.time.TimeParser.parseTime;

@Service
public class DealService {

    private final ChallengeDataClient dataClient;

    public DealService(ChallengeDataClient dataClient) {
        this.dataClient = dataClient;
    }

    public DealsResponse activeDeals(LocalTime requestedTime) {

        var restaurants = dataClient.fetch().restaurants().stream();

        var dealsResponse = restaurants.flatMap(restaurant -> {
            if (restaurant == null || restaurant.deals() == null) {
                throw new ChallengeDataException("Data is missing restaurant details");
            }

            if (!hasTime(restaurant.open()) || !hasTime(restaurant.close())) {
                throw new ChallengeDataException("Missing restaurant opening or closing time");
            }

            LocalTime open;
            LocalTime close;
            try {
                open = parseTime(restaurant.open());
                close = parseTime(restaurant.close());
            } catch (DateTimeParseException e) {
                throw new ChallengeDataException("Invalid restaurant opening or closing time", e);
            }
            //If the deals list is empty and the restaurants time is badly formatted , it would never throw the exception
            //when it was in the activeDeal check.
            if (!isWithinTimeWindow(requestedTime, open, close)) {
                return Stream.empty();
            }

            return restaurant.deals().stream()
                    .filter(deal -> isActiveDeal(deal, requestedTime))
                    .map(sourceDeal -> toDealResponse(restaurant, sourceDeal) );
        });

        return new DealsResponse(dealsResponse.toList());
    }

    private boolean isActiveDeal(SourceDeal deal, LocalTime requestedTime) {
        if (Integer.parseInt(deal.qtyLeft()) <= 0) {
            return false;
        }

        String dealStart;
        String dealEnd;
        if (hasTime(deal.open()) && hasTime(deal.close())) {
            dealStart = deal.open();
            dealEnd = deal.close();
        } else if (hasTime(deal.start()) && hasTime(deal.end())) {
            dealStart = deal.start();
            dealEnd = deal.end();
        } else {
            // No deal-specific hours, so the restaurant's hours apply.
            return true;
        }

        return isWithinTimeWindow(requestedTime, parseTime(dealStart), parseTime(dealEnd));
    }

    private boolean isWithinTimeWindow(LocalTime requestedTime, LocalTime start, LocalTime end) {
        boolean atOrAfterStart = !requestedTime.isBefore(start);
        boolean beforeEnd = requestedTime.isBefore(end);
        // Case 1, the time rolls over midnight.
        if (end.isBefore(start)) {
            return atOrAfterStart || beforeEnd;
        }
        //Case 2, both times are on the same day.
        return atOrAfterStart && beforeEnd;
    }

    private boolean hasTime(String value) {
        return value != null && !value.isBlank();
    }

    private Deal toDealResponse(SourceRestaurant restaurant, SourceDeal deal) {
        return new Deal(
                restaurant.objectId(),
                restaurant.name(),
                restaurant.address1(),
                restaurant.suburb(),
                restaurant.open(),
                restaurant.close(),
                deal.objectId(),
                deal.discount(),
                deal.dineIn(),
                deal.lightning(),
                deal.qtyLeft()
        );
    }


}
