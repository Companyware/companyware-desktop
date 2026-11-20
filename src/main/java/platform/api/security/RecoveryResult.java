// Companyware
// Copyright (c) 2026 Companyware contributors
//
// SPDX-License-Identifier: MIT
// Licensed under the MIT License; see LICENSE in the project root.
// This file is part of Companyware.
package platform.api.security;

/** Reveals an account name only after a recovery key was successfully consumed. */
public final class RecoveryResult {
    private final boolean success;
    private final String username;
    private RecoveryResult(boolean success, String username) { this.success = success; this.username = username; }
    public static RecoveryResult denied() { return new RecoveryResult(false, null); }
    public static RecoveryResult success(String username) { return new RecoveryResult(true, username); }
    public boolean isSuccess() { return success; }
    public String username() { return username; }
}
