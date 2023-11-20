/*
 * Copyright 2021 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.configuration;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;

import org.gravity.security.annotations.requirements.Critical;
import org.gravity.security.annotations.requirements.Secrecy;
import org.whispersystems.textsecuregcm.configuration.secrets.SecretString;

@Critical(secrecy = "HCaptchaConfiguration.apiKey:org.whispersystems.textsecuregcm.configuration.secrets.SecretString")
public class HCaptchaConfiguration {

  @Secrecy
  @JsonProperty
  @NotNull
  SecretString apiKey;

  @JsonProperty
  @NotNull
  CircuitBreakerConfiguration circuitBreaker = new CircuitBreakerConfiguration();

  @JsonProperty
  @NotNull
  RetryConfiguration retry = new RetryConfiguration();


  @Secrecy
  public SecretString getApiKey() {
    return apiKey;
  }

  public CircuitBreakerConfiguration getCircuitBreaker() {
    return circuitBreaker;
  }

  public RetryConfiguration getRetry() {
    return retry;
  }

}
