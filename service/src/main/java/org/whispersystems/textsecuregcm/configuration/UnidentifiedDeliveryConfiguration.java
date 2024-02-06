/*
 * Copyright 2013 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.configuration;

import javax.validation.constraints.NotNull;

import org.gravity.security.annotations.requirements.Secrecy;
import org.signal.libsignal.protocol.InvalidKeyException;
import org.signal.libsignal.protocol.ecc.Curve;
import org.signal.libsignal.protocol.ecc.ECPrivateKey;
import org.whispersystems.textsecuregcm.configuration.secrets.SecretBytes;
import org.whispersystems.textsecuregcm.util.ExactlySize;


public record UnidentifiedDeliveryConfiguration(@Secrecy @NotNull SecretBytes certificate,
                                                @Secrecy @ExactlySize(32) SecretBytes privateKey,
                                                int expiresDays) {
  // Leaving Scope
  @Secrecy
  public ECPrivateKey ecPrivateKey() throws InvalidKeyException {
    return Curve.decodePrivatePoint(privateKey.value()); // &line[SecretAccess]
  }
}
