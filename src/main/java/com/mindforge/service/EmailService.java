package com.mindforge.service;

import com.mindforge.config.GoogleOAuthConfig;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class EmailService {

    private static final String FROM_EMAIL = "mindforge126@gmail.com";
    private static final String APP_PASSWORD = "uhmtltfcpbeaaxam"; // Your Gmail app password
    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;

    /**
     * Send password reset email
     */
    public boolean sendPasswordResetEmail(String toEmail, String resetCode, String userName) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);
            props.put("mail.smtp.ssl.trust", SMTP_HOST);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, "MindForge Support"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🔐 MindForge Password Reset Request");

            String htmlContent = buildResetEmailHtml(userName, resetCode);

            message.setContent(htmlContent, "text/html; charset=utf-8");

            Transport.send(message);
            System.out.println("Reset email sent successfully to: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("Failed to send email: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Build HTML email content
     */
    private String buildResetEmailHtml(String userName, String resetCode) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: 'Segoe UI', Arial, sans-serif; background: #f5f5fa; margin: 0; padding: 20px; }
                    .container { max-width: 500px; margin: 0 auto; background: white; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 40px rgba(0,0,0,0.1); }
                    .header { background: linear-gradient(135deg, #7c3aed, #4f46e5); padding: 30px; text-align: center; }
                    .header h1 { color: white; margin: 0; font-size: 24px; }
                    .content { padding: 30px; }
                    .code-box { background: #1e1e2e; color: #7c3aed; font-size: 32px; font-weight: bold; text-align: center; padding: 20px; border-radius: 12px; margin: 20px 0; letter-spacing: 4px; }
                    .footer { background: #f8f9fa; padding: 20px; text-align: center; font-size: 12px; color: #6b7280; }
                    .warning { color: #dc2626; font-size: 13px; margin-top: 20px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔥 MindForge</h1>
                    </div>
                    <div class="content">
                        <h2>Password Reset Request</h2>
                        <p>Hello <strong>%s</strong>,</p>
                        <p>We received a request to reset your MindForge password. Use the code below to complete the process:</p>
                        
                        <div class="code-box">%s</div>
                        
                        <p>This code will expire in <strong>15 minutes</strong>.</p>
                        <p class="warning">If you didn't request this reset, please ignore this email or contact support.</p>
                    </div>
                    <div class="footer">
                        <p>© 2024 MindForge. All rights reserved.</p>
                        <p>This is an automated message, please do not reply.</p>
                    </div>
                </div>
            </body>
            </html>
            """.formatted(userName != null ? userName : "User", resetCode);
    }

    /**
     * Send simple text email (for testing)
     */
    public boolean sendSimpleEmail(String toEmail, String subject, String body) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", SMTP_PORT);

            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
                }
            });

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}