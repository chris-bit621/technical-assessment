package com.eatclub.service;

import com.eatclub.client.ChallengeDataClient;
import com.eatclub.exception.ChallengeDataException;
import com.eatclub.model.api.Deal;
import com.eatclub.model.api.DealsResponse;
import com.eatclub.model.api.PeakTimeResponse;
import com.eatclub.model.source.SourceDeal;
import com.eatclub.model.source.SourceRestaurant;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.eatclub.time.TimeParser.parseTime;

@Service
public class DealService {

    private static final int MINUTES_IN_HOUR = 60;
    private static final int MINUTES_IN_DAY = 24 * MINUTES_IN_HOUR;

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

    public PeakTimeResponse peakTime() {
        var restaurants = dataClient.fetch().restaurants();
        int[] dealsCount = new int[MINUTES_IN_DAY];

        for (int minute = 0; minute < MINUTES_IN_DAY; minute++) {
            LocalTime requestedTime = LocalTime.of(minute / MINUTES_IN_HOUR, minute % MINUTES_IN_HOUR);
            dealsCount[minute] = countActiveDealsAt(restaurants, requestedTime);
        }

        int maxActive = 0;
        for (int count: dealsCount) {
            maxActive = Math.max(maxActive, count);
        }

        if (maxActive == 0) {
            return new PeakTimeResponse(null, null);
        }

        int peakStart = -1;
        for (int minute = 0; minute < MINUTES_IN_DAY; minute++) {
            int previousMinute = minute - 1;
            if (minute == 0) {
                previousMinute = MINUTES_IN_DAY - 1;
            }

            if (dealsCount[minute] == maxActive && dealsCount[previousMinute] < maxActive) {
                peakStart = minute;
                break;
            }
        }

        if (peakStart == -1) {
            return new PeakTimeResponse("00:00", "23:59");
        }

        int peakEnd = peakStart;
        int nextMinute = peakStart;
        while (dealsCount[nextMinute] == maxActive) {
            peakEnd = nextMinute;
            nextMinute++;
            if (nextMinute == MINUTES_IN_DAY) {
                nextMinute = 0;
            }
        }

        LocalTime start = LocalTime.of(peakStart / MINUTES_IN_HOUR, peakStart % MINUTES_IN_HOUR);
        LocalTime end = LocalTime.of(peakEnd / MINUTES_IN_HOUR, peakEnd % MINUTES_IN_HOUR);
        return new PeakTimeResponse(start.toString(), end.toString());
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

    private int countActiveDealsAt(List<SourceRestaurant> restaurants, LocalTime requestedTime) {
        int count = 0;

        for (SourceRestaurant restaurant : restaurants) {
            if (restaurant == null || restaurant.deals() == null) {
                throw new ChallengeDataException("Data is missing restaurant details");
            }

            if (!hasTime(restaurant.open()) || !hasTime(restaurant.close())) {
                throw new ChallengeDataException("Missing restaurant opening or closing time");
            }

            try {
                LocalTime open = parseTime(restaurant.open());
                LocalTime close = parseTime(restaurant.close());

                if (!isWithinTimeWindow(requestedTime, open, close)) {
                    continue;
                }

                for (SourceDeal deal : restaurant.deals()) {
                    if (isActiveDeal(deal, requestedTime)) {
                        count++;
                    }
                }
            } catch (DateTimeParseException e) {
                throw new ChallengeDataException("Invalid restaurant or deal hours", e);
            }
        }

        return count;
    }


}
