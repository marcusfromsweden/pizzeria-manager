package com.pizzeriaservice.service.controller;

import com.pizzeriaservice.api.dto.AdminReplyRequest;
import com.pizzeriaservice.api.dto.FeedbackResponse;
import com.pizzeriaservice.service.config.CommonApiResponses;
import com.pizzeriaservice.service.service.FeedbackService;
import com.pizzeriaservice.service.service.PizzeriaService;
import com.pizzeriaservice.service.support.ForbiddenException;
import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(
    path = "/api/v1/admin/pizzerias/{pizzeriaCode}/feedback",
    produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Admin Feedback", description = "Feedback management for pizzeria administrators")
public class AdminFeedbackController {

  private final FeedbackService feedbackService;
  private final PizzeriaService pizzeriaService;

  @GetMapping
  @CommonApiResponses
  @Operation(summary = "Get all feedback for pizzeria")
  @ApiResponse(responseCode = "200", description = "List of all feedback returned")
  public Flux<FeedbackResponse> getAllFeedback(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode,
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    validateAdminAccess(user, pizzeriaCode);

    return pizzeriaService
        .resolvePizzeriaId(pizzeriaCode)
        .flatMapMany(feedbackService::getAllForPizzeria);
  }

  @PostMapping("/{feedbackId}/reply")
  @CommonApiResponses
  @Operation(summary = "Reply to feedback")
  @ApiResponse(responseCode = "200", description = "Reply added to feedback")
  public Mono<FeedbackResponse> replyToFeedback(
      @Parameter(description = "Pizzeria code identifier") @PathVariable String pizzeriaCode,
      @Parameter(description = "Feedback unique identifier") @PathVariable UUID feedbackId,
      @Valid @RequestBody AdminReplyRequest request,
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    validateAdminAccess(user, pizzeriaCode);

    return pizzeriaService
        .resolvePizzeriaId(pizzeriaCode)
        .flatMap(
            pizzeriaId -> feedbackService.addAdminReply(feedbackId, pizzeriaId, request.reply()));
  }

  private void validateAdminAccess(AuthenticatedUser user, String pizzeriaCode) {
    if (!user.isAdminFor(pizzeriaCode)) {
      throw new ForbiddenException(
          "User is not authorized to view feedback for pizzeria: " + pizzeriaCode);
    }
  }
}
