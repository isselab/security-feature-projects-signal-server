/*
 * Copyright 2013-2020 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.auth;

import java.util.Base64;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.gravity.security.annotations.requirements.Critical;
import org.gravity.security.annotations.requirements.Secrecy;

@Critical(secrecy = {"Anonymous.unidentifiedSenderAccessKey:byte[]"})
public class Anonymous {

  @Secrecy
  private final byte[] unidentifiedSenderAccessKey;

  public Anonymous(String header) {
    try {
      this.unidentifiedSenderAccessKey = Base64.getDecoder().decode(header);
    } catch (IllegalArgumentException e) {
      throw new WebApplicationException(e, Response.Status.UNAUTHORIZED);
    }
  }

  @Secrecy
  public byte[] getAccessKey() {
    return unidentifiedSenderAccessKey;
  }
}
