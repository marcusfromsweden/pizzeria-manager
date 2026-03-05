package com.pizzeriaservice.service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzeriaservice.api.dto.AddressResponse;
import com.pizzeriaservice.api.dto.DayHoursResponse;
import com.pizzeriaservice.api.dto.OpeningHoursResponse;
import com.pizzeriaservice.api.dto.PhoneNumberResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PizzeriaConfigParser {

  private static final Logger log = LoggerFactory.getLogger(PizzeriaConfigParser.class);

  private final ObjectMapper objectMapper;

  public AddressResponse parseAddress(String configJson) {
    if (configJson == null || configJson.isBlank()) {
      return null;
    }

    try {
      JsonNode root = objectMapper.readTree(configJson);
      JsonNode addressNode = root.get("address");

      if (addressNode == null || addressNode.isNull()) {
        return null;
      }

      String street = getTextOrNull(addressNode.get("street"));
      String postalCode = getTextOrNull(addressNode.get("postalCode"));
      String city = getTextOrNull(addressNode.get("city"));

      if (street == null && postalCode == null && city == null) {
        return null;
      }

      return new AddressResponse(street, postalCode, city);
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse address from config: {}", e.getMessage());
      return null;
    }
  }

  public OpeningHoursResponse parseOpeningHours(String configJson) {
    if (configJson == null || configJson.isBlank()) {
      return null;
    }

    try {
      JsonNode root = objectMapper.readTree(configJson);
      JsonNode hoursNode = root.get("openingHours");

      if (hoursNode == null || hoursNode.isNull()) {
        return null;
      }

      return new OpeningHoursResponse(
          parseDayHoursList(hoursNode.get("monday")),
          parseDayHoursList(hoursNode.get("tuesday")),
          parseDayHoursList(hoursNode.get("wednesday")),
          parseDayHoursList(hoursNode.get("thursday")),
          parseDayHoursList(hoursNode.get("friday")),
          parseDayHoursList(hoursNode.get("saturday")),
          parseDayHoursList(hoursNode.get("sunday")));
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse opening hours from config: {}", e.getMessage());
      return null;
    }
  }

  public List<PhoneNumberResponse> parsePhoneNumbers(String configJson) {
    if (configJson == null || configJson.isBlank()) {
      return List.of();
    }

    try {
      JsonNode root = objectMapper.readTree(configJson);
      JsonNode phonesNode = root.get("phoneNumbers");

      if (phonesNode == null || phonesNode.isNull() || !phonesNode.isArray()) {
        return List.of();
      }

      List<PhoneNumberResponse> phoneNumbers = new ArrayList<>();
      for (JsonNode phoneNode : phonesNode) {
        String label = getTextOrNull(phoneNode.get("label"));
        String number = getTextOrNull(phoneNode.get("number"));
        if (label != null && number != null) {
          phoneNumbers.add(new PhoneNumberResponse(label, number));
        }
      }
      return phoneNumbers;
    } catch (JsonProcessingException e) {
      log.warn("Failed to parse phone numbers from config: {}", e.getMessage());
      return List.of();
    }
  }

  private List<DayHoursResponse> parseDayHoursList(JsonNode dayNode) {
    if (dayNode == null || dayNode.isNull()) {
      return List.of();
    }

    // Support both array format and single object format for backwards compatibility
    if (dayNode.isArray()) {
      List<DayHoursResponse> slots = new ArrayList<>();
      for (JsonNode slotNode : dayNode) {
        DayHoursResponse slot = parseSingleDayHours(slotNode);
        if (slot != null) {
          slots.add(slot);
        }
      }
      return slots;
    } else {
      // Single object format (backwards compatibility)
      DayHoursResponse slot = parseSingleDayHours(dayNode);
      return slot != null ? List.of(slot) : List.of();
    }
  }

  private DayHoursResponse parseSingleDayHours(JsonNode dayNode) {
    if (dayNode == null || dayNode.isNull()) {
      return null;
    }

    String open = getTextOrNull(dayNode.get("open"));
    String close = getTextOrNull(dayNode.get("close"));

    if (open == null || close == null) {
      return null;
    }

    return new DayHoursResponse(open, close);
  }

  private String getTextOrNull(JsonNode node) {
    return (node != null && !node.isNull()) ? node.asText() : null;
  }
}
