package com.mindforge.utils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class EmailService {

    private static final String FROM_EMAIL    = "mohamedamine.rja053@gmail.com";
    private static final String FROM_NAME     = "MindForge Careers";
    private static final String APP_PASSWORD  = "ewaopqfxewwzjxtu";
    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final int    SMTP_PORT     = 587;

    // ── Send acceptance email ─────────────────────────────────────────────────

    public static void sendAcceptanceEmail(String toEmail, String applicantName,
                                           String opportunityTitle, String opportunityType,
                                           String companyName) {
        String subject = "Your application for \"" + opportunityTitle + "\" has been accepted!";
        String body    = buildAcceptedHtml(applicantName, opportunityTitle, opportunityType, companyName);
        send(toEmail, subject, body);
    }

    // ── Send rejection email ──────────────────────────────────────────────────

    public static void sendRejectionEmail(String toEmail, String applicantName,
                                          String opportunityTitle, String companyName) {
        String subject = "Update on your application for \"" + opportunityTitle + "\"";
        String body    = buildRejectedHtml(applicantName, opportunityTitle, companyName);
        send(toEmail, subject, body);
    }

    // ── Lookup user email from DB by userId ───────────────────────────────────

    public static String getUserEmail(int userId) {
        String query = "SELECT email FROM user WHERE id = ?";
        try {
            Connection cnx = DBConnection.getConnection();
            try (PreparedStatement pst = cnx.prepareStatement(query)) {
                pst.setInt(1, userId);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) return rs.getString("email");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching user email: " + e.getMessage());
        }
        return null;
    }

    // ── Core send logic ───────────────────────────────────────────────────────

    private static void send(String toEmail, String subject, String htmlBody) {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            String.valueOf(SMTP_PORT));

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME));
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            message.setSubject(subject, "UTF-8");
            message.setContent(htmlBody, "text/html; charset=UTF-8");
            Transport.send(message);
            System.out.println("Email sent to: " + toEmail);
        } catch (Exception e) {
            System.out.println("Failed to send email: " + e.getMessage());
        }
    }

    // ── HTML Templates (mirrors Symfony Twig templates) ───────────────────────

    private static String buildAcceptedHtml(String applicantName, String opportunityTitle,
                                             String opportunityType, String companyName) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
            <style>
              body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 0; }
              .wrapper { max-width: 600px; margin: 40px auto; background: #fff; border-radius: 6px;
                         overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
              .header  { background: #5cb85c; padding: 30px; text-align: center; }
              .header h1 { color: #fff; margin: 0; font-size: 24px; }
              .body    { padding: 30px; color: #333; line-height: 1.7; }
              .detail-box { background: #f9f9f9; border-left: 4px solid #5cb85c;
                            padding: 15px 20px; margin: 20px 0; border-radius: 0 4px 4px 0; }
              .footer  { background: #f4f4f4; padding: 15px 30px; text-align: center;
                         font-size: 12px; color: #888; }
            </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="header">
                  <h1>Congratulations — Your Application Was Accepted!</h1>
                </div>
                <div class="body">
                  <p>Dear %s,</p>
                  <p>We are pleased to inform you that your application for the following opportunity
                     has been <strong>accepted</strong>:</p>
                  <div class="detail-box">
                    <strong>Position:</strong> %s<br>
                    <strong>Company:</strong> %s<br>
                    <strong>Type:</strong> %s
                  </div>
                  <p>The company will reach out to you shortly with further details.
                     Make sure your profile information is up to date.</p>
                  <p>Good luck — the MindForge team is rooting for you!</p>
                </div>
                <div class="footer">
                  &copy; MindForge — This is an automated notification, please do not reply to this email.
                </div>
              </div>
            </body>
            </html>
            """.formatted(applicantName, opportunityTitle, companyName, opportunityType);
    }

    private static String buildRejectedHtml(String applicantName, String opportunityTitle,
                                             String companyName) {
        return """
            <!DOCTYPE html>
            <html>
            <head><meta charset="UTF-8">
            <style>
              body { font-family: Arial, sans-serif; background: #f4f4f4; margin: 0; padding: 0; }
              .wrapper { max-width: 600px; margin: 40px auto; background: #fff; border-radius: 6px;
                         overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
              .header  { background: #e55353; padding: 30px; text-align: center; }
              .header h1 { color: #fff; margin: 0; font-size: 22px; }
              .body    { padding: 30px; color: #333; line-height: 1.7; }
              .detail-box { background: #f9f9f9; border-left: 4px solid #e55353;
                            padding: 15px 20px; margin: 20px 0; border-radius: 0 4px 4px 0; }
              .footer  { background: #f4f4f4; padding: 15px 30px; text-align: center;
                         font-size: 12px; color: #888; }
            </style>
            </head>
            <body>
              <div class="wrapper">
                <div class="header">
                  <h1>Update on Your Application</h1>
                </div>
                <div class="body">
                  <p>Dear %s,</p>
                  <p>Thank you for applying to the opportunity below. After careful consideration,
                     we regret to inform you that your application was <strong>not selected</strong>
                     at this time:</p>
                  <div class="detail-box">
                    <strong>Position:</strong> %s<br>
                    <strong>Company:</strong> %s
                  </div>
                  <p>We encourage you to continue exploring other opportunities on MindForge.
                     Don't give up — the right opportunity is out there!</p>
                  <p>Best regards,<br>The MindForge Team</p>
                </div>
                <div class="footer">
                  &copy; MindForge — This is an automated notification, please do not reply to this email.
                </div>
              </div>
            </body>
            </html>
            """.formatted(applicantName, opportunityTitle, companyName);
    }
}
