package com.pizzeriaservice.service.support;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class UserValidator {

  private static final Pattern DATA_URL_PATTERN =
      Pattern.compile("^data:image/(jpeg|png|gif|webp);base64,.+$", Pattern.DOTALL);

  public void validateProfilePhoto(String base64Data) {
    if (base64Data == null || base64Data.isEmpty()) {
      return;
    }

    if (!DATA_URL_PATTERN.matcher(base64Data).matches()) {
      throw new DomainValidationException(
          "Invalid profile photo format. Expected data URL with image/jpeg, image/png, image/gif, or image/webp MIME type.");
    }
  }
}
