package services;

import entity.Culture;
import utils.EmailService;

import jakarta.mail.MessagingException;
import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CultureNotificationService {
    private final CultureService cultureService = new CultureService();

    public void sendHarvestDayNotifications() {
        if (!EmailService.isEnabled()) {
            return; // Email disabled -> no-op
        }
        List<Culture> all;
        try {
            all = cultureService.getAllCultures();
        } catch (java.sql.SQLException e) {
            return; // DB error: skip silently (could log)
        }
        LocalDate today = LocalDate.now();
        StringJoiner lines = new StringJoiner("\n");
        java.util.List<Culture> todays = new java.util.ArrayList<>();
        for (Culture c : all) {
            if (c.getDateRecolte() != null && c.getDateRecolte().toLocalDate().isEqual(today)) {
                lines.add(formatLine(c));
                todays.add(c);
            }
        }
        if (todays.isEmpty()) return; // Nothing today

        String subject = "Rappel Récolte – Cultures à récolter aujourd’hui";
        String html = buildHtmlEmail(todays, today);
        try {
            EmailService.sendHtml(subject, html);
        } catch (MessagingException e) {
            // Swallow to avoid crashing the app; you may log this if you have a logger
        }
    }

    private String formatLine(Culture c) {
        String parcelle = "#" + c.getParcelleId();
        String surface = c.getSurface() > 0 ? (String.format("%.2f ha", c.getSurface())) : "";
        String type = c.getTypeCulture() != null ? c.getTypeCulture() : "";
        StringBuilder sb = new StringBuilder();
        sb.append(" - ").append(c.getNom() != null ? c.getNom() : "Culture");
        if (!type.isBlank()) sb.append(" (type: ").append(type).append(")");
        sb.append(" – Parcelle: ").append(parcelle);
        if (!surface.isBlank()) sb.append(" – Surface: ").append(surface);
        return sb.toString();
    }

    private String buildHtmlEmail(List<Culture> todays, LocalDate today) {
        String primary = "#2e7d32";   // green
        String accent  = "#66bb6a";   // light green
        String danger  = "#e53935";   // red for today badge
        String gray700 = "#37474f";
        String gray500 = "#607d8b";
        String bg      = "#f5f7fb";

        StringBuilder html = new StringBuilder();
        html.append("<!doctype html><html lang=\"fr\"><head>")
            .append("<meta charset=\"UTF-8\">")
            .append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">")
            .append("<title>Récoltes du ").append(today).append("</title>")
            .append("</head><body style=\"margin:0;padding:0;background:").append(bg).append(";font-family:Segoe UI,Roboto,Arial,sans-serif;color:").append(gray700).append(";\">");

        // Container
        html.append("<div style=\"max-width:720px;margin:24px auto;padding:0 12px;\">");

        // Header card
        html.append("<div style=\"background:white;border-radius:14px;box-shadow:0 6px 18px rgba(0,0,0,.08);overflow:hidden;\">")
            .append("<div style=\"background:").append(primary).append(";padding:18px 22px;color:white;display:flex;align-items:center;justify-content:space-between;\">")
            .append("<div style=\"font-size:18px;font-weight:600;letter-spacing:.2px;\">AgriSense‑360 · Récolte aujourd’hui</div>")
            .append("<span style=\"background:").append(danger).append(";padding:6px 10px;border-radius:999px;font-size:12px;font-weight:700;letter-spacing:.3px;\">Aujourd’hui ")
            .append(htmlEscape(today.toString()))
            .append("</span></div>")
            .append("<div style=\"padding:16px 22px;color:").append(gray500).append(";font-size:14px;\">Voici les cultures dont la date de récolte est prévue pour aujourd’hui. Bon courage !</div>")
            .append("</div>");

        // Cards grid
        for (Culture c : todays) {
            String name = htmlEscape(nonNullOr(c.getNom(), "Culture"));
            String type = htmlEscape(nonNullOr(c.getTypeCulture(), "—"));
            String parcelle = "#" + c.getParcelleId();
            String surface = c.getSurface() > 0 ? String.format("%.2f ha", c.getSurface()) : "—";
            String etat = htmlEscape(nonNullOr(c.getEtat(), "—"));
            String dPlant = c.getDatePlantation() != null ? c.getDatePlantation().toString() : "—";
            String dRec   = c.getDateRecolte() != null ? c.getDateRecolte().toString() : "—";

            html.append("<div style=\"margin-top:16px;background:white;border:1px solid #e6ebf1;border-radius:12px;box-shadow:0 4px 12px rgba(0,0,0,.05);overflow:hidden;\">")
                .append("<div style=\"display:flex;align-items:center;gap:12px;padding:14px 16px;background:linear-gradient(90deg, ")
                .append(accent).append("22, #ffffff 80%);border-bottom:1px solid #eef2f6;\">")
                .append("<div style=\"width:10px;height:10px;border-radius:50%;background:").append(primary).append(";box-shadow:0 0 0 3px ").append(accent).append("33;\"></div>")
                .append("<div style=\"font-weight:700;color:").append(gray700).append(";font-size:15px;\">")
                .append(name)
                .append("</div>")
                .append("<div style=\"margin-left:auto;background:").append(primary).append("0f;color:").append(primary).append(";border:1px solid ").append(primary).append("33;padding:4px 8px;border-radius:999px;font-size:12px;font-weight:600;\">")
                .append("Parcelle ").append(htmlEscape(parcelle))
                .append("</div>")
                .append("</div>");

            // Body rows
            html.append("<div style=\"padding:14px 16px;\">")
                .append(infoRow("Type", type))
                .append(infoRow("Surface", htmlEscape(surface)))
                .append(infoRow("État", etat))
                .append(infoRow("Plantation", htmlEscape(dPlant)))
                .append(infoRow("Récolte", htmlEscape(dRec)))
                .append("</div>");

            html.append("</div>");
        }

        // Footer
        html.append("<div style=\"text-align:center;color:").append(gray500).append(";font-size:12px;margin:22px 0;\">© AgriSense‑360 · Notification automatique — vous pouvez désactiver dans config.properties</div>");

        html.append("</div></body></html>");
        return html.toString();
    }

    private String infoRow(String label, String value) {
        return "<div style=\"display:flex;align-items:center;gap:10px;padding:6px 0;border-bottom:1px dashed #eef2f6;\">"
                + "<div style=\"min-width:110px;color:#546e7a;font-weight:600;\">" + htmlEscape(label) + "</div>"
                + "<div style=\"flex:1;color:#263238;\">" + (value == null ? "—" : value) + "</div>"
                + "</div>";
    }

    private String nonNullOr(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }

    private String htmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    // Utility: schedule daily at fixed hour (10:00 local), plus immediate run
    public void scheduleDailyAtTen() {
        ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "HarvestNotifyScheduler");
            t.setDaemon(true);
            return t;
        });

        // Immediate run at startup
        exec.execute(this::sendHarvestDayNotifications);

        // Compute initial delay until next 10:00 local
        long initialDelay = computeInitialDelayToTen();
        long period = TimeUnit.DAYS.toSeconds(1);
        exec.scheduleAtFixedRate(this::sendHarvestDayNotifications, initialDelay, period, TimeUnit.SECONDS);
    }

    private long computeInitialDelayToTen() {
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now();
        java.time.ZonedDateTime nextRun = now.withHour(10).withMinute(0).withSecond(0).withNano(0);
        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1);
        }
        return java.time.Duration.between(now, nextRun).getSeconds();
    }
}
