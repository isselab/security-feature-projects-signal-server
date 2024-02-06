/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.attachments;

import org.whispersystems.textsecuregcm.configuration.secrets.SecretBytes;
import org.whispersystems.textsecuregcm.util.ExactlySize;
import javax.validation.constraints.NotEmpty;
import org.gravity.security.annotations.requirements.Secrecy;

public record TusConfiguration(
  @Secrecy @ExactlySize(32) SecretBytes userAuthenticationTokenSharedSecret,
  @NotEmpty String uploadUri
){}
