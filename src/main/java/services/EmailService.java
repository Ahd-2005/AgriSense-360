package services;

import jakarta.mail.*;
import jakarta.mail.internet.*;

import java.io.InputStream;
import java.util.Properties;

public class EmailService {

    private static String senderEmail;
    private static String senderPassword;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = EmailService.class
                .getClassLoader().getResourceAsStream("google-config.properties")) {
            Properties prop = new Properties();
            prop.load(input);
            senderEmail    = prop.getProperty("mail.sender.email");
            senderPassword = prop.getProperty("mail.sender.password");
        } catch (Exception e) {
            throw new RuntimeException("❌ Impossible de charger la config email", e);
        }
    }

    public static boolean isEnabled() {
        return senderEmail != null && !senderEmail.isBlank()
                && senderPassword != null && !senderPassword.isBlank();
    }

    // ============================================================
    // ENVOYER L'EMAIL DE RESET
    // ============================================================
    public static void sendResetCode(String toEmail, String userName, String resetCode) throws Exception {
        Properties props = buildSmtpProps();

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(senderEmail, "AgriSense 360"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("🔐 Réinitialisation de votre mot de passe - AgriSense 360");

        String htmlContent = buildResetEmailHtml(userName, resetCode);
        message.setContent(htmlContent, "text/html; charset=utf-8");

        Transport.send(message);
        System.out.println("✅ Email de reset envoyé à: " + toEmail);
    }

    // ============================================================
    // ENVOYER L'EMAIL DE REFUS DE CANDIDATURE  ← NEW
    // ============================================================
    public static void sendRejectionEmail(String toEmail, String userName,
                                          String farmName, String desiredRole) throws Exception {
        Properties props = buildSmtpProps();

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(senderEmail, "AgriSense 360"));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject("Votre candidature à " + farmName + " — Résultat");

        message.setContent(buildRejectionEmailHtml(userName, farmName, desiredRole),
                "text/html; charset=utf-8");

        Transport.send(message);
        System.out.println("✅ Email de refus envoyé à: " + toEmail);
    }

    // ============================================================
    // SHARED SMTP CONFIG
    // ============================================================
    private static Properties buildSmtpProps() {
        Properties props = new Properties();
        props.put("mail.smtp.host",            "smtp.gmail.com");
        props.put("mail.smtp.port",            "587");
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        return props;
    }

    // ============================================================
    // TEMPLATE HTML — RESET
    // ============================================================
    private static String buildResetEmailHtml(String userName, String resetCode) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f0f4f0; margin: 0; padding: 30px;">
                  <div style="max-width: 500px; margin: auto; background: white;
                              border-radius: 12px; padding: 40px;
                              box-shadow: 0 4px 20px rgba(0,0,0,0.1);">

                    <h2 style="color: #2e7d32; text-align: center;">🌱 AgriSense 360</h2>
                    <hr style="border-color: #e8f5e9;">

                    <p style="color: #333; font-size: 16px;">Bonjour <strong>%s</strong>,</p>

                    <p style="color: #555;">Vous avez demandé à réinitialiser votre mot de passe.
                    Voici votre code de vérification :</p>

                    <div style="text-align: center; margin: 30px 0;">
                      <span style="background: #e8f5e9; color: #2e7d32;
                                   font-size: 36px; font-weight: bold;
                                   letter-spacing: 10px; padding: 15px 30px;
                                   border-radius: 10px; border: 2px solid #4CAF50;">
                        %s
                      </span>
                    </div>

                    <p style="color: #555;">Ce code est valable pendant <strong>15 minutes</strong>.</p>
                    <p style="color: #999; font-size: 13px;">
                      Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.
                    </p>

                    <hr style="border-color: #e8f5e9;">
                    <p style="color: #aaa; font-size: 12px; text-align: center;">
                      AgriSense 360 — Smart Farm Management
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(userName, resetCode);
    }

    // ============================================================
    // TEMPLATE HTML — REJECTION
    // ============================================================
    private static String buildRejectionEmailHtml(String userName, String farmName, String desiredRole) {
        return """
                <!DOCTYPE html>
                <html>
                <head><meta charset="UTF-8"></head>
                <body style="font-family: Arial, sans-serif; background-color: #f0f4f0; margin: 0; padding: 30px;">
                  <div style="max-width: 500px; margin: auto; background: white;
                              border-radius: 12px; padding: 40px;
                              box-shadow: 0 4px 20px rgba(0,0,0,0.1);">

                    <h2 style="color: #2e7d32; text-align: center;">🌱 AgriSense 360</h2>
                    <hr style="border-color: #e8f5e9;">

                    <p style="color: #333; font-size: 16px;">Bonjour <strong>%s</strong>,</p>

                    <p style="color: #555;">
                      Nous avons bien examiné votre candidature pour le poste de
                      <strong>%s</strong> à la ferme <strong>« %s »</strong>.
                    </p>

                    <div style="background: #fff3f3; border-left: 4px solid #e53935;
                                border-radius: 8px; padding: 18px 22px; margin: 24px 0;">
                      <p style="margin: 0; color: #c62828; font-size: 15px;">
                        ❌ Nous avons le regret de vous informer que votre candidature
                        n'a pas été retenue pour ce poste.
                      </p>
                    </div>

                    <p style="color: #555;">
                      Ne vous découragez pas — d'autres fermes sont disponibles sur la
                      plateforme AgriSense 360. Nous vous encourageons à continuer à postuler !
                    </p>

                    <hr style="border-color: #e8f5e9;">
                    <p style="color: #aaa; font-size: 12px; text-align: center;">
                      AgriSense 360 — Smart Farm Management
                    </p>
                  </div>
                </body>
                </html>
                """.formatted(userName, desiredRole, farmName);
    }
}