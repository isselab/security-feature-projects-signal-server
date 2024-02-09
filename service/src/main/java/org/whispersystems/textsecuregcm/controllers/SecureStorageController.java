/*
 * Copyright 2013 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.controllers;

import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.gravity.security.annotations.requirements.Critical;
import org.gravity.security.annotations.requirements.Secrecy;
import org.whispersystems.textsecuregcm.auth.AuthenticatedAccount;
import org.whispersystems.textsecuregcm.auth.ExternalServiceCredentials;
import org.whispersystems.textsecuregcm.auth.ExternalServiceCredentialsGenerator;
import org.whispersystems.textsecuregcm.configuration.SecureStorageServiceConfiguration;

@Path("/v1/storage")
@Tag(name = "Secure Storage")
@Critical(secrecy = "ExternalServiceCredentialsGenerator.generateForUuid(UUID):ExternalServiceCredentials")
public class SecureStorageController {

  private final ExternalServiceCredentialsGenerator storageServiceCredentialsGenerator;

  public static ExternalServiceCredentialsGenerator credentialsGenerator(final SecureStorageServiceConfiguration cfg) {
    return ExternalServiceCredentialsGenerator
        .builder(cfg.userAuthenticationTokenSharedSecret())
        .prependUsername(true)
        .build();
  }

  public SecureStorageController(ExternalServiceCredentialsGenerator storageServiceCredentialsGenerator) {
    this.storageServiceCredentialsGenerator = storageServiceCredentialsGenerator;
  }

  @GET
  @Path("/auth")
  @Produces(MediaType.APPLICATION_JSON)
  @Secrecy
  public ExternalServiceCredentials getAuth(@Auth AuthenticatedAccount auth) { // &line[AccountAuthenticator]
    return storageServiceCredentialsGenerator.generateForUuid(auth.getAccount().getUuid());
  }
}
