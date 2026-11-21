package com.eatclub.controller;

import com.eatclub.model.api.DealsResponse;
import com.eatclub.service.DealService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/deals")
public class DealController {

    private final DealService dealService;

    public DealController(DealService dealService) {
        this.dealService = dealService;
    }

    @GetMapping
    public DealsResponse activeDeals(@RequestParam String timeOfDay) {
        // TODO: Return HTTP 400 for invalid times and 502 for upstream failures.
        return dealService.activeDeals(timeOfDay);
    }
}
