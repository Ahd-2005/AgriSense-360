package controllers;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import entity.Parcelle;
import entity.Parcellehistorique;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import services.Parcellehistoriqueservice;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Popup stylé d'historique d'une parcelle avec export PDF.
 *
 * Usage dans ParcelleController:
 *   new ParcellehistoriqueController().show(parentStage, parcelle);
 */
public class ParcelleHistoriqueController {

    private final Parcellehistoriqueservice historiqueService = new Parcellehistoriqueservice();
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Color palette ──────────────────────────────────────────────────────────
    private static final String BG_DARK      = "#0F1923";
    private static final String BG_CARD      = "#1A2635";
    private static final String BG_HEADER    = "#162130";
    private static final String ACCENT_GREEN = "#2ECC71";
    private static final String ACCENT_AMBER = "#F39C12";
    private static final String ACCENT_RED   = "#E74C3C";
    private static final String ACCENT_BLUE  = "#3498DB";
    private static final String ACCENT_TEAL  = "#1ABC9C";
    private static final String TEXT_WHITE   = "#ECF0F1";
    private static final String TEXT_MUTED   = "#8FA3B1";

    // ──────────────────────────────────────────────────────────────────────────
    public void show(Stage owner, Parcelle parcelle) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(owner);
        stage.setTitle("📋 Historique — " + parcelle.getNom());
        stage.setMinWidth(860);
        stage.setMinHeight(680);

        List<Parcellehistorique> historique;
        try {
            historique = historiqueService.getHistoriqueByParcelle(parcelle.getId());
        } catch (Exception e) {
            showError("Impossible de charger l'historique: " + e.getMessage());
            return;
        }

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG_DARK + ";");

        // ── TOP HEADER ────────────────────────────────────────────────────────
        root.setTop(buildHeader(parcelle, historique.size(), stage));

        // ── CENTER: Timeline ──────────────────────────────────────────────────
        ScrollPane scroll = new ScrollPane(buildTimeline(historique));
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + BG_DARK + "; -fx-background-color: " + BG_DARK + "; " +
                "-fx-border-color: transparent;");
        scroll.setPadding(new Insets(10, 20, 10, 20));
        root.setCenter(scroll);

        // ── BOTTOM: Stats bar + Export ────────────────────────────────────────
        root.setBottom(buildBottomBar(parcelle, historique, stage));

        Scene scene = new Scene(root, 880, 700);
        stage.setScene(scene);
        stage.show();
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private HBox buildHeader(Parcelle parcelle, int count, Stage stage) {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 24, 16, 24));
        header.setStyle("-fx-background-color: " + BG_HEADER + "; " +
                "-fx-border-color: #1E3A50; -fx-border-width: 0 0 1 0;");

        // Icon circle
        StackPane iconCircle = new StackPane();
        Circle circle = new Circle(26);
        circle.setFill(Color.web(ACCENT_GREEN, 0.15));
        circle.setStroke(Color.web(ACCENT_GREEN));
        circle.setStrokeWidth(1.5);
        Label iconLbl = new Label("📋");
        iconLbl.setStyle("-fx-font-size: 18px;");
        iconCircle.getChildren().addAll(circle, iconLbl);

        // Title block
        VBox titleBlock = new VBox(3);
        Label titleLbl = new Label("Historique de la Parcelle");
        titleLbl.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 12px;");
        Label nameLbl = new Label(parcelle.getNom());
        nameLbl.setStyle("-fx-text-fill: " + TEXT_WHITE + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label subLbl  = new Label("📍 " + parcelle.getLocalisation() + "  ·  " + count + " événement(s)");
        subLbl.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 12px;");
        titleBlock.getChildren().addAll(titleLbl, nameLbl, subLbl);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Close button
        Button closeBtn = new Button("✕");
        closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED +
                "; -fx-font-size: 16px; -fx-cursor: hand;");
        closeBtn.setOnMouseEntered(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + ACCENT_RED + "; -fx-font-size: 16px; -fx-cursor: hand;"));
        closeBtn.setOnMouseExited(e -> closeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 16px; -fx-cursor: hand;"));
        closeBtn.setOnAction(e -> stage.close());

        header.getChildren().addAll(iconCircle, titleBlock, spacer, closeBtn);
        return header;
    }

    // ── Timeline ──────────────────────────────────────────────────────────────
    private VBox buildTimeline(List<Parcellehistorique> items) {
        VBox timeline = new VBox(0);
        timeline.setPadding(new Insets(16, 8, 24, 8));

        if (items.isEmpty()) {
            VBox empty = new VBox(12);
            empty.setAlignment(Pos.CENTER);
            empty.setPadding(new Insets(60));
            Label emptyIcon = new Label("🌿");
            emptyIcon.setStyle("-fx-font-size: 48px;");
            Label emptyLbl = new Label("Aucun événement enregistré pour cette parcelle.");
            emptyLbl.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 14px;");
            empty.getChildren().addAll(emptyIcon, emptyLbl);
            timeline.getChildren().add(empty);
            return timeline;
        }

        String currentDate = null;

        for (Parcellehistorique h : items) {
            String dateKey = h.getDateAction() != null ?
                    h.getDateAction().format(DateTimeFormatter.ofPattern("dd MMMM yyyy",
                            java.util.Locale.FRENCH)) : "Date inconnue";

            // Date separator
            if (!dateKey.equals(currentDate)) {
                currentDate = dateKey;
                HBox separator = buildDateSeparator(dateKey);
                timeline.getChildren().add(separator);
            }

            // Timeline item
            timeline.getChildren().add(buildTimelineItem(h));
        }
        return timeline;
    }

    private HBox buildDateSeparator(String dateStr) {
        HBox sep = new HBox(12);
        sep.setAlignment(Pos.CENTER_LEFT);
        sep.setPadding(new Insets(18, 0, 8, 10));

        Region line1 = new Region();
        line1.setPrefWidth(24);
        line1.setPrefHeight(1);
        line1.setStyle("-fx-background-color: #2A3F55;");

        Label dateLbl = new Label(dateStr.toUpperCase());
        dateLbl.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 11px; -fx-font-weight: bold; " +
                "-fx-padding: 3 8; -fx-background-color: #1E3248; -fx-background-radius: 10;");

        Region line2 = new Region();
        HBox.setHgrow(line2, Priority.ALWAYS);
        line2.setPrefHeight(1);
        line2.setStyle("-fx-background-color: #2A3F55;");

        sep.getChildren().addAll(line1, dateLbl, line2);
        return sep;
    }

    private HBox buildTimelineItem(Parcellehistorique h) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.TOP_LEFT);
        row.setPadding(new Insets(4, 8, 4, 8));

        // Left: dot + vertical line
        VBox leftLine = new VBox(0);
        leftLine.setAlignment(Pos.TOP_CENTER);
        leftLine.setMinWidth(36);

        StackPane dot = buildDot(h.getTypeAction());
        Region vline = new Region();
        vline.setPrefWidth(2);
        vline.setPrefHeight(60);
        vline.setStyle("-fx-background-color: #1E3248;");
        leftLine.getChildren().addAll(dot, vline);

        // Right: card
        VBox card = buildEventCard(h);
        HBox.setHgrow(card, Priority.ALWAYS);

        row.getChildren().addAll(leftLine, card);
        return row;
    }

    private StackPane buildDot(String typeAction) {
        StackPane sp = new StackPane();
        String color = getActionColor(typeAction);
        Circle outer = new Circle(14);
        outer.setFill(Color.web(color, 0.15));
        outer.setStroke(Color.web(color, 0.4));
        outer.setStrokeWidth(1);
        Circle inner = new Circle(6);
        inner.setFill(Color.web(color));

        Label icon = new Label(getActionEmoji(typeAction));
        icon.setStyle("-fx-font-size: 10px;");

        sp.getChildren().addAll(outer, inner);
        return sp;
    }

    private VBox buildEventCard(Parcellehistorique h) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: " + BG_CARD + "; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: " + getActionColor(h.getTypeAction()) + "33; " +
                "-fx-border-radius: 10; -fx-border-width: 1;");
        card.setPadding(new Insets(12, 16, 12, 16));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1F3045; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: " + getActionColor(h.getTypeAction()) + "; " +
                "-fx-border-radius: 10; -fx-border-width: 1;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: " + BG_CARD + "; " +
                "-fx-background-radius: 10; " +
                "-fx-border-color: " + getActionColor(h.getTypeAction()) + "33; " +
                "-fx-border-radius: 10; -fx-border-width: 1;"));

        // Row 1: Badge + Culture name + time
        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(getActionEmoji(h.getTypeAction()) + "  " + h.getTypeLabelFr().toUpperCase());
        badge.setStyle("-fx-text-fill: " + getActionColor(h.getTypeAction()) + "; " +
                "-fx-font-size: 10px; -fx-font-weight: bold; " +
                "-fx-padding: 3 8; " +
                "-fx-background-color: " + getActionColor(h.getTypeAction()) + "1A; " +
                "-fx-background-radius: 8;");

        Label cultureName = new Label(h.getCultureNom());
        cultureName.setStyle("-fx-text-fill: " + TEXT_WHITE + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        String timeStr = h.getDateAction() != null ? h.getDateAction().format(DISPLAY_FMT) : "";
        Label timeLbl = new Label("🕐 " + timeStr);
        timeLbl.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 11px;");

        topRow.getChildren().addAll(badge, cultureName, sp, timeLbl);

        // Row 2: Description
        Label descLbl = new Label(h.getDescription() != null ? h.getDescription() : "");
        descLbl.setStyle("-fx-text-fill: " + TEXT_MUTED + "; -fx-font-size: 12px;");
        descLbl.setWrapText(true);

        // Row 3: Metadata chips
        HBox chips = new HBox(8);
        chips.setAlignment(Pos.CENTER_LEFT);

        if (h.getTypeCulture() != null) {
            chips.getChildren().add(buildChip("🌿 " + h.getTypeCulture(), "#2C3E50", TEXT_MUTED));
        }
        if (h.getSurface() != null) {
            chips.getChildren().add(buildChip("📏 " + h.getSurface() + " m²", "#1A2635", ACCENT_BLUE));
        }
        if (h.getEtatAvant() != null && h.getEtatApres() != null) {
            chips.getChildren().add(buildChip(
                    h.getEtatAvant() + " → " + h.getEtatApres(), "#2C1A35", "#A855F7"));
        } else if (h.getEtatApres() != null) {
            chips.getChildren().add(buildChip("État: " + h.getEtatApres(), "#1A2635", ACCENT_GREEN));
        }
        if (h.getQuantiteRecolte() != null) {
            chips.getChildren().add(buildChip("🌾 " + h.getQuantiteRecolte() + " kg récoltés",
                    "#1A2B1A", ACCENT_GREEN));
        }

        card.getChildren().addAll(topRow, descLbl);
        if (!chips.getChildren().isEmpty()) card.getChildren().add(chips);

        return card;
    }

    private Label buildChip(String text, String bg, String fg) {
        Label chip = new Label(text);
        chip.setStyle("-fx-background-color: " + bg + "; -fx-text-fill: " + fg + "; " +
                "-fx-font-size: 11px; -fx-padding: 3 8; -fx-background-radius: 10;");
        return chip;
    }

    // ── Bottom bar ────────────────────────────────────────────────────────────
    private HBox buildBottomBar(Parcelle parcelle, List<Parcellehistorique> historique, Stage stage) {
        HBox bar = new HBox(16);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.setStyle("-fx-background-color: " + BG_HEADER + "; " +
                "-fx-border-color: #1E3A50; -fx-border-width: 1 0 0 0;");

        // Stats
        long ajoutees  = historique.stream().filter(h -> "CULTURE_AJOUTEE".equals(h.getTypeAction())).count();
        long modifiees = historique.stream().filter(h -> "CULTURE_MODIFIEE".equals(h.getTypeAction())).count();
        long supprimees= historique.stream().filter(h -> "CULTURE_SUPPRIMEE".equals(h.getTypeAction())).count();
        long recoltes  = historique.stream().filter(h -> "RECOLTE".equals(h.getTypeAction())).count();

        bar.getChildren().addAll(
                buildStatChip("🌱 Ajoutées", ajoutees, ACCENT_GREEN),
                buildStatChip("✏️ Modifiées", modifiees, ACCENT_AMBER),
                buildStatChip("🗑 Supprimées", supprimees, ACCENT_RED),
                buildStatChip("🌾 Récoltes", recoltes, ACCENT_TEAL)
        );

        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);

        // Export PDF button
        Button exportBtn = new Button("⬇  Exporter PDF");
        exportBtn.setStyle("-fx-background-color: " + ACCENT_GREEN + "; -fx-text-fill: #0F1923; " +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 9 20; " +
                "-fx-background-radius: 8; -fx-cursor: hand;");
        exportBtn.setOnMouseEntered(e -> exportBtn.setStyle("-fx-background-color: #27AE60; -fx-text-fill: #0F1923; " +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 9 20; -fx-background-radius: 8; -fx-cursor: hand;"));
        exportBtn.setOnMouseExited(e -> exportBtn.setStyle("-fx-background-color: " + ACCENT_GREEN + "; -fx-text-fill: #0F1923; " +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 9 20; -fx-background-radius: 8; -fx-cursor: hand;"));
        exportBtn.setOnAction(e -> exportPdf(parcelle, historique, stage));

        bar.getChildren().addAll(sp, exportBtn);
        return bar;
    }

    private Label buildStatChip(String label, long count, String color) {
        Label chip = new Label(label + "  " + count);
        chip.setStyle("-fx-background-color: " + color + "1A; -fx-text-fill: " + color + "; " +
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 5 12; " +
                "-fx-background-radius: 12;");
        return chip;
    }

    // ── PDF Export ────────────────────────────────────────────────────────────
    private void exportPdf(Parcelle parcelle, List<Parcellehistorique> historique, Stage stage) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Enregistrer le PDF");
        fc.setInitialFileName("historique_" + parcelle.getNom().replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        File file = fc.showSaveDialog(stage);
        if (file == null) return;

        try {
            generatePdf(parcelle, historique, file.getAbsolutePath());
            showSuccess("PDF exporté avec succès :\n" + file.getAbsolutePath());
        } catch (Exception e) {
            showError("Erreur export PDF: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generatePdf(Parcelle parcelle, List<Parcellehistorique> historique, String path) throws Exception {
        PdfWriter writer = new PdfWriter(path);
        PdfDocument pdf = new PdfDocument(writer);
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        DeviceRgb green     = new DeviceRgb(46, 204, 113);
        DeviceRgb darkBg    = new DeviceRgb(15, 25, 35);
        DeviceRgb cardBg    = new DeviceRgb(26, 38, 53);
        DeviceRgb textWhite = new DeviceRgb(236, 240, 241);
        DeviceRgb textMuted = new DeviceRgb(143, 163, 177);
        DeviceRgb amber     = new DeviceRgb(243, 156, 18);
        DeviceRgb red       = new DeviceRgb(231, 76, 60);
        DeviceRgb blue      = new DeviceRgb(52, 152, 219);
        DeviceRgb teal      = new DeviceRgb(26, 188, 156);

        // ── Page background ──────────────────────────────────────────────────
        pdf.addNewPage();

        // ── Header block ─────────────────────────────────────────────────────
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1})).useAllAvailableWidth();
        Cell headerCell = new Cell()
                .setBackgroundColor(darkBg)
                .setBorder(Border.NO_BORDER)
                .setPadding(20)
                .add(new Paragraph("📋  HISTORIQUE DE LA PARCELLE")
                        .setFontSize(10).setFontColor(textMuted).setBold())
                .add(new Paragraph(parcelle.getNom())
                        .setFontSize(24).setFontColor(textWhite).setBold()
                        .setMarginTop(4))
                .add(new Paragraph("📍 " + parcelle.getLocalisation()
                        + "   ·   Surface: " + parcelle.getSurface() + " m²"
                        + "   ·   Sol: " + parcelle.getTypeSol()
                        + "   ·   Statut: " + parcelle.getStatut())
                        .setFontSize(10).setFontColor(textMuted).setMarginTop(6))
                .add(new Paragraph("Généré le " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")))
                        .setFontSize(9).setFontColor(textMuted).setMarginTop(4));

        // Green bottom border
        headerCell.setBorderBottom(new SolidBorder(green, 3));
        headerTable.addCell(headerCell);
        doc.add(headerTable);
        doc.add(new Paragraph(" "));

        // ── Stats row ─────────────────────────────────────────────────────────
        long ajoutees   = historique.stream().filter(h -> "CULTURE_AJOUTEE".equals(h.getTypeAction())).count();
        long modifiees  = historique.stream().filter(h -> "CULTURE_MODIFIEE".equals(h.getTypeAction())).count();
        long supprimees = historique.stream().filter(h -> "CULTURE_SUPPRIMEE".equals(h.getTypeAction())).count();
        long recoltes   = historique.stream().filter(h -> "RECOLTE".equals(h.getTypeAction())).count();

        Table statsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1})).useAllAvailableWidth();
        statsTable.addCell(buildPdfStatCell("🌱 Cultures ajoutées", String.valueOf(ajoutees), green));
        statsTable.addCell(buildPdfStatCell("✏ Modifications", String.valueOf(modifiees), amber));
        statsTable.addCell(buildPdfStatCell("🗑 Suppressions", String.valueOf(supprimees), red));
        statsTable.addCell(buildPdfStatCell("🌾 Récoltes", String.valueOf(recoltes), teal));
        doc.add(statsTable);
        doc.add(new Paragraph(" "));

        // ── Events table ───────────────────────────────────────────────────────
        Paragraph sectionTitle = new Paragraph("Détail des Événements")
                .setFontSize(13).setFontColor(textWhite).setBold()
                .setBorderBottom(new SolidBorder(green, 1))
                .setPaddingBottom(6)
                .setMarginBottom(12);
        doc.add(sectionTitle);

        if (historique.isEmpty()) {
            doc.add(new Paragraph("Aucun événement enregistré pour cette parcelle.")
                    .setFontColor(textMuted).setFontSize(11).setItalic());
        } else {
            for (Parcellehistorique h : historique) {
                DeviceRgb actionColor = getActionColorRgb(h.getTypeAction(), green, amber, red, teal);
                Table evtTable = new Table(UnitValue.createPercentArray(new float[]{0.04f, 0.96f})).useAllAvailableWidth();
                evtTable.setMarginBottom(8);

                // Color accent bar (left column)
                Cell colorBar = new Cell()
                        .setBackgroundColor(actionColor)
                        .setBorder(Border.NO_BORDER)
                        .setPadding(0);
                evtTable.addCell(colorBar);

                // Content cell (right column)
                Cell content = new Cell()
                        .setBackgroundColor(cardBg)
                        .setBorder(Border.NO_BORDER)
                        .setPadding(12);

                // Top row: badge + time
                Table innerTop = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
                innerTop.setBorder(Border.NO_BORDER);
                Cell badgeCell = new Cell().setBorder(Border.NO_BORDER)
                        .add(new Paragraph(h.getTypeIcon() + "  " + h.getTypeLabelFr().toUpperCase())
                                .setFontSize(9).setFontColor(actionColor).setBold());
                String timeStr = h.getDateAction() != null ? h.getDateAction().format(DISPLAY_FMT) : "";
                Cell timeCell = new Cell().setBorder(Border.NO_BORDER)
                        .setTextAlignment(TextAlignment.RIGHT)
                        .add(new Paragraph(timeStr).setFontSize(9).setFontColor(textMuted));
                innerTop.addCell(badgeCell);
                innerTop.addCell(timeCell);
                content.add(innerTop);

                // Culture name
                content.add(new Paragraph(h.getCultureNom())
                        .setFontSize(13).setFontColor(textWhite).setBold().setMarginTop(4));

                // Description
                if (h.getDescription() != null && !h.getDescription().isEmpty()) {
                    content.add(new Paragraph(h.getDescription())
                            .setFontSize(10).setFontColor(textMuted).setMarginTop(4));
                }

                // Chips row
                StringBuilder chips = new StringBuilder();
                if (h.getTypeCulture() != null) chips.append("Type: ").append(h.getTypeCulture()).append("  ");
                if (h.getSurface() != null)      chips.append("Surface: ").append(h.getSurface()).append(" m²  ");
                if (h.getEtatAvant() != null && h.getEtatApres() != null)
                    chips.append("État: ").append(h.getEtatAvant()).append(" → ").append(h.getEtatApres()).append("  ");
                else if (h.getEtatApres() != null)
                    chips.append("État: ").append(h.getEtatApres()).append("  ");
                if (h.getQuantiteRecolte() != null)
                    chips.append("Récolte: ").append(h.getQuantiteRecolte()).append(" kg");

                if (chips.length() > 0) {
                    content.add(new Paragraph(chips.toString())
                            .setFontSize(9).setFontColor(textMuted).setMarginTop(6));
                }

                evtTable.addCell(content);
                doc.add(evtTable);
            }
        }

        // ── Footer ────────────────────────────────────────────────────────────
        doc.add(new Paragraph(" "));
        doc.add(new Paragraph("AgriSense 360  ·  Historique exporté automatiquement")
                .setFontSize(8).setFontColor(textMuted)
                .setTextAlignment(TextAlignment.CENTER)
                .setBorderTop(new SolidBorder(textMuted, 0.5f))
                .setPaddingTop(8));

        doc.close();
    }

    private Cell buildPdfStatCell(String label, String value, DeviceRgb color) {
        DeviceRgb bg = new DeviceRgb(
                Math.max(0, color.getColorValue()[0] - 180),
                Math.max(0, color.getColorValue()[1] - 180),
                Math.max(0, color.getColorValue()[2] - 180)
        );
        Cell cell = new Cell()
                .setBackgroundColor(new DeviceRgb(26, 38, 53))
                .setBorder(new SolidBorder(color, 1))
                .setPadding(10)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(value).setFontSize(22).setFontColor(color).setBold())
                .add(new Paragraph(label).setFontSize(9).setFontColor(new DeviceRgb(143, 163, 177)));
        return cell;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private String getActionColor(String type) {
        if (type == null) return TEXT_MUTED;
        switch (type) {
            case "CULTURE_AJOUTEE":   return ACCENT_GREEN;
            case "CULTURE_MODIFIEE":  return ACCENT_AMBER;
            case "CULTURE_SUPPRIMEE": return ACCENT_RED;
            case "RECOLTE":           return ACCENT_TEAL;
            default:                  return ACCENT_BLUE;
        }
    }

    private DeviceRgb getActionColorRgb(String type, DeviceRgb green, DeviceRgb amber,
                                        DeviceRgb red, DeviceRgb teal) {
        if (type == null) return teal;
        switch (type) {
            case "CULTURE_AJOUTEE":   return green;
            case "CULTURE_MODIFIEE":  return amber;
            case "CULTURE_SUPPRIMEE": return red;
            case "RECOLTE":           return teal;
            default:                  return teal;
        }
    }

    private String getActionEmoji(String type) {
        if (type == null) return "📋";
        switch (type) {
            case "CULTURE_AJOUTEE":   return "🌱";
            case "CULTURE_MODIFIEE":  return "✏️";
            case "CULTURE_SUPPRIMEE": return "🗑";
            case "RECOLTE":           return "🌾";
            default:                  return "📋";
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText("Erreur");
        alert.showAndWait();
    }

    private void showSuccess(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        alert.setHeaderText("Exporté !");
        alert.showAndWait();
    }
}