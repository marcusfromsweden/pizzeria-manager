package com.pizzeriaservice.service.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pizzeriaservice.api.dto.AddressResponse;
import com.pizzeriaservice.api.dto.DayHoursResponse;
import com.pizzeriaservice.api.dto.OpeningHoursResponse;
import com.pizzeriaservice.api.dto.PhoneNumberResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PizzeriaConfigParserTest {

  private PizzeriaConfigParser parser;

  @BeforeEach
  void setUp() {
    parser = new PizzeriaConfigParser(new ObjectMapper());
  }

  // Opening Hours tests

  @Test
  void shouldParseOpeningHoursWithSingleSlotPerDay() {
    String config =
        """
        {
          "openingHours": {
            "monday": {"open": "10:00", "close": "22:00"},
            "tuesday": {"open": "10:00", "close": "22:00"},
            "wednesday": {"open": "10:00", "close": "22:00"},
            "thursday": {"open": "10:00", "close": "22:00"},
            "friday": {"open": "10:00", "close": "23:00"},
            "saturday": {"open": "11:00", "close": "23:00"},
            "sunday": {"open": "12:00", "close": "21:00"}
          }
        }
        """;

    OpeningHoursResponse result = parser.parseOpeningHours(config);

    assertThat(result).isNotNull();
    assertThat(result.monday()).containsExactly(new DayHoursResponse("10:00", "22:00"));
    assertThat(result.tuesday()).containsExactly(new DayHoursResponse("10:00", "22:00"));
    assertThat(result.wednesday()).containsExactly(new DayHoursResponse("10:00", "22:00"));
    assertThat(result.thursday()).containsExactly(new DayHoursResponse("10:00", "22:00"));
    assertThat(result.friday()).containsExactly(new DayHoursResponse("10:00", "23:00"));
    assertThat(result.saturday()).containsExactly(new DayHoursResponse("11:00", "23:00"));
    assertThat(result.sunday()).containsExactly(new DayHoursResponse("12:00", "21:00"));
  }

  @Test
  void shouldParseOpeningHoursWithMultipleSlotsPerDay() {
    String config =
        """
        {
          "openingHours": {
            "monday": [{"open": "11:00", "close": "14:00"}, {"open": "16:30", "close": "21:00"}],
            "saturday": [{"open": "16:00", "close": "21:00"}]
          }
        }
        """;

    OpeningHoursResponse result = parser.parseOpeningHours(config);

    assertThat(result).isNotNull();
    assertThat(result.monday())
        .containsExactly(
            new DayHoursResponse("11:00", "14:00"), new DayHoursResponse("16:30", "21:00"));
    assertThat(result.saturday()).containsExactly(new DayHoursResponse("16:00", "21:00"));
  }

  @Test
  void shouldReturnEmptyListForDayWithMissingHours() {
    String config =
        """
        {
          "openingHours": {
            "monday": {"open": "10:00", "close": "22:00"},
            "tuesday": null
          }
        }
        """;

    OpeningHoursResponse result = parser.parseOpeningHours(config);

    assertThat(result).isNotNull();
    assertThat(result.monday()).containsExactly(new DayHoursResponse("10:00", "22:00"));
    assertThat(result.tuesday()).isEmpty();
    assertThat(result.wednesday()).isEmpty();
  }

  @Test
  void shouldReturnNullForMissingOpeningHours() {
    String config =
        """
        {
          "features": {"someFeature": true}
        }
        """;

    OpeningHoursResponse result = parser.parseOpeningHours(config);

    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNullForNullConfig() {
    assertThat(parser.parseOpeningHours(null)).isNull();
  }

  @Test
  void shouldReturnNullForEmptyConfig() {
    assertThat(parser.parseOpeningHours("")).isNull();
    assertThat(parser.parseOpeningHours("  ")).isNull();
  }

  @Test
  void shouldReturnNullForInvalidJson() {
    assertThat(parser.parseOpeningHours("not valid json")).isNull();
  }

  // Phone Numbers tests

  @Test
  void shouldParsePhoneNumbers() {
    String config =
        """
        {
          "phoneNumbers": [
            {"label": "Orders", "number": "+46 40 123 456"},
            {"label": "Support", "number": "+46 40 123 457"}
          ]
        }
        """;

    List<PhoneNumberResponse> result = parser.parsePhoneNumbers(config);

    assertThat(result).hasSize(2);
    assertThat(result.get(0)).isEqualTo(new PhoneNumberResponse("Orders", "+46 40 123 456"));
    assertThat(result.get(1)).isEqualTo(new PhoneNumberResponse("Support", "+46 40 123 457"));
  }

  @Test
  void shouldReturnEmptyListForMissingPhoneNumbers() {
    String config =
        """
        {
          "features": {"someFeature": true}
        }
        """;

    List<PhoneNumberResponse> result = parser.parsePhoneNumbers(config);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldReturnEmptyListForNullPhoneNumbers() {
    String config =
        """
        {
          "phoneNumbers": null
        }
        """;

    List<PhoneNumberResponse> result = parser.parsePhoneNumbers(config);

    assertThat(result).isEmpty();
  }

  @Test
  void shouldSkipPhoneNumbersWithMissingFields() {
    String config =
        """
        {
          "phoneNumbers": [
            {"label": "Orders", "number": "+46 40 123 456"},
            {"label": "NoNumber"},
            {"number": "+46 40 123 457"}
          ]
        }
        """;

    List<PhoneNumberResponse> result = parser.parsePhoneNumbers(config);

    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(new PhoneNumberResponse("Orders", "+46 40 123 456"));
  }

  @Test
  void shouldReturnEmptyListForNullConfig() {
    assertThat(parser.parsePhoneNumbers(null)).isEmpty();
  }

  @Test
  void shouldReturnEmptyListForEmptyConfig() {
    assertThat(parser.parsePhoneNumbers("")).isEmpty();
    assertThat(parser.parsePhoneNumbers("  ")).isEmpty();
  }

  @Test
  void shouldReturnEmptyListForInvalidJson() {
    assertThat(parser.parsePhoneNumbers("not valid json")).isEmpty();
  }

  // Address tests

  @Test
  void shouldParseAddressWithAllFields() {
    String config =
        """
        {
          "address": {
            "street": "Föreningsgatan 67",
            "postalCode": "211 52",
            "city": "Malmö"
          }
        }
        """;

    AddressResponse result = parser.parseAddress(config);

    assertThat(result).isNotNull();
    assertThat(result.street()).isEqualTo("Föreningsgatan 67");
    assertThat(result.postalCode()).isEqualTo("211 52");
    assertThat(result.city()).isEqualTo("Malmö");
  }

  @Test
  void shouldParseAddressWithPartialFields() {
    String config =
        """
        {
          "address": {
            "city": "Stockholm"
          }
        }
        """;

    AddressResponse result = parser.parseAddress(config);

    assertThat(result).isNotNull();
    assertThat(result.street()).isNull();
    assertThat(result.postalCode()).isNull();
    assertThat(result.city()).isEqualTo("Stockholm");
  }

  @Test
  void shouldReturnNullForMissingAddress() {
    String config =
        """
        {
          "features": {"someFeature": true}
        }
        """;

    AddressResponse result = parser.parseAddress(config);

    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNullForEmptyAddress() {
    String config =
        """
        {
          "address": {}
        }
        """;

    AddressResponse result = parser.parseAddress(config);

    assertThat(result).isNull();
  }

  @Test
  void shouldReturnNullAddressForNullConfig() {
    assertThat(parser.parseAddress(null)).isNull();
  }

  @Test
  void shouldReturnNullAddressForEmptyConfig() {
    assertThat(parser.parseAddress("")).isNull();
    assertThat(parser.parseAddress("  ")).isNull();
  }

  // Combined config tests

  @Test
  void shouldParseConfigWithAllFields() {
    String config =
        """
        {
          "features": {"dietarySuitability": true},
          "address": {
            "street": "Main Street 1",
            "city": "City"
          },
          "openingHours": {
            "monday": {"open": "09:00", "close": "21:00"}
          },
          "phoneNumbers": [
            {"label": "Main", "number": "+1 555 1234"}
          ]
        }
        """;

    AddressResponse address = parser.parseAddress(config);
    OpeningHoursResponse hours = parser.parseOpeningHours(config);
    List<PhoneNumberResponse> phones = parser.parsePhoneNumbers(config);

    assertThat(address).isNotNull();
    assertThat(address.street()).isEqualTo("Main Street 1");
    assertThat(address.city()).isEqualTo("City");

    assertThat(hours).isNotNull();
    assertThat(hours.monday()).containsExactly(new DayHoursResponse("09:00", "21:00"));

    assertThat(phones).hasSize(1);
    assertThat(phones.get(0).label()).isEqualTo("Main");
  }
}
