/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.configuration.secrets;

import org.gravity.security.annotations.requirements.*;
import org.apache.commons.lang3.Validate;

@Critical(secrecy = "Secret.Secret(T):void", integrity = "Secret.Secret(T):void")
public class SecretString extends Secret<String> {
	@Secrecy
  public SecretString(final String value) {
    super(Validate.notBlank(value, "SecretString value must not be blank"));
  }
}
