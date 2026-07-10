package com.supportflow.identity.service.impl;

import com.supportflow.identity.service.EmailSender;
import org.springframework.stereotype.Component;

// TEMPORARY (Epic 1): logs to console instead of sending real email.
// Replace with a real implementation once the Notification Service
// (Doc 03 §3) exists — this interface boundary means AuthServiceImpl
// won't need to change when that happens.
@Component
public class ConsoleEmailSender implements EmailSender {

    @Override
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        System.out.println("=== VERIFICATION EMAIL (console stub) ===");
        System.out.println("To: " + toEmail);
        System.out.println("Link: http://localhost:3000/verify-email?token=" + verificationToken);
        System.out.println("==========================================");
    }
}