package com.pizzeriaservice.service.controller;

import com.pizzeriaservice.api.dto.FeedbackResponse;
import com.pizzeriaservice.api.dto.ServiceFeedbackRequest;
import com.pizzeriaservice.api.dto.UnreadFeedbackCountResponse;
import com.pizzeriaservice.service.config.CommonApiResponses;
import com.pizzeriaservice.service.service.FeedbackService;
import com.pizzeriaservice.service.support.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping(path = "/api/v1/feedback", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@Tag(name = "Feedback", description = "User service feedback and admin replies")
public class FeedbackController {

  private final FeedbackService feedbackService;

  @GetMapping("/me")
  @Operation(summary = "Get current user's feedback")
  @ApiResponse(responseCode = "200", description = "List of user's feedback returned")
  @CommonApiResponses
  public Flux<FeedbackResponse> myFeedback(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    return feedbackService.getForUser(user.userId(), user.pizzeriaId());
  }

  @PostMapping("/service")
  @Operation(summary = "Submit service feedback")
  @ApiResponse(responseCode = "201", description = "Feedback submitted successfully")
  @CommonApiResponses
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<FeedbackResponse> submitServiceFeedback(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user,
      @Valid @RequestBody ServiceFeedbackRequest request) {
    return feedbackService.submitServiceFeedback(user.userId(), user.pizzeriaId(), request);
  }

  @GetMapping("/me/unread-count")
  @Operation(summary = "Get count of unread admin replies")
  @ApiResponse(responseCode = "200", description = "Unread reply count returned")
  @CommonApiResponses
  public Mono<UnreadFeedbackCountResponse> getUnreadReplyCount(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    return feedbackService.getUnreadReplyCount(user.userId(), user.pizzeriaId());
  }

  @PostMapping("/me/mark-read")
  @Operation(summary = "Mark all admin replies as read")
  @ApiResponse(responseCode = "200", description = "All replies marked as read")
  @CommonApiResponses
  public Mono<Void> markAllRepliesAsRead(
      @Parameter(hidden = true) @AuthenticationPrincipal AuthenticatedUser user) {
    return feedbackService.markAllRepliesAsRead(user.userId(), user.pizzeriaId());
  }
}
