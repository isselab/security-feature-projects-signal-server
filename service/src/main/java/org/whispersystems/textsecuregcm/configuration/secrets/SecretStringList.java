/*
 * Copyright 2023 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.textsecuregcm.configuration.secrets;

import org.gravity.security.annotations.requirements.*;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import javax.validation.constraints.NotEmpty;
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.NotEmptyValidatorForCollection;

@Critical(secrecy = "Secret.Secret(T):void", integrity = "Secret.Secret(T):void")
public class SecretStringList extends Secret<List<String>> {

  @SuppressWarnings("rawtypes")
  public static class ValidatorNotEmpty extends BaseSecretValidator<NotEmpty, Collection, SecretStringList> {
    public ValidatorNotEmpty() {
      super(new NotEmptyValidatorForCollection());
    }
  }

  @Secrecy
  public SecretStringList(final List<String> value) {
    super(ImmutableList.copyOf(value));
  }
}
