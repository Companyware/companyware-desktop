// Companyware
// Copyright (c) 2026 Companyware contributors
// SPDX-License-Identifier: MIT
package core.security;

/** Sends security mail without exposing SMTP details to the recovery service. */
public interface RecoveryMailSender {
    void sendVerification(String recipient, String link) throws Exception;
    void sendRecovery(String recipient, String link) throws Exception;
}
