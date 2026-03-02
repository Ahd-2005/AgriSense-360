package controllers;

import entity.AffectationTravail;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import services.AffectationTravailService;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

public class WorkersCalendarController implements Initializable {

    @FXML private GridPane dayHeaders;
    @FXML private GridPane calendarGrid;
    @FXML private Label lblCurrentMonth;

    private final AffectationTravailService service = new AffectationTravailService();
    private YearMonth currentMonth;
    private List<AffectationTravail> allAffectations = new ArrayList<>();

    private static final String[] DAY_NAMES = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentMonth = YearMonth.now();
        loadData();
        buildDayHeaders();
        buildCalendar();
    }

    private void loadData() {
        try {
            allAffectations = service.getAll();
        } catch (SQLException e) {
            System.err.println("[WorkersCalendar] Error: " + e.getMessage());
        }
    }

    private void buildDayHeaders() {
        dayHeaders.getChildren().clear();
        for (int i = 0; i < 7; i++) {
            Label lbl = new Label(DAY_NAMES[i]);
            lbl.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #2c3e50; " +
                         "-fx-alignment: center; -fx-padding: 8;");
            lbl.setMaxWidth(Double.MAX_VALUE);
            lbl.setAlignment(Pos.CENTER);
            dayHeaders.add(lbl, i, 0);
        }
    }

    private void buildCalendar() {
        calendarGrid.getChildren().clear();

        // Update month label
        String monthName = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.FRENCH);
        lblCurrentMonth.setText(monthName.substring(0, 1).toUpperCase() + monthName.substring(1) + " " + currentMonth.getYear());

        // Get the first day of month and total days
        LocalDate firstDay = currentMonth.atDay(1);
        int daysInMonth = currentMonth.lengthOfMonth();

        // Monday = 1, Sunday = 7 → column offset
        int startCol = firstDay.getDayOfWeek().getValue() - 1; // 0-based, Mon=0

        // Find affectations active this month
        Map<Integer, List<AffectationTravail>> dayAffectations = new HashMap<>();
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.atDay(day);
            List<AffectationTravail> active = allAffectations.stream()
                    .filter(a -> a.getDateDebut() != null && a.getDateFin() != null)
                    .filter(a -> !date.isBefore(a.getDateDebut()) && !date.isAfter(a.getDateFin()))
                    .collect(Collectors.toList());
            if (!active.isEmpty()) {
                dayAffectations.put(day, active);
            }
        }

        // Build cells
        int row = 0;
        int col = startCol;

        for (int day = 1; day <= daysInMonth; day++) {
            VBox cell = createDayCell(day, dayAffectations.get(day));
            calendarGrid.add(cell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createDayCell(int day, List<AffectationTravail> affectations) {
        VBox cell = new VBox(2);
        cell.setPadding(new Insets(4));
        cell.setMinHeight(80);
        cell.setMaxWidth(Double.MAX_VALUE);

        boolean isToday = currentMonth.atDay(day).equals(LocalDate.now());

        String bgColor = isToday ? "#e8f5e9" : "#fafafa";
        String borderColor = isToday ? "#4caf50" : "#e8e8e8";
        cell.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 6; " +
                      "-fx-border-color: " + borderColor + "; -fx-border-radius: 6; -fx-border-width: 1;");

        // Day number
        Label dayLabel = new Label(String.valueOf(day));
        dayLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: " + (isToday ? "bold" : "normal") +
                          "; -fx-text-fill: " + (isToday ? "#2e7d32" : "#555") + ";");
        cell.getChildren().add(dayLabel);

        // Affectation badges
        if (affectations != null) {
            for (AffectationTravail a : affectations) {
                Label badge = new Label(truncate(a.getTypeTravail(), 14));
                badge.setStyle("-fx-background-color: " + getStatusColor(a.getStatut()) +
                               "; -fx-text-fill: white; -fx-font-size: 9px; -fx-font-weight: bold; " +
                               "-fx-background-radius: 4; -fx-padding: 2 5 2 5; -fx-max-width: Infinity;");
                badge.setMaxWidth(Double.MAX_VALUE);

                Tooltip tooltip = new Tooltip(
                        a.getTypeTravail() + "\n" +
                        "Zone: " + a.getZoneTravail() + "\n" +
                        "Du " + a.getDateDebut() + " au " + a.getDateFin() + "\n" +
                        "Statut: " + a.getStatut()
                );
                Tooltip.install(badge, tooltip);

                cell.getChildren().add(badge);
            }
        }

        return cell;
    }

    private String getStatusColor(String statut) {
        if (statut == null) return "#95a5a6";
        return switch (statut) {
            case "En cours"   -> "#e67e22";
            case "Terminée"   -> "#27ae60";
            case "En attente" -> "#3498db";
            case "Annulée"    -> "#e74c3c";
            default           -> "#95a5a6";
        };
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() > max ? text.substring(0, max - 1) + "…" : text;
    }

    // ── Navigation ─────────────────────────────────────────────────

    @FXML
    private void onPrevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        buildCalendar();
    }

    @FXML
    private void onNextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        buildCalendar();
    }

    @FXML
    private void onSwitchToAffectation() {
        MainLayoutController.getInstance().navigateToWorkers();
    }

    @FXML
    private void onSwitchToEvaluation() {
        MainLayoutController.getInstance().navigateToEvaluation();
    }

    @FXML
    private void onSwitchToDashboard() {
        MainLayoutController.getInstance().navigateToDashboardWorkers();
    }
}
