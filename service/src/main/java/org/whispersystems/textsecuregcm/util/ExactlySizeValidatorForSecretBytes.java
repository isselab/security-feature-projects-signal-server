/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.util;

import org.gravity.security.annotations.requirements.*;
import org.whispersystems.textsecuregcm.configuration.secrets.SecretBytes;

@Critical(integrity = "Secret.value():T", secrecy = "Secret.value():T")
public class ExactlySizeValidatorForSecretBytes extends ExactlySizeValidator<SecretBytes> {
  @Override
  protected int size(final SecretBytes value) {
    return value == null ? 0 : value.value().length;
  }
}
