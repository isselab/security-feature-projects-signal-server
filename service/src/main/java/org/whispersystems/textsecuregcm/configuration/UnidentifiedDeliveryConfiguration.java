/*
 * Copyright 2013 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.configuration;

import org.gravity.security.annotations.requirements.*;
import javax.validation.constraints.NotNull;
import org.signal.libsignal.protocol.InvalidKeyException;
import org.signal.libsignal.protocol.ecc.Curve;
import org.signal.libsignal.protocol.ecc.ECPrivateKey;
import org.whispersystems.textsecuregcm.configuration.secrets.SecretBytes;
import org.whispersystems.textsecuregcm.util.ExactlySize;

@Critical(secrecy = "Secret.value():T", integrity = "Secret.value():T")
public record UnidentifiedDeliveryConfiguration(@NotNull SecretBytes certificate,
                                                @ExactlySize(32) SecretBytes privateKey,
                                                int expiresDays) {
	@Secrecy
  public ECPrivateKey ecPrivateKey() throws InvalidKeyException {
    return Curve.decodePrivatePoint(privateKey.value());
  }
}
