/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.configuration.secrets;

import org.gravity.security.annotations.requirements.Critical;
import org.gravity.security.annotations.requirements.Secrecy;

@Critical(secrecy = "Secret.value:T")
public class Secret<T> {

  @Secrecy
  private final T value;


  public Secret(final T value) {
    this.value = value;
  }

  @Secrecy
  public T value() {
    return value;
  }

  @Override
  public String toString() {
    return "[REDACTED]";
  }
}
