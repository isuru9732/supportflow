package com.supportflow.identity.service;

public interface EmailSender {
    void sendVerificationEmail(String toEmail, String verificationToken);
    void sendPasswordResetEmail(String toEmail, String resetToken);
}