// Companyware
// Copyright (c) 2026 Companyware contributors
//
// SPDX-License-Identifier: MIT
// Licensed under the MIT License; see LICENSE in the project root.
// This file is part of Companyware.
package platform.api.security;

import platform.api.service.Service;

/** Secure recovery by offline key or a verified profile e-mail address. */
public interface AccountRecoveryService extends Service {
    String rotate(String username);
    void requestEmailVerification(String username);
    boolean verifyEmail(String verificationLinkOrToken);
    boolean emailVerified(String username);
    void requestEmailRecovery(String email);
    RecoveryResult reset(String key, char[] newPassword);
}
