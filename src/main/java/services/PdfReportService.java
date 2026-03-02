package services;

import entity.Animal;
import entity.AnimalHealthRecord;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class PdfReportService {

    public static class VisitEntry {
        public final Animal animal;
        public final AnimalHealthRecord record; // null if skipped
        public VisitEntry(Animal animal, AnimalHealthRecord record) {
            this.animal = animal;
            this.record = record;
        }
        public boolean isSaved() { return record != null; }
    }

    private static final float PAGE_W    = PDRectangle.A4.getWidth();
    private static final float PAGE_H    = PDRectangle.A4.getHeight();
    private static final float MARGIN    = 30f;
    private static final float CONTENT_W = PAGE_W - 2 * MARGIN;


    private static final float[] COL_W = {42, 55, 75, 48, 52, 60, 55, 90, 58};
    private static final String[] COL_HEADERS = {
        "#Boucle", "Type", "Date", "Poids(kg)", "Appetit", "Condition", "Production", "Notes", "Statut"
    };

    private static final float ROW_H         = 18f;
    private static final float HEADER_ROW_H  = 20f;
    private static final float DATA_FONT_SIZE = 8f;
    private static final float HDR_FONT_SIZE  = 8.5f;

    private static final Color COLOR_BRAND    = new Color(0x2e, 0x7d, 0x32);
    private static final Color COLOR_TBL_HDR  = new Color(0x43, 0xa0, 0x47);
    private static final Color COLOR_ROW_ALT  = new Color(0xf3, 0xf8, 0xf3);
    private static final Color COLOR_SAVED    = new Color(0x2e, 0x7d, 0x32);
    private static final Color COLOR_SKIPPED  = new Color(0x9e, 0x9e, 0x9e);
    private static final Color COLOR_BORDER   = new Color(0xdd, 0xdd, 0xdd);

    public void generateVisitReport(String location, List<VisitEntry> entries, File outputFile) throws IOException {
        long savedCount   = entries.stream().filter(VisitEntry::isSaved).count();
        long skippedCount = entries.size() - savedCount;

        try (PDDocument doc = new PDDocument()) {
            PDFont bold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page,
                    PDPageContentStream.AppendMode.OVERWRITE, true);

            float y = PAGE_H - MARGIN;


            y -= 14;
            drawText(cs, bold, 18, MARGIN, y, "AgriSense 360", COLOR_BRAND);
            y -= 22;
            drawText(cs, bold, 13, MARGIN, y, "Rapport de Visite par Emplacement", Color.BLACK);
            y -= 16;
            drawText(cs, regular, 9, MARGIN, y,
                    "Emplacement : " + capitalize(location)
                    + "    |    Date : " + LocalDate.now(), Color.DARK_GRAY);
            y -= 14;
            drawText(cs, regular, 9, MARGIN, y,
                    "Total : " + entries.size() + " animaux"
                    + "    |    Sauvegardes : " + savedCount
                    + "    |    Passes : " + skippedCount, Color.DARK_GRAY);
            y -= 10;


            cs.setStrokingColor(COLOR_BRAND);
            cs.setLineWidth(1.5f);
            cs.moveTo(MARGIN, y);
            cs.lineTo(PAGE_W - MARGIN, y);
            cs.stroke();
            y -= 6;


            drawTableHeaderRow(cs, bold, y);
            y -= HEADER_ROW_H;

            for (int i = 0; i < entries.size(); i++) {
                if (y < MARGIN + ROW_H) {
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page,
                            PDPageContentStream.AppendMode.OVERWRITE, true);
                    y = PAGE_H - MARGIN;
                    drawTableHeaderRow(cs, bold, y);
                    y -= HEADER_ROW_H;
                }
                drawDataRow(cs, regular, bold, entries.get(i), i, y);
                y -= ROW_H;
            }


            y -= 10;
            cs.setStrokingColor(COLOR_BORDER);
            cs.setLineWidth(0.5f);
            cs.moveTo(MARGIN, y);
            cs.lineTo(PAGE_W - MARGIN, y);
            cs.stroke();
            y -= 12;
            drawText(cs, regular, 7.5f, MARGIN, y,
                    "Genere par AgriSense 360 — " + LocalDate.now(), Color.LIGHT_GRAY);

            cs.close();
            doc.save(outputFile);
        }
    }
    private static final float[] ANIMAL_COL_W     = {55, 75, 65, 90, 130, 120};
    private static final String[] ANIMAL_COL_HEADS = {"#Boucle", "Type", "Poids(kg)", "Sante", "Emplacement", "Vaccine"};
    private static final float[] RECORD_COL_W     = {55, 65, 75, 65, 65, 70, 65, 75};
    private static final String[] RECORD_COL_HEADS = {"#Boucle", "Type", "Date", "Poids", "Appetit", "Condition", "Production", "Notes"};
    private PDDocument farmDoc;
    private PDPage     farmPage;
    private PDPageContentStream farmCs;
    private PDFont     farmBold, farmRegular;
    private float      farmY;

    public void generateFarmReport(
            boolean includeSummary,
            boolean includeAllAnimals,
            boolean includeAtRisk,
            boolean includeRecentRecords,
            List<Animal> animals,
            List<AnimalHealthRecord> recentRecords,
            File outputFile) throws IOException {

        farmDoc = new PDDocument();
        farmBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        farmRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        newFarmPage();


        farmY -= 14;
        drawText(farmCs, farmBold, 18, MARGIN, farmY, "AgriSense 360", COLOR_BRAND);
        farmY -= 22;
        drawText(farmCs, farmBold, 13, MARGIN, farmY, "Rapport General de la Ferme", Color.BLACK);
        farmY -= 16;
        drawText(farmCs, farmRegular, 9, MARGIN, farmY, "Date : " + LocalDate.now()
                + "    |    Animaux : " + animals.size(), Color.DARK_GRAY);
        farmY -= 10;
        farmCs.setStrokingColor(COLOR_BRAND);
        farmCs.setLineWidth(1.5f);
        farmCs.moveTo(MARGIN, farmY);
        farmCs.lineTo(PAGE_W - MARGIN, farmY);
        farmCs.stroke();
        farmY -= 14;


        if (includeSummary) {
            drawFarmSectionHeader("Resume de la Ferme");
            drawFarmSummary(animals, recentRecords);
        }

        if (includeAllAnimals) {
            drawFarmSectionHeader("Liste Complete des Animaux (" + animals.size() + ")");
            drawAnimalTable(animals, false);
        }

        if (includeAtRisk) {
            List<Animal> atRisk = animals.stream().filter(a -> {
                String s = a.getHealthStatus();
                return s != null && (s.equalsIgnoreCase("sick")
                        || s.equalsIgnoreCase("injured")
                        || s.equalsIgnoreCase("critical"));
            }).collect(java.util.stream.Collectors.toList());
            drawFarmSectionHeader("Animaux a Risque (" + atRisk.size() + ")");
            if (atRisk.isEmpty()) {
                ensureFarmSpace(16);
                drawText(farmCs, farmRegular, 9, MARGIN, farmY,
                        "Aucun animal a risque detecte.", Color.DARK_GRAY);
                farmY -= 16;
            } else {
                drawAnimalTable(atRisk, true);
            }
        }

        if (includeRecentRecords) {
            drawFarmSectionHeader("Dossiers de Sante Recents (" + recentRecords.size() + ")");
            if (recentRecords.isEmpty()) {
                ensureFarmSpace(16);
                drawText(farmCs, farmRegular, 9, MARGIN, farmY,
                        "Aucun dossier disponible.", Color.DARK_GRAY);
                farmY -= 16;
            } else {
                drawRecordsTable(recentRecords, animals);
            }
        }

        ensureFarmSpace(30);
        farmY -= 10;
        farmCs.setStrokingColor(COLOR_BORDER);
        farmCs.setLineWidth(0.5f);
        farmCs.moveTo(MARGIN, farmY);
        farmCs.lineTo(PAGE_W - MARGIN, farmY);
        farmCs.stroke();
        farmY -= 12;
        drawText(farmCs, farmRegular, 7.5f, MARGIN, farmY,
                "Genere par AgriSense 360 — " + LocalDate.now(), Color.LIGHT_GRAY);

        farmCs.close();
        farmDoc.save(outputFile);
        farmDoc.close();
    }

    private void newFarmPage() throws IOException {
        if (farmCs != null) farmCs.close();
        farmPage = new PDPage(PDRectangle.A4);
        farmDoc.addPage(farmPage);
        farmCs = new PDPageContentStream(farmDoc, farmPage,
                PDPageContentStream.AppendMode.OVERWRITE, true);
        farmY = PAGE_H - MARGIN;
    }

    private void ensureFarmSpace(float needed) throws IOException {
        if (farmY - needed < MARGIN + 20) {
            newFarmPage();
        }
    }

    private void drawFarmSectionHeader(String title) throws IOException {
        ensureFarmSpace(28);
        farmY -= 6;
        farmCs.setNonStrokingColor(COLOR_BRAND);
        farmCs.addRect(MARGIN, farmY - 18, CONTENT_W, 18);
        farmCs.fill();
        drawText(farmCs, farmBold, 10, MARGIN + 5, farmY - 13, title, Color.WHITE);
        farmY -= 24;
    }

    private void drawFarmSummary(List<Animal> animals, List<AnimalHealthRecord> records) throws IOException {
        long vaccinated = animals.stream().filter(a -> Boolean.TRUE.equals(a.getVaccinated())).count();
        long healthy    = animals.stream().filter(a -> "healthy".equalsIgnoreCase(a.getHealthStatus())).count();
        long sick       = animals.stream().filter(a -> "sick".equalsIgnoreCase(a.getHealthStatus())).count();
        long injured    = animals.stream().filter(a -> "injured".equalsIgnoreCase(a.getHealthStatus())).count();
        long critical   = animals.stream().filter(a -> "critical".equalsIgnoreCase(a.getHealthStatus())).count();
        long atRisk     = sick + injured + critical;
        double vacPct   = animals.isEmpty() ? 0 : (vaccinated * 100.0 / animals.size());

        String[] lines = {
            "Total animaux       : " + animals.size(),
            "Vaccines            : " + vaccinated + " (" + String.format("%.0f", vacPct) + "%)",
            "Animaux a risque    : " + atRisk,
            "Sains               : " + healthy + "    |    Malades : " + sick
                + "    |    Blesses : " + injured + "    |    Critiques : " + critical,
            "Dossiers de sante   : " + records.size()
        };

        for (String line : lines) {
            ensureFarmSpace(16);
            drawText(farmCs, farmRegular, 9, MARGIN + 5, farmY, line, Color.BLACK);
            farmY -= 15;
        }
        farmY -= 6;
    }

    private void drawAnimalTable(List<Animal> animals, boolean highlightBad) throws IOException {

        ensureFarmSpace(ROW_H + HEADER_ROW_H);
        float x = MARGIN;
        farmCs.setNonStrokingColor(COLOR_TBL_HDR);
        farmCs.addRect(MARGIN, farmY - HEADER_ROW_H, CONTENT_W, HEADER_ROW_H);
        farmCs.fill();
        for (int i = 0; i < ANIMAL_COL_HEADS.length; i++) {
            drawText(farmCs, farmBold, HDR_FONT_SIZE, x + 3,
                    farmY - HEADER_ROW_H + 6, ANIMAL_COL_HEADS[i], Color.WHITE);
            x += ANIMAL_COL_W[i];
        }
        farmY -= HEADER_ROW_H;

        for (int rowIdx = 0; rowIdx < animals.size(); rowIdx++) {
            ensureFarmSpace(ROW_H);
            Animal a = animals.get(rowIdx);
            boolean bad = highlightBad && a.getHealthStatus() != null
                    && !a.getHealthStatus().equalsIgnoreCase("healthy");

            if (rowIdx % 2 == 1) {
                farmCs.setNonStrokingColor(COLOR_ROW_ALT);
                farmCs.addRect(MARGIN, farmY - ROW_H, CONTENT_W, ROW_H);
                farmCs.fill();
            }

            String[] vals = {
                a.getEarTag() != null ? "#" + a.getEarTag() : "-",
                capitalize(a.getType()),
                a.getWeight() != null ? String.format("%.1f", a.getWeight()) : "-",
                a.getHealthStatus() != null ? capitalize(a.getHealthStatus()) : "-",
                a.getLocation() != null ? capitalize(a.getLocation()) : "-",
                Boolean.TRUE.equals(a.getVaccinated()) ? "Oui" : "Non"
            };

            x = MARGIN;
            for (int i = 0; i < vals.length; i++) {
                Color c = Color.BLACK;
                if (i == 3 && bad) c = new Color(0xc6, 0x28, 0x28);
                drawText(farmCs, farmRegular, DATA_FONT_SIZE, x + 3, farmY - ROW_H + 5, vals[i], c);
                x += ANIMAL_COL_W[i];
            }

            farmCs.setStrokingColor(COLOR_BORDER);
            farmCs.setLineWidth(0.5f);
            farmCs.moveTo(MARGIN, farmY - ROW_H);
            farmCs.lineTo(PAGE_W - MARGIN, farmY - ROW_H);
            farmCs.stroke();

            farmY -= ROW_H;
        }
        farmY -= 8;
    }

    private void drawRecordsTable(List<AnimalHealthRecord> records, List<Animal> animals) throws IOException {
        java.util.Map<Integer, Animal> animalMap = new java.util.HashMap<>();
        for (Animal a : animals) animalMap.put(a.getId(), a);

        ensureFarmSpace(ROW_H + HEADER_ROW_H);
        float x = MARGIN;
        farmCs.setNonStrokingColor(COLOR_TBL_HDR);
        farmCs.addRect(MARGIN, farmY - HEADER_ROW_H, CONTENT_W, HEADER_ROW_H);
        farmCs.fill();
        for (int i = 0; i < RECORD_COL_HEADS.length; i++) {
            drawText(farmCs, farmBold, HDR_FONT_SIZE, x + 3,
                    farmY - HEADER_ROW_H + 6, RECORD_COL_HEADS[i], Color.WHITE);
            x += RECORD_COL_W[i];
        }
        farmY -= HEADER_ROW_H;

        for (int rowIdx = 0; rowIdx < records.size(); rowIdx++) {
            ensureFarmSpace(ROW_H);
            AnimalHealthRecord r = records.get(rowIdx);
            Animal a = animalMap.get(r.getAnimalId());

            if (rowIdx % 2 == 1) {
                farmCs.setNonStrokingColor(COLOR_ROW_ALT);
                farmCs.addRect(MARGIN, farmY - ROW_H, CONTENT_W, ROW_H);
                farmCs.fill();
            }

            String[] vals = {
                a != null && a.getEarTag() != null ? "#" + a.getEarTag() : "-",
                a != null ? capitalize(a.getType()) : "-",
                r.getRecordDate() != null ? r.getRecordDate().toString() : "-",
                r.getWeight() != null ? String.format("%.1f", r.getWeight()) : "-",
                r.getAppetite() != null ? capitalize(r.getAppetite().name()) : "-",
                r.getConditionStatus() != null ? capitalize(r.getConditionStatus().name()) : "-",
                formatProduction(r),
                r.getNotes() != null ? truncate(r.getNotes(), 18) : "-"
            };

            x = MARGIN;
            for (int i = 0; i < vals.length; i++) {
                drawText(farmCs, farmRegular, DATA_FONT_SIZE, x + 3, farmY - ROW_H + 5, vals[i], Color.BLACK);
                x += RECORD_COL_W[i];
            }

            farmCs.setStrokingColor(COLOR_BORDER);
            farmCs.setLineWidth(0.5f);
            farmCs.moveTo(MARGIN, farmY - ROW_H);
            farmCs.lineTo(PAGE_W - MARGIN, farmY - ROW_H);
            farmCs.stroke();

            farmY -= ROW_H;
        }
        farmY -= 8;
    }


    private void drawTableHeaderRow(PDPageContentStream cs, PDFont bold, float y) throws IOException {
        float x = MARGIN;
        cs.setNonStrokingColor(COLOR_TBL_HDR);
        cs.addRect(x, y - HEADER_ROW_H, CONTENT_W, HEADER_ROW_H);
        cs.fill();
        for (int i = 0; i < COL_HEADERS.length; i++) {
            drawText(cs, bold, HDR_FONT_SIZE, x + 3, y - HEADER_ROW_H + 6, COL_HEADERS[i], Color.WHITE);
            x += COL_W[i];
        }
    }

    private void drawDataRow(PDPageContentStream cs, PDFont regular, PDFont bold,
                              VisitEntry entry, int rowIndex, float y) throws IOException {
        Animal a = entry.animal;
        AnimalHealthRecord r = entry.record;
        boolean saved = entry.isSaved();

        if (rowIndex % 2 == 1) {
            cs.setNonStrokingColor(COLOR_ROW_ALT);
            cs.addRect(MARGIN, y - ROW_H, CONTENT_W, ROW_H);
            cs.fill();
        }

        String[] values = {
            a.getEarTag() != null ? "#" + a.getEarTag() : "-",
            capitalize(a.getType()),
            saved && r.getRecordDate() != null ? r.getRecordDate().toString() : "-",
            saved && r.getWeight() != null ? String.format("%.1f", r.getWeight()) : "-",
            saved && r.getAppetite() != null ? capitalize(r.getAppetite().name()) : "-",
            saved && r.getConditionStatus() != null ? capitalize(r.getConditionStatus().name()) : "-",
            saved ? formatProduction(r) : "-",
            saved && r.getNotes() != null ? truncate(r.getNotes(), 22) : "-",
            saved ? "Sauvegarde" : "Passe"
        };

        float x = MARGIN;
        for (int i = 0; i < values.length; i++) {
            Color textColor = Color.BLACK;
            PDFont font = regular;
            if (i == 8) {
                textColor = saved ? COLOR_SAVED : COLOR_SKIPPED;
                font = bold;
            }
            drawText(cs, font, DATA_FONT_SIZE, x + 3, y - ROW_H + 5, values[i], textColor);
            x += COL_W[i];
        }

        cs.setStrokingColor(COLOR_BORDER);
        cs.setLineWidth(0.5f);
        cs.moveTo(MARGIN, y - ROW_H);
        cs.lineTo(PAGE_W - MARGIN, y - ROW_H);
        cs.stroke();
    }

    private void drawText(PDPageContentStream cs, PDFont font, float size,
                           float x, float y, String text, Color color) throws IOException {
        if (text == null || text.isEmpty()) return;
        cs.setNonStrokingColor(color);
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
    }

    private String formatProduction(AnimalHealthRecord r) {
        if (r.getMilkYield() != null) return String.format("%.1fL", r.getMilkYield());
        if (r.getEggCount() != null) return r.getEggCount() + " oeufs";
        if (r.getWoolLength() != null) return String.format("%.1fcm", r.getWoolLength());
        return "-";
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "-";
        return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "...";
    }

    private String sanitize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c < 256) sb.append(c);
            else sb.append('?');
        }
        return sb.toString();
    }
}
