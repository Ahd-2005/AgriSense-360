package utils;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

public class EmailService {
    private static final String CONFIG_PATH = "/config.properties";
    private static final Properties configProps = new Properties();

    static {
        try (InputStream in = EmailService.class.getResourceAsStream(CONFIG_PATH)) {
            if (in != null) {
                configProps.load(in);
            }
        } catch (Exception ignored) {
        }
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(configProps.getProperty("mail.enabled", "false"));
    }

    private static Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", configProps.getProperty("mail.starttls.enable", "true"));
        props.put("mail.smtp.ssl.enable", configProps.getProperty("mail.ssl.enable", "false"));
        props.put("mail.smtp.host", configProps.getProperty("mail.host", ""));
        props.put("mail.smtp.port", configProps.getProperty("mail.port", "587"));

        final String username = configProps.getProperty("mail.username", "");
        final String password = configProps.getProperty("mail.password", "");

        return Session.getInstance(props, new jakarta.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    private static Message baseMessage(String subject) throws MessagingException {
        String to = configProps.getProperty("mail.to", "").trim();
        String from = configProps.getProperty("mail.from", "").trim();
        if (to.isEmpty() || from.isEmpty()) {
            throw new MessagingException("Missing mail.to or mail.from in config.properties");
        }
        Session session = getSession();
        Message message = new MimeMessage(session);

        // Parse "Display Name <email@example.com>" format safely
        InternetAddress fromAddress;
        try {
            InternetAddress[] parsed = InternetAddress.parse(from, false);
            if (parsed.length > 0) {
                fromAddress = parsed[0];
                // Re-encode personal name (display name) as UTF-8 to handle special chars
                if (fromAddress.getPersonal() != null) {
                    fromAddress = new InternetAddress(
                            fromAddress.getAddress(),
                            fromAddress.getPersonal(),
                            StandardCharsets.UTF_8.name()
                    );
                }
            } else {
                fromAddress = new InternetAddress(from);
            }
        } catch (UnsupportedEncodingException e) {
            fromAddress = new InternetAddress(from);
        }
        message.setFrom(fromAddress);
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject);
        return message;
    }

    // Plain-text email (backward-compatible)
    public static void send(String subject, String body) throws MessagingException {
        Message message = baseMessage(subject);
        message.setText(body);
        Transport.send(message);
    }

    // HTML email
    public static void sendHtml(String subject, String html) throws MessagingException {
        Message message = baseMessage(subject);
        // Set content as HTML with UTF-8 charset
        message.setContent(html, "text/html; charset=UTF-8");
        Transport.send(message);
    }
}