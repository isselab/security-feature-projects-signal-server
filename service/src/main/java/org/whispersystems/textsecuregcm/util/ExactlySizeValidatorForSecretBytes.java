/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.util;

import org.gravity.security.annotations.requirements.Critical;
import org.whispersystems.textsecuregcm.configuration.secrets.SecretBytes;

@Critical(secrecy = "Secret.value():Object")
public class ExactlySizeValidatorForSecretBytes extends ExactlySizeValidator<SecretBytes> {
  @Override
  // TODO: is the size of a secret also a secret? e.g. knowing the length of a password might allow for better password cracking capabilities...
  protected int size(final SecretBytes value) {
    return value == null ? 0 : value.value().length; // &line[SecretAccess]
  }
}
