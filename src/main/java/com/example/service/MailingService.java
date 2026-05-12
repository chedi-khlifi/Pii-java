package com.example.service;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;

public class MailingService {

    // SMTP credentials loaded from config.properties
    private static final String SMTP_HOST = ConfigLoader.get("mail.smtp.host", "smtp.gmail.com");
    private static final String SMTP_PORT = ConfigLoader.get("mail.smtp.port", "587");
    private static final String SENDER_EMAIL = ConfigLoader.get("mail.sender.email");
    private static final String APP_PASSWORD = ConfigLoader.get("mail.app.password");
    private static final String INBOX_URL = ConfigLoader.get("mail.inbox.url", "http://localhost:8080/");

    public void sendChallengeEmail(String to, String title, String fromUser) {
        if (SENDER_EMAIL == null || APP_PASSWORD == null) return;

        String subject = "Nouveau Défi MindForge : " + title;
        String html = buildChallengeEmailHtml(title, fromUser);
        sendHtmlEmail(to, subject, html);
    }

    public void sendStatusUpdateEmail(String to, String title, String status) {
        if (SENDER_EMAIL == null || APP_PASSWORD == null) return;

        String subject = "Mise à jour de votre défi : " + title;
        String html = buildStatusEmailHtml(title, status);
        sendHtmlEmail(to, subject, html);
    }

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_PORT);

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SENDER_EMAIL, APP_PASSWORD);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SENDER_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText("MindForge notification: open this email in HTML mode for the full design.", "UTF-8");

            MimeBodyPart htmlPart = new MimeBodyPart();
            htmlPart.setContent(htmlBody, "text/html; charset=UTF-8");

            MimeMultipart alternative = new MimeMultipart("alternative");
            alternative.addBodyPart(textPart);
            alternative.addBodyPart(htmlPart);

            message.setContent(alternative);
            message.saveChanges();
            Transport.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private String buildChallengeEmailHtml(String title, String fromUser) {
        String safeTitle = escapeHtml(title);
        String safeFromUser = escapeHtml(fromUser);
        String safeUrl = escapeHtml(INBOX_URL);

        return """
                <html>
                  <body style="margin:0;padding:0;background:#f4f6fb;font-family:'Segoe UI',Arial,sans-serif;color:#14213d;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:24px 0;">
                      <tr>
                        <td align="center">
                          <table role="presentation" width="640" cellspacing="0" cellpadding="0" style="max-width:640px;background:#ffffff;border:1px solid #e8ecf4;border-radius:12px;overflow:hidden;">
                            <tr>
                              <td align="center" style="background:#5865f2;padding:28px 20px;">
                                <div style="font-size:42px;line-height:1;margin-bottom:8px;">🎯</div>
                                <div style="color:#ffffff;font-weight:800;font-size:40px;line-height:1.05;">MindForge Community</div>
                                <div style="color:#dbe4ff;font-size:11px;margin-top:4px;">Template v2</div>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:34px 30px 22px 30px;">
                                <div style="font-size:38px;font-weight:800;color:#0f172a;margin-bottom:16px;">Bonjour !</div>
                                <p style="margin:0 0 20px 0;font-size:28px;line-height:1.45;color:#1f2937;">
                                  Bonne nouvelle ! <strong>%s</strong> vient de vous lancer un nouveau défi d'apprentissage.
                                </p>
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#fbfcff;border-left:6px solid #5865f2;border-radius:6px;">
                                  <tr>
                                    <td style="padding:16px 18px;">
                                      <div style="font-size:17px;letter-spacing:0.4px;font-weight:900;color:#5865f2;text-transform:uppercase;">Titre du Défi</div>
                                      <div style="margin-top:8px;font-size:40px;font-weight:800;color:#0f172a;line-height:1.15;">%s</div>
                                    </td>
                                  </tr>
                                </table>
                                <p style="margin:24px 0 30px 0;font-size:28px;line-height:1.45;color:#334155;">
                                  Prêt à relever le challenge ? Connectez-vous dès maintenant pour l'accepter !
                                </p>
                                <div style="text-align:center;">
                                  <a href="%s" style="display:inline-block;background:#5865f2;color:#ffffff !important;text-decoration:none;font-size:30px;font-weight:700;padding:16px 30px;border-radius:10px;">
                                    Voir mon Inbox
                                  </a>
                                </div>
                              </td>
                            </tr>
                            <tr>
                              <td style="background:#f8fafe;padding:16px;text-align:center;color:#94a3b8;font-size:22px;line-height:1.4;">
                                Ceci est une notification automatique de MindForge.<br/>
                                Ne répondez pas directement à cet email.
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(safeFromUser, safeTitle, safeUrl);
    }

    private String buildStatusEmailHtml(String title, String status) {
        String safeTitle = escapeHtml(title);
        boolean accepted = "ACCEPTED".equalsIgnoreCase(status);
        String label = accepted ? "ACCEPTÉ ✅" : "REFUSÉ ❌";
        String accent = accepted ? "#22c55e" : "#ef4444";

        return """
                <html>
                  <body style="margin:0;padding:0;background:#f4f6fb;font-family:'Segoe UI',Arial,sans-serif;color:#14213d;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="padding:24px 0;">
                      <tr>
                        <td align="center">
                          <table role="presentation" width="640" cellspacing="0" cellpadding="0" style="max-width:640px;background:#ffffff;border:1px solid #e8ecf4;border-radius:12px;overflow:hidden;">
                            <tr>
                              <td align="center" style="background:#5865f2;padding:24px 20px;">
                                <div style="color:#ffffff;font-weight:800;font-size:36px;">MindForge Community</div>
                              </td>
                            </tr>
                            <tr>
                              <td style="padding:28px 30px;">
                                <div style="font-size:32px;font-weight:800;color:#0f172a;margin-bottom:14px;">Mise à jour du défi</div>
                                <p style="margin:0 0 16px 0;font-size:24px;line-height:1.4;color:#1f2937;">
                                  Votre défi a reçu une réponse.
                                </p>
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0" style="background:#fbfcff;border-left:6px solid %s;border-radius:6px;">
                                  <tr>
                                    <td style="padding:16px 18px;">
                                      <div style="font-size:16px;letter-spacing:0.4px;font-weight:900;color:#5865f2;text-transform:uppercase;">Titre du Défi</div>
                                      <div style="margin-top:8px;font-size:30px;font-weight:800;color:#0f172a;line-height:1.2;">%s</div>
                                      <div style="margin-top:10px;font-size:24px;font-weight:800;color:%s;">%s</div>
                                    </td>
                                  </tr>
                                </table>
                              </td>
                            </tr>
                            <tr>
                              <td style="background:#f8fafe;padding:16px;text-align:center;color:#94a3b8;font-size:18px;line-height:1.4;">
                                Notification automatique MindForge
                              </td>
                            </tr>
                          </table>
                        </td>
                      </tr>
                    </table>
                  </body>
                </html>
                """.formatted(accent, safeTitle, accent, label);
    }

    private String escapeHtml(String value) {
        if (value == null) return "";
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
