package utils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;


public class BarcodeGenerator {

        /**
         * Génère un code-barres CODE128 et sauvegarde l'image
         * @param code Le code à encoder (ex: "PROD-123")
         * @param width Largeur (ex: 400)
         * @param height Hauteur (ex: 150)
         * @return Chemin de l'image sauvegardée (ex: "images/barcodes/prod_123.png")
         */
        public static String generateAndSave(String code, int width, int height) {
            try {
                Map<EncodeHintType, Object> hints = new HashMap<>();
                hints.put(EncodeHintType.MARGIN, 1);  // Marge minimale

                Code128Writer writer = new Code128Writer();
                BitMatrix matrix = writer.encode(code, BarcodeFormat.CODE_128, width, height, hints);

                // Dossier images/barcodes (crée si pas existant)
                String dir = "src/main/resources/images/barcodes/";
                Files.createDirectories(Paths.get(dir));

                // Nom fichier unique
                String fileName = "barcode_" + code.replace("-", "_") + ".png";
                String filePath = dir + fileName;

                MatrixToImageWriter.writeToPath(matrix, "PNG", Paths.get(filePath));

                return "images/barcodes/" + fileName;  // Chemin relatif pour DB
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }
    }
