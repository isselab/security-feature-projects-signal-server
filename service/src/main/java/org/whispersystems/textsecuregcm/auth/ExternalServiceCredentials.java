/*
 * Copyright 2013-2020 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.auth;

import org.gravity.security.annotations.requirements.Critical;

@Critical(secrecy = "password:String")
public record ExternalServiceCredentials(String username, String password) {

}
