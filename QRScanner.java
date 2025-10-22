/*import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

public class QRGenerator {
    public static void generateQRCode(String data, String fileName) {
        int width = 300;
        int height = 300;
        String filePath = "QRCodes/" + fileName + ".png";

        try {
            // Create directory if missing
            java.io.File dir = new java.io.File("QRCodes");
            if (!dir.exists()) dir.mkdirs();

            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, width, height);
            Path path = FileSystems.getDefault().getPath(filePath);
            MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

            System.out.println("QR code saved to: " + filePath);
        } catch (WriterException | IOException e) {
            System.out.println(" Error generating QR code: " + e.getMessage());
        }
    }
}*/
/*
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.util.HashMap;
import java.util.Map;

public class QRGenerator {
    public static void generateQRCode(String data, String fileName) {
        try {
            int width = 250;
            int height = 250;
            String folderPath = "QRCodes";
            File folder = new File(folderPath);
            if (!folder.exists()) folder.mkdirs();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            BitMatrix matrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, width, height, hints);
            Path path = FileSystems.getDefault().getPath(folderPath + "/" + fileName + ".png");
            MatrixToImageWriter.writeToPath(matrix, "PNG", path);
            System.out.println("✅ QR Code generated: " + path);
        } catch (Exception e) {
            System.out.println("❌ Error generating QR Code");
            e.printStackTrace();
        }
    }
}
*/
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// ZXing imports
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

// Camera support
import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import com.github.sarxos.webcam.WebcamResolution;

/**
 * QRScanner.java
 * 
 * A complete QR Code scanner utility that supports:
 * - Scanning from file (image)
 * - Real-time scanning using webcam
 * - Parsing booking details from QR text
 */
public class QRScanner {

    private static volatile boolean scanning = false;
    private static String scannedResult = null;

    /**
     * ✅ Scans QR Code from image file
     */
    public static String scanQRCode(String filePath) {
        try {
            File qrFile = new File(filePath);
            if (!qrFile.exists()) {
                System.err.println("❌ QR Code file not found: " + filePath);
                return null;
            }

            BufferedImage bufferedImage = ImageIO.read(new FileInputStream(qrFile));
            if (bufferedImage == null) {
                System.err.println("❌ Invalid image file: " + filePath);
                return null;
            }

            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(bitmap);
            String decodedText = result.getText();

            System.out.println("✅ QR Code scanned successfully!");
            return decodedText;

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error scanning QR Code: " + e.getMessage());
            return null;
        }
    }

    /**
     * ✅ Scans QR Code from BufferedImage (for live camera feed)
     */
    public static String scanQRCodeFromImage(BufferedImage image) {
        try {
            if (image == null) {
                return null;
            }

            BufferedImageLuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();

        } catch (Exception e) {
            // Ignore continuous scan errors
            return null;
        }
    }

    /**
     * ✅ Scans QR Code using webcam
     */
    public static String scanQRCodeWithCamera(Component parent) {
        scannedResult = null;
        scanning = true;

        Webcam webcam = Webcam.getDefault();
        if (webcam == null) {
            JOptionPane.showMessageDialog(parent,
                    "❌ No webcam detected!\nPlease connect a camera or use file upload option.",
                    "Camera Not Found", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        webcam.setViewSize(WebcamResolution.VGA.getSize());

        JDialog scanDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent),
                "Scan QR Code", true);
        scanDialog.setSize(700, 600);
        scanDialog.setLocationRelativeTo(parent);
        scanDialog.setLayout(new BorderLayout(10, 10));

        WebcamPanel webcamPanel = new WebcamPanel(webcam);
        webcamPanel.setMirrored(true);

        JLabel infoLabel = new JLabel(
                "<html><center>📷 <b>Position the QR code in front of the camera</b><br><br>" +
                        "• Hold steady<br>• Ensure good lighting<br>• Keep it centered</center></html>",
                SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton uploadBtn = new JButton("📁 Upload QR Image");
        uploadBtn.setBackground(new Color(33, 150, 243));
        uploadBtn.setForeground(Color.WHITE);
        uploadBtn.setFocusPainted(false);
        uploadBtn.setPreferredSize(new Dimension(160, 40));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setBackground(new Color(244, 67, 54));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFocusPainted(false);
        cancelBtn.setPreferredSize(new Dimension(120, 40));

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(uploadBtn);
        buttonPanel.add(cancelBtn);

        scanDialog.add(infoLabel, BorderLayout.NORTH);
        scanDialog.add(webcamPanel, BorderLayout.CENTER);
        scanDialog.add(buttonPanel, BorderLayout.SOUTH);

        uploadBtn.addActionListener(e -> {
            scanning = false;
            webcam.close();
            scanDialog.dispose();

            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Select QR Image");
            chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Image Files", "png", "jpg", "jpeg"));

            int result = chooser.showOpenDialog(parent);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selected = chooser.getSelectedFile();
                scannedResult = scanQRCode(selected.getAbsolutePath());
            }
        });

        cancelBtn.addActionListener(e -> {
            scanning = false;
            webcam.close();
            scanDialog.dispose();
        });

        // Scanning Thread
        Executor executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "QR-Scanner");
                t.setDaemon(true);
                return t;
            }
        });

        executor.execute(() -> {
            while (scanning) {
                try {
                    if (webcam.isOpen()) {
                        BufferedImage img = webcam.getImage();
                        String result = scanQRCodeFromImage(img);
                        if (result != null && validateQRData(result)) {
                            scannedResult = result;
                            scanning = false;

                            SwingUtilities.invokeLater(() -> {
                                webcam.close();
                                scanDialog.dispose();
                                JOptionPane.showMessageDialog(parent,
                                        "✅ QR Code scanned successfully!",
                                        "Success", JOptionPane.INFORMATION_MESSAGE);
                            });
                            break;
                        }
                    }
                    Thread.sleep(100);
                } catch (Exception e) {
                    System.err.println("⚠️ Error in scanning thread: " + e.getMessage());
                }
            }
        });

        scanDialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                scanning = false;
                if (webcam.isOpen()) webcam.close();
            }
        });

        scanDialog.setVisible(true);
        return scannedResult;
    }

    /**
     * ✅ Validate QR text content
     */
    public static boolean validateQRData(String data) {
        return data != null &&
                data.contains("Booking ID:") &&
                data.contains("Vehicle Number:");
    }

    /**
     * ✅ Parse QR text to key-value map
     */
    public static Map<String, String> parseQRData(String qrData) {
        Map<String, String> details = new HashMap<>();

        if (qrData == null || qrData.trim().isEmpty()) return details;

        String[] lines = qrData.split("\n");
        for (String line : lines) {
            if (line.contains("Booking ID:"))
                details.put("booking_id", line.split(":", 2)[1].trim());
            else if (line.contains("Vehicle Number:"))
                details.put("vehicle_number", line.split(":", 2)[1].trim());
            else if (line.contains("Owner Name:"))
                details.put("owner_name", line.split(":", 2)[1].trim());
            else if (line.contains("Parking Slot:"))
                details.put("slot", line.split(":", 2)[1].trim());
            else if (line.contains("Vehicle Type:"))
                details.put("vehicle_type", line.split(":", 2)[1].trim());
            else if (line.contains("Duration:"))
                details.put("duration", line.split(":", 2)[1].trim());
            else if (line.contains("Total Cost:"))
                details.put("amount", line.split(":", 2)[1].trim().replace("₹", "").trim());
            else if (line.contains("Booking Time:"))
                details.put("in_time", line.split(":", 2)[1].trim());
        }

        details.put("status", "Verified");
        return details;
    }

    /**
     * ✅ For standalone testing (optional)
     */
    public static void main(String[] args) {
        String path = "test_qr.png"; // Path to test QR image
        String data = scanQRCode(path);

        if (data != null) {
            System.out.println("Decoded QR Data:\n" + data);
            Map<String, String> info = parseQRData(data);
            System.out.println("\nParsed Details:");
            for (String key : info.keySet()) {
                System.out.println(key + ": " + info.get(key));
            }
        } else {
            System.out.println("❌ Failed to scan QR code.");
        }
    }
}



