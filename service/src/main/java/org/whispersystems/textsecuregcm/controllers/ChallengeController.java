/*
 * Copyright 2021 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.controllers;

import static org.whispersystems.textsecuregcm.metrics.MetricsUtil.name;

import com.google.common.net.HttpHeaders;
import io.dropwizard.auth.Auth;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import javax.validation.Valid;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.whispersystems.textsecuregcm.auth.AuthenticatedAccount;
import org.whispersystems.textsecuregcm.entities.AnswerChallengeRequest;
import org.whispersystems.textsecuregcm.entities.AnswerPushChallengeRequest;
import org.whispersystems.textsecuregcm.entities.AnswerRecaptchaChallengeRequest;
import org.whispersystems.textsecuregcm.limits.RateLimitChallengeManager;
import org.whispersystems.textsecuregcm.metrics.UserAgentTagUtil;
import org.whispersystems.textsecuregcm.push.NotPushRegisteredException;
import org.whispersystems.textsecuregcm.spam.Extract;
import org.whispersystems.textsecuregcm.spam.FilterSpam;
import org.whispersystems.textsecuregcm.spam.PushChallengeConfig;
import org.whispersystems.textsecuregcm.spam.ScoreThreshold;
import org.whispersystems.textsecuregcm.util.HeaderUtils;

@Path("/v1/challenge")
@Tag(name = "Challenge")
@FilterSpam
public class ChallengeController {

  private final RateLimitChallengeManager rateLimitChallengeManager;

  private static final String CHALLENGE_RESPONSE_COUNTER_NAME = name(ChallengeController.class, "challengeResponse");
  private static final String CHALLENGE_TYPE_TAG = "type";

  public ChallengeController(final RateLimitChallengeManager rateLimitChallengeManager) {
    this.rateLimitChallengeManager = rateLimitChallengeManager;
  }

  @PUT
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(
      summary = "Submit proof of a challenge completion",
      description = """
          Some server endpoints (the "send message" endpoint, for example) may return a 428 response indicating the client must complete a challenge before continuing.
          Clients may use this endpoint to provide proof of a completed challenge. If successful, the client may then 
          continue their original operation.
          """,
      requestBody = @RequestBody(content = {@Content(schema = @Schema(oneOf = {AnswerPushChallengeRequest.class,
          AnswerRecaptchaChallengeRequest.class}))})
  )
  @ApiResponse(responseCode = "200", description = "Indicates the challenge proof was accepted")
  @ApiResponse(responseCode = "413", description = "Too many attempts", headers = @Header(
      name = "Retry-After",
      description = "If present, an positive integer indicating the number of seconds before a subsequent attempt could succeed"))
  @ApiResponse(responseCode = "429", description = "Too many attempts", headers = @Header(
      name = "Retry-After",
      description = "If present, an positive integer indicating the number of seconds before a subsequent attempt could succeed"))
  public Response handleChallengeResponse(@Auth final AuthenticatedAccount auth, // &line[AccountAuthenticator]
      @Valid final AnswerChallengeRequest answerRequest,
      @HeaderParam(HttpHeaders.X_FORWARDED_FOR) final String forwardedFor,
      @HeaderParam(HttpHeaders.USER_AGENT) final String userAgent,
      @Extract final ScoreThreshold captchaScoreThreshold,
      @Extract final PushChallengeConfig pushChallengeConfig) throws RateLimitExceededException, IOException {

    Tags tags = Tags.of(UserAgentTagUtil.getPlatformTag(userAgent));

    try {
      if (answerRequest instanceof final AnswerPushChallengeRequest pushChallengeRequest) {
        tags = tags.and(CHALLENGE_TYPE_TAG, "push");

        if (!pushChallengeConfig.pushPermitted()) {
          return Response.status(429).build();
        }
        rateLimitChallengeManager.answerPushChallenge(auth.getAccount(), pushChallengeRequest.getChallenge());
      } else if (answerRequest instanceof AnswerRecaptchaChallengeRequest recaptchaChallengeRequest) {
        tags = tags.and(CHALLENGE_TYPE_TAG, "recaptcha");

        final String mostRecentProxy = HeaderUtils.getMostRecentProxy(forwardedFor).orElseThrow(() -> new BadRequestException());
        // &begin[Recaptcha]
        boolean success = rateLimitChallengeManager.answerRecaptchaChallenge(
            auth.getAccount(),
            recaptchaChallengeRequest.getCaptcha(),
            mostRecentProxy,
            userAgent,
            captchaScoreThreshold.getScoreThreshold());
        // &end[Recaptcha]

        if (!success) {
          return Response.status(428).build();
        }

      } else {
        tags = tags.and(CHALLENGE_TYPE_TAG, "unrecognized");
      }
    } finally {
      Metrics.counter(CHALLENGE_RESPONSE_COUNTER_NAME, tags).increment();
    }

    return Response.status(200).build();
  }

  @POST
  @Path("/push")
  @Operation(
      summary = "Request a push challenge",
      description = """
          Clients may proactively request a push challenge by making an empty POST request. Push challenges will only be
          sent to the requesting account’s main device. When the push is received it may be provided as proof of completed 
          challenge to /v1/challenge.
          APNs challenge payloads will be formatted as follows:
          ```
          {
              "aps": {
                  "sound": "default",
                  "alert": {
                      "loc-key": "APN_Message"
                  }
              },
              "rateLimitChallenge": "{CHALLENGE_TOKEN}"
          }
          ```
          FCM challenge payloads will be formatted as follows: 
          ```
          {"rateLimitChallenge": "{CHALLENGE_TOKEN}"}
          ```

          Clients may retry the PUT in the event of an HTTP/5xx response (except HTTP/508) from the server, but must 
          implement an exponential back-off system and limit the total number of retries.
          """
  )
  @ApiResponse(responseCode = "200", description = """
      Indicates a payload to the account's primary device has been attempted. When clients receive a challenge push
      notification, they may issue a PUT request to /v1/challenge.
      """)
  @ApiResponse(responseCode = "404", description = """
      The server does not have a push notification token for the authenticated account’s main device; clients may add a push
      token and try again
      """)
  @ApiResponse(responseCode = "413", description = "Too many attempts", headers = @Header(
      name = "Retry-After",
      description = "If present, an positive integer indicating the number of seconds before a subsequent attempt could succeed"))
  @ApiResponse(responseCode = "429", description = "Too many attempts", headers = @Header(
      name = "Retry-After",
      description = "If present, an positive integer indicating the number of seconds before a subsequent attempt could succeed"))
  public Response requestPushChallenge(@Auth final AuthenticatedAccount auth, // &line[AccountAuthenticator]
      @Extract PushChallengeConfig pushChallengeConfig) {
    if (!pushChallengeConfig.pushPermitted()) {
      return Response.status(429).build();
    }
    try {
      rateLimitChallengeManager.sendPushChallenge(auth.getAccount());
      return Response.status(200).build();
    } catch (final NotPushRegisteredException e) {
      return Response.status(404).build();
    }
  }
}
