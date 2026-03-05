package com.pizzeriaservice.api.dto;

import java.util.List;

public record OpeningHoursResponse(
    List<DayHoursResponse> monday,
    List<DayHoursResponse> tuesday,
    List<DayHoursResponse> wednesday,
    List<DayHoursResponse> thursday,
    List<DayHoursResponse> friday,
    List<DayHoursResponse> saturday,
    List<DayHoursResponse> sunday) {}
