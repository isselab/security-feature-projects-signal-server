/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.configuration.secrets;

import org.gravity.security.annotations.requirements.*;
import org.apache.commons.lang3.Validate;

@Critical(integrity = "Secret.Secret(T):void", secrecy = "SecretBytes.requireNotEmpty(byte[]):byte[]")
public class SecretBytes extends Secret<byte[]> {

	@Secrecy
  public SecretBytes(final byte[] value) {
    super(requireNotEmpty(value));
  }

	@Secrecy
  private static byte[] requireNotEmpty(final byte[] value) {
    Validate.isTrue(value.length > 0, "SecretBytes value must not be empty");
    return value;
  }
}
