package com.pizzeriaservice.api.dto;

import java.util.List;

public record PizzeriaInfoResponse(
    String code,
    String name,
    String currency,
    String timezone,
    AddressResponse address,
    OpeningHoursResponse openingHours,
    List<PhoneNumberResponse> phoneNumbers) {}
