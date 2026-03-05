package com.pizzeriaservice.service.support;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserValidatorTest {

  private UserValidator validator;

  @BeforeEach
  void setUp() {
    validator = new UserValidator();
  }

  @Test
  void shouldAcceptNullPhoto() {
    assertThatCode(() -> validator.validateProfilePhoto(null)).doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptEmptyPhoto() {
    assertThatCode(() -> validator.validateProfilePhoto("")).doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptValidJpegDataUrl() {
    assertThatCode(() -> validator.validateProfilePhoto("data:image/jpeg;base64,/9j/4AAQ"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptValidPngDataUrl() {
    assertThatCode(() -> validator.validateProfilePhoto("data:image/png;base64,iVBORw0K"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptValidGifDataUrl() {
    assertThatCode(() -> validator.validateProfilePhoto("data:image/gif;base64,R0lGODlh"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldAcceptValidWebpDataUrl() {
    assertThatCode(() -> validator.validateProfilePhoto("data:image/webp;base64,UklGRiIA"))
        .doesNotThrowAnyException();
  }

  @Test
  void shouldRejectInvalidFormat() {
    assertThatThrownBy(() -> validator.validateProfilePhoto("not-a-data-url"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("Invalid profile photo format");
  }

  @Test
  void shouldRejectUnsupportedMimeType() {
    assertThatThrownBy(() -> validator.validateProfilePhoto("data:image/bmp;base64,Qk0"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("Invalid profile photo format");
  }

  @Test
  void shouldRejectMissingBase64Prefix() {
    assertThatThrownBy(() -> validator.validateProfilePhoto("data:image/jpeg,/9j/4AAQ"))
        .isInstanceOf(DomainValidationException.class)
        .hasMessageContaining("Invalid profile photo format");
  }
}
