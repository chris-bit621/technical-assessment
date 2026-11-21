package com.eatclub.controller;

import com.eatclub.exception.ChallengeDataException;
import com.eatclub.model.api.DealsResponse;
import com.eatclub.service.DealService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static com.eatclub.time.TimeParser.parseTime;

@RestController
@RequestMapping("/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    @GetMapping
    public DealsResponse activeDeals(@RequestParam String timeOfDay) {
        LocalTime requestedTime = parseTime(timeOfDay);
        return dealService.activeDeals(requestedTime);
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ProblemDetail handleInvalidTimeOfDay() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Invalid timeOfDay");
    }

    @ExceptionHandler(ChallengeDataException.class)
    public ProblemDetail handleUpstreamFailure() {
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY, "Restaurant deal data is currently unavailable.");
    }
}
