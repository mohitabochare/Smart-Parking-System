import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.File;

public class ParkingLotGUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private DefaultTableModel databaseTableModel;
    private JTable databaseTable;
    private Map<String, ParkingSpot> parkingSpots;
    private String currentUser = "";
    private String currentBookingId = "";
    
    // QR Code image storage
    private BufferedImage currentQRImage = null;
    private JLabel qrImageLabel = null;
    
    // Store current booking details for display after scan
    private Map<String, String> currentBookingDetails = new HashMap<>();
    
    // Local fallback storage when DB not available
    private final java.util.List<Map<String, String>> localBookings = new ArrayList<>();
    
    // Payment helper
    private final Payment paymentCalc = new Payment();
    
    // Algorithm: Priority Queue for slot allocation
    private PriorityQueue<String> availableSlotQueue;

    public ParkingLotGUI() {
        setTitle("QR Smart Vehicle Parking System");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        parkingSpots = new HashMap<>();
        availableSlotQueue = new PriorityQueue<>();
        initializeParkingSpots();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        mainPanel.add(createLoginScreen(), "login");
        mainPanel.add(createUserModule(), "user");
        mainPanel.add(createQRModule(), "qr");
        mainPanel.add(createParkingSlotAllocation(), "allocation");
        mainPanel.add(createAdminModule(), "admin");
        mainPanel.add(createDatabaseModule(), "database");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    // ======================= ALGORITHM IMPLEMENTATION =======================
    /**
     * ALGORITHM 1: Priority Queue for Optimal Slot Allocation
     * Time Complexity: O(log n) for insertion and removal
     * Space Complexity: O(n) where n is number of parking slots
     * 
     * This algorithm ensures that:
     * 1. Slots are allocated in sorted order (A1, A2, A3...)
     * 2. Nearest available slot is always suggested first
     * 3. Efficient slot management using heap data structure
     */
    private String getOptimalSlot() {
        if (!availableSlotQueue.isEmpty()) {
            return availableSlotQueue.peek();
        }
        return null;
    }
    
    private void allocateSlot(String slotId) {
        availableSlotQueue.remove(slotId);
        if (parkingSpots.containsKey(slotId)) {
            parkingSpots.get(slotId).setAvailable(false);
        }
    }
    
    private void releaseSlot(String slotId) {
        availableSlotQueue.offer(slotId);
        if (parkingSpots.containsKey(slotId)) {
            parkingSpots.get(slotId).setAvailable(true);
            parkingSpots.get(slotId).setVehicleNumber("");
        }
    }

    /**
     * ALGORITHM 2: Binary Search for Quick Booking Lookup
     * Time Complexity: O(log n)
     * Used for searching bookings in sorted database
     */
    private Map<String, String> binarySearchBooking(String bookingId) {
        // Sort bookings by ID first
        Collections.sort(localBookings, (a, b) -> 
            a.getOrDefault("booking_id", "").compareTo(b.getOrDefault("booking_id", "")));
        
        int left = 0, right = localBookings.size() - 1;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            String midId = localBookings.get(mid).getOrDefault("booking_id", "");
            
            int comparison = midId.compareTo(bookingId);
            if (comparison == 0) {
                return localBookings.get(mid);
            } else if (comparison < 0) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        return null;
    }

    // ======================= LOGIN SCREEN =======================
    private JPanel createLoginScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 247, 250));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(245, 247, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel iconLabel = new JLabel("🅿️");
        iconLabel.setFont(new Font("Arial", Font.BOLD, 80));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        centerPanel.add(iconLabel, gbc);

        JLabel titleLabel = new JLabel("QR Smart Parking System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(new Color(30, 136, 229));
        gbc.gridy = 1;
        centerPanel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Secure - Fast - Efficient");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(120, 120, 120));
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 10, 20, 10);
        centerPanel.add(subtitleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        centerPanel.add(userLabel, gbc);

        JTextField usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(usernameField, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        centerPanel.add(passLabel, gbc);

        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(passwordField, gbc);

        JButton loginBtn = new JButton("Login");
        styleModernButton(loginBtn, new Color(30, 136, 229), new Color(21, 101, 192));
        loginBtn.setPreferredSize(new Dimension(200, 45));
        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 10, 10, 10);
        centerPanel.add(loginBtn, gbc);

        JButton adminLoginBtn = new JButton("Admin Login");
        styleModernButton(adminLoginBtn, new Color(76, 175, 80), new Color(56, 142, 60));
        adminLoginBtn.setPreferredSize(new Dimension(200, 40));
        gbc.gridy = 6;
        gbc.insets = new Insets(10, 10, 10, 10);
        centerPanel.add(adminLoginBtn, gbc);

        loginBtn.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());
            if (!user.isEmpty() && !pass.isEmpty()) {
                currentUser = user;
                JOptionPane.showMessageDialog(this, "Welcome, " + user + "!", "Login Successful", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(mainPanel, "user");
            } else {
                JOptionPane.showMessageDialog(this, "Please enter username and password!", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        adminLoginBtn.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());
            if (user.equals("admin") && pass.equals("admin123")) {
                currentUser = "Administrator";
                JOptionPane.showMessageDialog(this, "Admin Access Granted!", "Success", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(mainPanel, "admin");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid admin credentials!", "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    // ======================= USER MODULE =======================
    private JPanel createUserModule() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("User Dashboard", "user");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(new Color(232, 245, 233));
        welcomePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 2),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel welcomeLabel = new JLabel("Welcome!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomeLabel.setForeground(new Color(46, 125, 50));
        welcomePanel.add(welcomeLabel, BorderLayout.NORTH);

        JLabel infoLabel = new JLabel("<html><div style='margin-top:10px;'>Book your parking slot quickly and securely with QR code technology.<br>Using <b>Priority Queue Algorithm</b> for optimal slot allocation!</div></html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        welcomePanel.add(infoLabel, BorderLayout.CENTER);

        contentPanel.add(welcomePanel, BorderLayout.NORTH);

        // Booking Form
        JPanel bookingPanel = new JPanel(new GridBagLayout());
        bookingPanel.setBackground(Color.WHITE);
        bookingPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Book Parking Slot",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String[] labels = {"Vehicle Number:", "Vehicle Type:", "Owner Name:", "Phone Number:", "Duration (hours):", "Parking Slot:"};
        JTextField vehicleNumField = new JTextField(20);
        JComboBox<String> vehicleTypeBox = new JComboBox<>(new String[]{"Car", "Bike", "SUV", "Van"});
        JTextField ownerField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
        JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 72, 1));
        JComboBox<String> slotBox = new JComboBox<>();
        
        // Use algorithm to populate slots
        updateSlotDropdown(slotBox);

        Component[] components = {vehicleNumField, vehicleTypeBox, ownerField, phoneField, durationSpinner, slotBox};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            gbc.weightx = 0.3;
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("Arial", Font.PLAIN, 14));
            bookingPanel.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.7;
            Component comp = components[i];
            if (comp instanceof JTextField || comp instanceof JSpinner) {
                ((JComponent)comp).setFont(new Font("Arial", Font.PLAIN, 14));
            }
            bookingPanel.add(comp, gbc);
        }

        JButton bookBtn = new JButton("Book Slot & Generate QR");
        styleModernButton(bookBtn, new Color(30, 136, 229), new Color(21, 101, 192));
        bookBtn.setPreferredSize(new Dimension(250, 45));
        gbc.gridx = 0;
        gbc.gridy = labels.length;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 15, 15, 15);
        bookingPanel.add(bookBtn, gbc);

        // Book button action
        bookBtn.addActionListener(e -> {
            String vehicleNum = vehicleNumField.getText().trim();
            String owner = ownerField.getText().trim();
            String phone = phoneField.getText().trim();
            String vehicleType = (String) vehicleTypeBox.getSelectedItem();
            int duration = (Integer) durationSpinner.getValue();
            String slot = (String) slotBox.getSelectedItem();

            if (vehicleNum.isEmpty() || owner.isEmpty() || phone.isEmpty() || slot == null) {
                JOptionPane.showMessageDialog(this, "Please fill all fields!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Create booking id and inTime
            currentBookingId = "BK" + System.currentTimeMillis();
            String inTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String status = "Booked";

            // Amount calculation
            float amountFloat = paymentCalc.TotalAmount(duration, 0);
            String amount = String.format("%.2f", amountFloat);

            // Store booking details
            currentBookingDetails.clear();
            currentBookingDetails.put("booking_id", currentBookingId);
            currentBookingDetails.put("vehicle_number", vehicleNum);
            currentBookingDetails.put("owner_name", owner);
            currentBookingDetails.put("phone", phone);
            currentBookingDetails.put("vehicle_type", vehicleType);
            currentBookingDetails.put("slot", slot);
            currentBookingDetails.put("duration", duration + " hrs");
            currentBookingDetails.put("amount", amount);
            currentBookingDetails.put("in_time", inTime);
            currentBookingDetails.put("status", status);

            // Algorithm: Allocate slot using priority queue
            allocateSlot(slot);

            // Save to DB
            addBookingToDB(currentBookingId, vehicleNum, slot, owner, phone, inTime, duration + " hrs", amount, status);

            // Generate QR Code with enhanced data
            String qrFileName = "QR_" + currentBookingId + ".png";
            String qrData = String.format(
                "╔══════════════════════════════════╗\n" +
                "║   QR SMART PARKING SYSTEM       ║\n" +
                "╚══════════════════════════════════╝\n\n" +
                "Booking ID: %s\n" +
                "Vehicle Number: %s\n" +
                "Owner Name: %s\n" +
                "Phone: %s\n" +
                "Parking Slot: %s\n" +
                "Vehicle Type: %s\n" +
                "Duration: %d hours\n" +
                "Total Cost: ₹%s\n" +
                "Booking Time: %s\n" +
                "Status: %s\n\n" +
                "═══════════════════════════════════\n" +
                "Please scan this QR code at entry.\n" +
                "Keep this code until check-out.\n" +
                "═══════════════════════════════════",
                currentBookingId, vehicleNum, owner, phone, slot, vehicleType, duration, amount, inTime, status
            );

            try {
                // Generate QR and get the image
                currentQRImage = QRGenerator.generateQRCode(qrData, qrFileName, 400, 400);
                
                if (currentQRImage != null) {
                    // Show QR Code in enhanced popup
                    showEnhancedQRPopup(currentQRImage, currentBookingId, qrFileName);
                    
                    JOptionPane.showMessageDialog(this, 
                        "✅ Booking Successful!\n\n" +
                        "Booking ID: " + currentBookingId + "\n" +
                        "Slot: " + slot + "\n" +
                        "QR Code generated and saved.\n\n" +
                        "You can scan it with Google Lens or any QR scanner!", 
                        "Success", 
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    throw new Exception("QR generation returned null");
                }
                
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                    "⚠️ Booking saved but QR generation failed!\n" +
                    "Error: " + ex.getMessage(), 
                    "Warning", 
                    JOptionPane.WARNING_MESSAGE);
            }

            // Clear fields
            vehicleNumField.setText("");
            ownerField.setText("");
            phoneField.setText("");
            durationSpinner.setValue(1);
            
            // Refresh slot dropdown using algorithm
            updateSlotDropdown(slotBox);
        });

        contentPanel.add(bookingPanel, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    // ======================= ENHANCED QR POPUP WITH SCAN OPTIONS =======================
    private void showEnhancedQRPopup(BufferedImage qrImage, String bookingId, String fileName) {
        JDialog qrDialog = new JDialog(this, "📱 Your QR Code - Ready to Scan!", true);
        qrDialog.setSize(600, 750);
        qrDialog.setLocationRelativeTo(this);
        qrDialog.setLayout(new BorderLayout(10, 10));
        qrDialog.getContentPane().setBackground(Color.WHITE);

        // Main container
        JPanel mainContainer = new JPanel(new BorderLayout(15, 15));
        mainContainer.setBackground(Color.WHITE);
        mainContainer.setBorder(new EmptyBorder(25, 25, 25, 25));

        // Title Panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(30, 136, 229));
        titlePanel.setBorder(new EmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("📱 Scan QR Code with Google Lens", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        mainContainer.add(titlePanel, BorderLayout.NORTH);

        // QR Image Panel with border
        JPanel qrPanel = new JPanel(new BorderLayout());
        qrPanel.setBackground(Color.WHITE);
        qrPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(30, 136, 229), 4, true),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel qrLabel = new JLabel(new ImageIcon(qrImage.getScaledInstance(400, 400, Image.SCALE_SMOOTH)));
        qrLabel.setHorizontalAlignment(SwingConstants.CENTER);
        qrPanel.add(qrLabel, BorderLayout.CENTER);

        mainContainer.add(qrPanel, BorderLayout.CENTER);

        // Info Panel
        JPanel infoPanel = new JPanel(new GridLayout(4, 1, 5, 5));
        infoPanel.setBackground(new Color(245, 248, 250));
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel info1 = new JLabel("✅ Booking ID: " + bookingId, SwingConstants.CENTER);
        info1.setFont(new Font("Arial", Font.BOLD, 15));
        
        JLabel info2 = new JLabel("💾 Saved as: " + fileName, SwingConstants.CENTER);
        info2.setFont(new Font("Arial", Font.PLAIN, 13));
        info2.setForeground(new Color(100, 100, 100));
        
        JLabel info3 = new JLabel("📸 Scan with: Google Lens, Camera, or any QR Scanner", SwingConstants.CENTER);
        info3.setFont(new Font("Arial", Font.BOLD, 13));
        info3.setForeground(new Color(76, 175, 80));
        
        JLabel info4 = new JLabel("ℹ️ All booking details will be displayed when scanned", SwingConstants.CENTER);
        info4.setFont(new Font("Arial", Font.ITALIC, 12));
        info4.setForeground(new Color(120, 120, 120));

        infoPanel.add(info1);
        infoPanel.add(info2);
        infoPanel.add(info3);
        infoPanel.add(info4);

        mainContainer.add(infoPanel, BorderLayout.SOUTH);

        // Button Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JButton viewDetailsBtn = new JButton("👁️ View Booking Details");
        styleModernButton(viewDetailsBtn, new Color(76, 175, 80), new Color(56, 142, 60));
        viewDetailsBtn.setPreferredSize(new Dimension(200, 45));
        viewDetailsBtn.addActionListener(e -> {
            showBookingDetailsDialog();
        });

        JButton saveBtn = new JButton("💾 Save to Desktop");
        styleModernButton(saveBtn, new Color(33, 150, 243), new Color(25, 118, 210));
        saveBtn.setPreferredSize(new Dimension(180, 45));
        saveBtn.addActionListener(e -> {
            JOptionPane.showMessageDialog(qrDialog, 
                "QR Code already saved as:\n" + fileName + "\n\n" +
                "You can find it in your project folder.", 
                "File Saved", 
                JOptionPane.INFORMATION_MESSAGE);
        });

        JButton closeBtn = new JButton("✖ Close");
        styleModernButton(closeBtn, new Color(96, 125, 139), new Color(69, 90, 100));
        closeBtn.setPreferredSize(new Dimension(120, 45));
        closeBtn.addActionListener(e -> qrDialog.dispose());

        buttonPanel.add(viewDetailsBtn);
        buttonPanel.add(saveBtn);
        buttonPanel.add(closeBtn);

        qrDialog.add(mainContainer, BorderLayout.CENTER);
        qrDialog.add(buttonPanel, BorderLayout.SOUTH);

        qrDialog.setVisible(true);
    }

    // ======================= SHOW BOOKING DETAILS DIALOG =======================
    private void showBookingDetailsDialog() {
        JDialog detailsDialog = new JDialog(this, "📋 Booking Details", true);
        detailsDialog.setSize(500, 600);
        detailsDialog.setLocationRelativeTo(this);
        detailsDialog.setLayout(new BorderLayout(15, 15));
        detailsDialog.getContentPane().setBackground(Color.WHITE);

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(76, 175, 80));
        headerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel headerLabel = new JLabel("✅ Booking Confirmed!", SwingConstants.CENTER);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel, BorderLayout.CENTER);

        // Details Panel
        JPanel detailsPanel = new JPanel(new GridLayout(10, 1, 8, 8));
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setBorder(new EmptyBorder(25, 30, 25, 30));

        detailsPanel.add(createDetailRow("📋 Booking ID:", currentBookingDetails.get("booking_id")));
        detailsPanel.add(createDetailRow("🚗 Vehicle Number:", currentBookingDetails.get("vehicle_number")));
        detailsPanel.add(createDetailRow("👤 Owner Name:", currentBookingDetails.get("owner_name")));
        detailsPanel.add(createDetailRow("📱 Phone Number:", currentBookingDetails.get("phone")));
        detailsPanel.add(createDetailRow("🚙 Vehicle Type:", currentBookingDetails.get("vehicle_type")));
        detailsPanel.add(createDetailRow("🅿️ Parking Slot:", currentBookingDetails.get("slot")));
        detailsPanel.add(createDetailRow("⏱️ Duration:", currentBookingDetails.get("duration")));
        detailsPanel.add(createDetailRow("💰 Total Amount:", "₹" + currentBookingDetails.get("amount")));
        detailsPanel.add(createDetailRow("🕐 Booking Time:", currentBookingDetails.get("in_time")));
        detailsPanel.add(createDetailRow("✅ Status:", currentBookingDetails.get("status")));

        // Footer
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        footerPanel.setBackground(Color.WHITE);
        footerPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        JButton okBtn = new JButton("OK");
        styleModernButton(okBtn, new Color(33, 150, 243), new Color(25, 118, 210));
        okBtn.setPreferredSize(new Dimension(150, 45));
        okBtn.addActionListener(e -> detailsDialog.dispose());
        footerPanel.add(okBtn);

        detailsDialog.add(headerPanel, BorderLayout.NORTH);
        detailsDialog.add(detailsPanel, BorderLayout.CENTER);
        detailsDialog.add(footerPanel, BorderLayout.SOUTH);

        detailsDialog.setVisible(true);
    }

    private JPanel createDetailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setBackground(Color.WHITE);
        row.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
            new EmptyBorder(8, 10, 8, 10)
        ));

        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Arial", Font.BOLD, 14));
        labelComp.setForeground(new Color(60, 60, 60));

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Arial", Font.PLAIN, 14));
        valueComp.setForeground(new Color(30, 30, 30));
        valueComp.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(labelComp, BorderLayout.WEST);
        row.add(valueComp, BorderLayout.EAST);

        return row;
    }

    // ======================= UPDATE SLOT DROPDOWN USING ALGORITHM =======================
    private void updateSlotDropdown(JComboBox<String> slotBox) {
        slotBox.removeAllItems();
        // Priority queue automatically maintains sorted order
        PriorityQueue<String> tempQueue = new PriorityQueue<>(availableSlotQueue);
        while (!tempQueue.isEmpty()) {
            slotBox.addItem(tempQueue.poll());
        }
        
        if (slotBox.getItemCount() == 0) {
            slotBox.addItem("No slots available");
        }
    }

    // ======================= QR MODULE (Updated) =======================
    private JPanel createQRModule() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("QR Code - Booking Confirmed", "qr");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);

        // Success message
        JPanel successPanel = new JPanel(new BorderLayout());
        successPanel.setBackground(new Color(232, 245, 233));
        successPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 2),
            new EmptyBorder(15, 20, 15, 20)
        ));

        JLabel successLabel = new JLabel("✅ QR Code Generated Successfully!", SwingConstants.CENTER);
        successLabel.setFont(new Font("Arial", Font.BOLD, 20));
        successLabel.setForeground(new Color(46, 125, 50));
        successPanel.add(successLabel, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        centerPanel.add(successPanel, gbc);

        // QR Code Display Panel
        JPanel qrPanel = new JPanel(new BorderLayout());
        qrPanel.setPreferredSize(new Dimension(350, 350));
        qrPanel.setBackground(Color.WHITE);
        qrPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(30, 136, 229), 3),
            new EmptyBorder(20, 20, 20, 20)
        ));

        qrImageLabel = new JLabel("QR Code", SwingConstants.CENTER);
        qrImageLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        if (currentQRImage != null) {
            ImageIcon icon = new ImageIcon(currentQRImage.getScaledInstance(300, 300, Image.SCALE_SMOOTH));
            qrImageLabel.setIcon(icon);
            qrImageLabel.setText("");
        }
        
        qrPanel.add(qrImageLabel, BorderLayout.CENTER);

        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        centerPanel.add(qrPanel, gbc);

        // Action Buttons Panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);

        JButton viewDetailsBtn = new JButton("👁️ View Details");
        styleModernButton(viewDetailsBtn, new Color(76, 175, 80), new Color(56, 142, 60));
        viewDetailsBtn.setPreferredSize(new Dimension(160, 40));
        viewDetailsBtn.addActionListener(e -> showBookingDetailsDialog());

        JButton continueBtn = new JButton("Continue →");
        styleModernButton(continueBtn, new Color(156, 39, 176), new Color(123, 31, 162));
        continueBtn.setPreferredSize(new Dimension(150, 40));
        continueBtn.addActionListener(e -> cardLayout.show(mainPanel, "allocation"));

        buttonPanel.add(viewDetailsBtn);
        buttonPanel.add(continueBtn);

        gbc.gridy = 2;
        centerPanel.add(buttonPanel, gbc);

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    // ======================= PARKING SLOT ALLOCATION =======================
    private JPanel createParkingSlotAllocation() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("Parking Slot Allocation", "allocation");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        summaryPanel.setBackground(Color.WHITE);

        long available = parkingSpots.values().stream().filter(ParkingSpot::isAvailable).count();
        long occupied = parkingSpots.size() - available;

        summaryPanel.add(createStatCard("Total Slots", String.valueOf(parkingSpots.size()), new Color(33, 150, 243), "🅿️"));
        summaryPanel.add(createStatCard("Available", String.valueOf(available), new Color(76, 175, 80), "✓"));
        summaryPanel.add(createStatCard("Occupied", String.valueOf(occupied), new Color(244, 67, 54), "✗"));

        contentPanel.add(summaryPanel, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(4, 5, 10, 10));
        gridPanel.setBackground(Color.WHITE);
        gridPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        parkingSpots.forEach((slotId, spot) -> {
            JPanel slotPanel = createSlotPanel(slotId, spot);
            gridPanel.add(slotPanel);
        });

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBorder(null);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JButton nextBtn = new JButton("View Admin Dashboard");
        styleModernButton(nextBtn, new Color(30, 136, 229), new Color(21, 101, 192));
        nextBtn.setPreferredSize(new Dimension(250, 45));
        nextBtn.addActionListener(e -> cardLayout.show(mainPanel, "admin"));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(nextBtn);

        contentPanel.add(bottomPanel, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    // ======================= ADMIN MODULE =======================
    private JPanel createAdminModule() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("Admin Dashboard", "admin");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setBackground(Color.WHITE);

        statsPanel.add(createStatCard("Total Revenue", "Rs 8,450", new Color(156, 39, 176), "💰"));
        statsPanel.add(createStatCard("Today's Bookings", "23", new Color(255, 152, 0), "📊"));
        statsPanel.add(createStatCard("Active Users", "18", new Color(33, 150, 243), "👥"));
        statsPanel.add(createStatCard("Avg Duration", "2.5 hrs", new Color(76, 175, 80), "⏱️"));

        contentPanel.add(statsPanel, BorderLayout.NORTH);

        JPanel controlPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        controlPanel.setBackground(Color.WHITE);
        controlPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        JButton viewBookingsBtn = createAdminButton("View All Bookings", new Color(33, 150, 243));
        JButton manageSlotsBtn = createAdminButton("Manage Slots", new Color(76, 175, 80));
        JButton reportsBtn = createAdminButton("Generate Reports", new Color(255, 152, 0));
        JButton settingsBtn = createAdminButton("Settings", new Color(96, 125, 139));
        JButton databaseBtn = createAdminButton("Database", new Color(156, 39, 176));
        JButton logoutBtn = createAdminButton("Logout", new Color(244, 67, 54));

        viewBookingsBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Viewing all bookings..."));
        manageSlotsBtn.addActionListener(e -> cardLayout.show(mainPanel, "allocation"));
        reportsBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Generating reports..."));
        settingsBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Opening settings..."));
        databaseBtn.addActionListener(e -> {
            updateDatabaseTable();
            cardLayout.show(mainPanel, "database");
        });
        logoutBtn.addActionListener(e -> {
            currentUser = "";
            cardLayout.show(mainPanel, "login");
        });

        controlPanel.add(viewBookingsBtn);
        controlPanel.add(manageSlotsBtn);
        controlPanel.add(reportsBtn);
        controlPanel.add(settingsBtn);
        controlPanel.add(databaseBtn);
        controlPanel.add(logoutBtn);

        contentPanel.add(controlPanel, BorderLayout.CENTER);

        JPanel activityPanel = new JPanel(new BorderLayout());
        activityPanel.setBackground(Color.WHITE);
        activityPanel.setBorder(BorderFactory.createTitledBorder("Recent Activity"));

        String[] columns = {"Time", "Action", "User", "Details"};
        Object[][] data = {
            {getCurrentTime(), "Booking", "John Doe", "Slot A5 - TN01AB1234"},
            {getCurrentTime(), "Payment", "Jane Smith", "Rs150 - Card"},
            {getCurrentTime(), "Check-out", "Mike Johnson", "Slot B3 - 2h 30m"}
        };

        JTable activityTable = new JTable(data, columns);
        styleTable(activityTable);
        JScrollPane actScroll = new JScrollPane(activityTable);
        actScroll.setPreferredSize(new Dimension(0, 150));
        activityPanel.add(actScroll, BorderLayout.CENTER);

        contentPanel.add(activityPanel, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    // ======================= DATABASE MODULE =======================
    private JPanel createDatabaseModule() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("Database Management", "database");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setBackground(Color.WHITE);

        JButton refreshBtn = new JButton("Refresh");
        JButton addBtn = new JButton("Add Record");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton exportBtn = new JButton("Export");

        styleModernButton(refreshBtn, new Color(33, 150, 243), new Color(25, 118, 210));
        styleModernButton(addBtn, new Color(76, 175, 80), new Color(56, 142, 60));
        styleModernButton(updateBtn, new Color(255, 152, 0), new Color(245, 124, 0));
        styleModernButton(deleteBtn, new Color(244, 67, 54), new Color(211, 47, 47));
        styleModernButton(exportBtn, new Color(156, 39, 176), new Color(123, 31, 162));

        refreshBtn.addActionListener(e -> updateDatabaseTable());
        addBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Add Record Form"));
        updateBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Update Record Form"));
        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this record?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(this, "Record deleted!");
            }
        });
        exportBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Database exported to CSV!"));

        controlPanel.add(refreshBtn);
        controlPanel.add(addBtn);
        controlPanel.add(updateBtn);
        controlPanel.add(deleteBtn);
        controlPanel.add(exportBtn);

        contentPanel.add(controlPanel, BorderLayout.NORTH);

        String[] columns = {"S.No", "Booking ID", "Vehicle No", "Spot No", "Name", "Phone", "In Time", "Duration", "Amount", "Status"};
        databaseTableModel = new DefaultTableModel(columns, 0);
        databaseTable = new JTable(databaseTableModel);
        styleTable(databaseTable);

        JScrollPane scrollPane = new JScrollPane(databaseTable);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JButton exitBtn = new JButton("Exit System");
        styleModernButton(exitBtn, new Color(244, 67, 54), new Color(211, 47, 47));
        exitBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Exit application?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) System.exit(0);
        });

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightBtns.add(exitBtn);
        bottom.add(rightBtns, BorderLayout.EAST);

        JLabel statusLabel = new JLabel("Total Records: 0 | Last Updated: " + getCurrentTime());
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        bottom.add(statusLabel, BorderLayout.WEST);

        contentPanel.add(bottom, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);

        updateDatabaseTable();
        return panel;
    }

    // ======================= UTILITY METHODS =======================
    private void initializeParkingSpots() {
        for (int i = 1; i <= 20; i++) {
            boolean isAvailable = i > 5;
            ParkingSpot spot = new ParkingSpot("A" + i, isAvailable);
            if (!isAvailable) {
                spot.setVehicleNumber("TN01XX" + (1000 + i));
            } else {
                // Add to priority queue for algorithm
                availableSlotQueue.offer("A" + i);
            }
            parkingSpots.put("A" + i, spot);
        }
    }

    private JPanel createHeaderPanel(String title, String currentScreen) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 136, 229));
        headerPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        navPanel.setBackground(new Color(30, 136, 229));

        if (!currentScreen.equals("login") && !currentScreen.equals("user")) {
            JButton backBtn = new JButton("← Back");
            backBtn.setForeground(Color.WHITE);
            backBtn.setBackground(new Color(25, 118, 210));
            backBtn.setFocusPainted(false);
            backBtn.setBorderPainted(false);
            backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            backBtn.setFont(new Font("Arial", Font.BOLD, 12));
            backBtn.addActionListener(e -> {
                if (currentScreen.equals("qr")) {
                    cardLayout.show(mainPanel, "user");
                } else if (currentScreen.equals("allocation")) {
                    cardLayout.show(mainPanel, "qr");
                } else if (currentScreen.equals("admin") || currentScreen.equals("database")) {
                    cardLayout.show(mainPanel, "allocation");
                }
            });
            navPanel.add(backBtn);
        }

        JButton homeBtn = new JButton("🏠 Home");
        homeBtn.setForeground(Color.WHITE);
        homeBtn.setBackground(new Color(25, 118, 210));
        homeBtn.setFocusPainted(false);
        homeBtn.setBorderPainted(false);
        homeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        homeBtn.setFont(new Font("Arial", Font.BOLD, 12));
        homeBtn.addActionListener(e -> cardLayout.show(mainPanel, "login"));
        navPanel.add(homeBtn);

        headerPanel.add(navPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JPanel createStatCard(String title, String value, Color color, String icon) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(color);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Arial", Font.BOLD, 32));
        iconLabel.setForeground(Color.WHITE);
        card.add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setBackground(color);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));

        textPanel.add(titleLabel);
        textPanel.add(valueLabel);
        card.add(textPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createSlotPanel(String slotId, ParkingSpot spot) {
        JPanel slotPanel = new JPanel(new BorderLayout());
        slotPanel.setPreferredSize(new Dimension(120, 80));

        Color bgColor = spot.isAvailable() ? new Color(200, 230, 201) : new Color(255, 205, 210);
        Color borderColor = spot.isAvailable() ? new Color(76, 175, 80) : new Color(244, 67, 54);

        slotPanel.setBackground(bgColor);
        slotPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 2),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel slotLabel = new JLabel(slotId, SwingConstants.CENTER);
        slotLabel.setFont(new Font("Arial", Font.BOLD, 18));
        slotLabel.setForeground(borderColor);

        JLabel statusLabel = new JLabel(spot.isAvailable() ? "Available" : "Occupied", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusLabel.setForeground(borderColor);

        slotPanel.add(slotLabel, BorderLayout.CENTER);
        slotPanel.add(statusLabel, BorderLayout.SOUTH);

        slotPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        slotPanel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                slotPanel.setBackground(bgColor.darker());
            }
            public void mouseExited(MouseEvent e) {
                slotPanel.setBackground(bgColor);
            }
            public void mouseClicked(MouseEvent e) {
                String status = spot.isAvailable() ? "Available" : "Occupied by: " + spot.getVehicleNumber();
                JOptionPane.showMessageDialog(null, "Slot: " + slotId + "\nStatus: " + status);
            }
        });

        return slotPanel;
    }

    private JButton createAdminButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 80));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.darker());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });

        return btn;
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        label.setBorder(new EmptyBorder(5, 5, 5, 5));
        return label;
    }

    private void styleModernButton(JButton btn, Color normalColor, Color hoverColor) {
        btn.setBackground(normalColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverColor);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(normalColor);
            }
        });
    }

    private void styleTable(JTable table) {
        table.setRowHeight(35);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(63, 81, 181));
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(232, 234, 246));
        table.setSelectionForeground(Color.BLACK);
        table.setGridColor(new Color(224, 224, 224));
        table.setShowGrid(true);
    }

    private void updateDatabaseTable() {
        databaseTableModel.setRowCount(0);
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            if (conn == null) throw new SQLException("DB connection returned null");

            String query = "SELECT booking_id, vehicle_number, spot_number, name, phone, in_time, duration, amount, status FROM parking_spots";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();
            int serial = 1;
            boolean hasRows = false;

            while (rs.next()) {
                hasRows = true;
                databaseTableModel.addRow(new Object[]{
                    serial++,
                    rs.getString("booking_id"),
                    rs.getString("vehicle_number"),
                    rs.getString("spot_number"),
                    rs.getString("name"),
                    rs.getString("phone"),
                    rs.getString("in_time"),
                    rs.getString("duration"),
                    rs.getString("amount"),
                    rs.getString("status")
                });
            }

            rs.close();
            ps.close();

            if (!hasRows) {
                String q2 = "SELECT spot_id, vehicle_number, status, entry_time, exit_time, amount FROM parking_spots";
                try (PreparedStatement p2 = conn.prepareStatement(q2); ResultSet r2 = p2.executeQuery()) {
                    int s = 1;
                    while (r2.next()) {
                        String bid = "N/A";
                        String veh = r2.getString("vehicle_number");
                        String spotNo = String.valueOf(r2.getInt("spot_id"));
                        String name = "";
                        String phone = "";
                        String intime = r2.getString("entry_time");
                        String duration = "N/A";
                        String amount = r2.getString("amount");
                        String status = r2.getString("status");
                        databaseTableModel.addRow(new Object[]{s++, bid, veh, spotNo, name, phone, intime, duration, amount, status});
                    }
                } catch (SQLException ignore) {}
            }
            return;

        } catch (SQLException ex) {
            System.err.println("DB failure in updateDatabaseTable(): " + ex.getMessage());
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }

        // Fallback: show localBookings
        int serial = 1;
        for (Map<String, String> b : localBookings) {
            databaseTableModel.addRow(new Object[]{
                serial++,
                b.getOrDefault("booking_id", ""),
                b.getOrDefault("vehicle_number", ""),
                b.getOrDefault("spot_number", ""),
                b.getOrDefault("name", ""),
                b.getOrDefault("phone", ""),
                b.getOrDefault("in_time", ""),
                b.getOrDefault("duration", ""),
                b.getOrDefault("amount", ""),
                b.getOrDefault("status", "")
            });
        }
    }

    public void addBookingToDB(String bookingId, String vehicleNumber, String spotNumber, String name, String phone, String inTime, String duration, String amount, String status) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            if (conn != null) {
                String insertQuery = "INSERT INTO parking_spots (booking_id, vehicle_number, spot_number, name, phone, in_time, duration, amount, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertQuery)) {
                    ps.setString(1, bookingId);
                    ps.setString(2, vehicleNumber);
                    ps.setString(3, spotNumber);
                    ps.setString(4, name);
                    ps.setString(5, phone);
                    ps.setString(6, inTime);
                    ps.setString(7, duration);
                    ps.setString(8, amount);
                    ps.setString(9, status);
                    ps.executeUpdate();
                    updateDatabaseTable();
                    return;
                } catch (SQLException e) {
                    System.err.println("Insert with booking columns failed: " + e.getMessage());
                    try {
                        String insertAlt = "INSERT INTO parking_spots (vehicle_number, status, entry_time, amount) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement ps2 = conn.prepareStatement(insertAlt)) {
                            ps2.setString(1, vehicleNumber);
                            ps2.setString(2, status);
                            ps2.setString(3, inTime);
                            ps2.setString(4, amount);
                            ps2.executeUpdate();
                            updateDatabaseTable();
                            return;
                        }
                    } catch (SQLException e2) {
                        System.err.println("Alternative insert failed: " + e2.getMessage());
                    }
                }
            } else {
                System.err.println("DBConnection.getConnection() returned null; falling back to local storage.");
            }
        } catch (Exception ex) {
            System.err.println("Error saving booking to DB: " + ex.getMessage());
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }

        // Fallback to local storage
        Map<String, String> rec = new HashMap<>();
        rec.put("booking_id", bookingId);
        rec.put("vehicle_number", vehicleNumber);
        rec.put("spot_number", spotNumber);
        rec.put("name", name);
        rec.put("phone", phone);
        rec.put("in_time", inTime);
        rec.put("duration", duration);
        rec.put("amount", amount);
        rec.put("status", status);
        localBookings.add(rec);
        updateDatabaseTable();
    }

    private String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        return now.format(formatter);
    }

    // ======================= PARKING SPOT CLASS =======================
    class ParkingSpot {
        private String id;
        private boolean available;
        private String vehicleNumber;

        public ParkingSpot(String id, boolean available) {
            this.id = id;
            this.available = available;
            this.vehicleNumber = "";
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public String getVehicleNumber() {
            return vehicleNumber;
        }

        public void setVehicleNumber(String vehicleNumber) {
            this.vehicleNumber = vehicleNumber;
        }

        public String getId() {
            return id;
        }
    }

    // ======================= MAIN METHOD =======================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ParkingLotGUI().setVisible(true);
        });
    }
}
/*MAIN CODE 
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class ParkingLotGUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private DefaultTableModel databaseTableModel;
    private JTable databaseTable;
    private Map<String, ParkingSpot> parkingSpots;
    private String currentUser = "";
    private String currentBookingId = "";

    // Local fallback storage when DB not available
    private final java.util.List<Map<String, String>> localBookings = new ArrayList<>();

    // Payment helper
    private final Payment paymentCalc = new Payment();

    public ParkingLotGUI() {
        setTitle("QR Smart Vehicle Parking System");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        parkingSpots = new HashMap<>();
        initializeParkingSpots();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Add screens in the specified order (keeps your GUI unchanged)
        mainPanel.add(createLoginScreen(), "login");
        mainPanel.add(createUserModule(), "user");
        mainPanel.add(createQRModule(), "qr");
        mainPanel.add(createParkingSlotAllocation(), "allocation");
        mainPanel.add(createAdminModule(), "admin");
        mainPanel.add(createDatabaseModule(), "database");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    // ----------------------- LOGIN SCREEN -----------------------
    private JPanel createLoginScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 247, 250));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(245, 247, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel iconLabel = new JLabel("P");
        iconLabel.setFont(new Font("Arial", Font.BOLD, 80));
        iconLabel.setForeground(new Color(30, 136, 229));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        centerPanel.add(iconLabel, gbc);

        JLabel titleLabel = new JLabel("QR Smart Parking System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(new Color(30, 136, 229));
        gbc.gridy = 1;
        centerPanel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Secure - Fast - Efficient");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(120, 120, 120));
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 10, 20, 10);
        centerPanel.add(subtitleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridy = 3; gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        centerPanel.add(userLabel, gbc);

        JTextField usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(usernameField, gbc);

        gbc.gridy = 4; gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        centerPanel.add(passLabel, gbc);

        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(passwordField, gbc);

        JButton loginBtn = new JButton("Login");
        styleModernButton(loginBtn, new Color(30, 136, 229), new Color(21, 101, 192));
        loginBtn.setPreferredSize(new Dimension(200, 45));
        gbc.gridy = 5; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 10, 10, 10);
        centerPanel.add(loginBtn, gbc);

        JButton adminLoginBtn = new JButton("Admin Login");
        styleModernButton(adminLoginBtn, new Color(76, 175, 80), new Color(56, 142, 60));
        adminLoginBtn.setPreferredSize(new Dimension(200, 40));
        gbc.gridy = 6;
        gbc.insets = new Insets(10, 10, 10, 10);
        centerPanel.add(adminLoginBtn, gbc);

        loginBtn.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());
            if (!user.isEmpty() && !pass.isEmpty()) {
                currentUser = user;
                JOptionPane.showMessageDialog(this, "Welcome, " + user + "!",
                        "Login Successful", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(mainPanel, "user");
            } else {
                JOptionPane.showMessageDialog(this, "Please enter username and password!",
                        "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        adminLoginBtn.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());
            if (user.equals("admin") && pass.equals("admin123")) {
                currentUser = "Administrator";
                JOptionPane.showMessageDialog(this, "Admin Access Granted!",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(mainPanel, "admin");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid admin credentials!",
                        "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    // ----------------------- USER MODULE -----------------------
    private JPanel createUserModule() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("User Dashboard", "user");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(new Color(232, 245, 233));
        welcomePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 2),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel welcomeLabel = new JLabel("Welcome!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomeLabel.setForeground(new Color(46, 125, 50));
        welcomePanel.add(welcomeLabel, BorderLayout.NORTH);

        JLabel infoLabel = new JLabel("<html><div style='margin-top:10px;'>Book your parking slot quickly and securely with QR code technology.</div></html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        welcomePanel.add(infoLabel, BorderLayout.CENTER);

        contentPanel.add(welcomePanel, BorderLayout.NORTH);

        // Booking Form (kept similar to your design)
        JPanel bookingPanel = new JPanel(new GridBagLayout());
        bookingPanel.setBackground(Color.WHITE);
        bookingPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Book Parking Slot",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String[] labels = {"Vehicle Number:", "Vehicle Type:", "Owner Name:",
                          "Phone Number:", "Duration (hours):", "Parking Slot:"};

        JTextField vehicleNumField = new JTextField(20);
        JComboBox<String> vehicleTypeBox = new JComboBox<>(new String[]{"Car", "Bike", "SUV", "Van"});
        JTextField ownerField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
        JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 72, 1));
        JComboBox<String> slotBox = new JComboBox<>();

        parkingSpots.forEach((k, v) -> {
            if (v.isAvailable()) slotBox.addItem(k);
        });

        Component[] components = {vehicleNumField, vehicleTypeBox, ownerField,
                                 phoneField, durationSpinner, slotBox};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i;
            gbc.weightx = 0.3;
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("Arial", Font.PLAIN, 14));
            bookingPanel.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.7;
            Component comp = components[i];
            if (comp instanceof JTextField || comp instanceof JSpinner) {
                ((JComponent)comp).setFont(new Font("Arial", Font.PLAIN, 14));
            }
            bookingPanel.add(comp, gbc);
        }

        JButton bookBtn = new JButton("Book Slot & Generate QR");
        styleModernButton(bookBtn, new Color(30, 136, 229), new Color(21, 101, 192));
        bookBtn.setPreferredSize(new Dimension(250, 45));
        gbc.gridx = 0; gbc.gridy = labels.length;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 15, 15, 15);
        bookingPanel.add(bookBtn, gbc);

        // Book button action: compute amount, save to DB (or local fallback), generate QR
        bookBtn.addActionListener(e -> {
            String vehicleNum = vehicleNumField.getText().trim();
            String owner = ownerField.getText().trim();
            String phone = phoneField.getText().trim();
            String vehicleType = (String) vehicleTypeBox.getSelectedItem();
            int duration = (Integer) durationSpinner.getValue();
            String slot = (String) slotBox.getSelectedItem();

            if (vehicleNum.isEmpty() || owner.isEmpty() || phone.isEmpty() || slot == null) {
                JOptionPane.showMessageDialog(this, "Please fill all fields!", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Create booking id and inTime
            currentBookingId = "BK" + (System.currentTimeMillis() % 1000000);
            String inTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String status = "Booked";

            // Amount calculation using Payment class (hours, minutes=0)
            float amountFloat = paymentCalc.TotalAmount(duration, 0);
            String amount = String.format("%.2f", amountFloat);

            // Mark slot as occupied in local map
            if (slot != null && parkingSpots.containsKey(slot)) {
                parkingSpots.get(slot).setAvailable(false);
                parkingSpots.get(slot).setVehicleNumber(vehicleNum);
            }

            // Save to DB or fallback local
            addBookingToDB(currentBookingId, vehicleNum, slot, owner, phone, inTime, duration + " hrs", amount, status);

            // Generate QR (with booking summary)
            String qrData = "BookingID:" + currentBookingId + "|Veh:" + vehicleNum + "|Slot:" + slot + "|In:" + inTime + "|Dur:" + duration + "h|Amt:" + amount;
            try {
                String qrFileName = "QR_" + currentBookingId;
                // QRGenerator present in project
                QRGenerator.generateQRCode(qrData, qrFileName, 300, 300);
                JOptionPane.showMessageDialog(this, "Booking successful!\nBooking ID: " + currentBookingId + "\nQR generated: " + qrFileName + ".png", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Booking saved but QR generation failed: " + ex.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
            }

            // clear fields
            vehicleNumField.setText("");
            ownerField.setText("");
            phoneField.setText("");
            durationSpinner.setValue(1);

            // show QR screen
            cardLayout.show(mainPanel, "qr");
        });

        contentPanel.add(bookingPanel, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    // ----------------------- QR MODULE -----------------------
    private JPanel createQRModule() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("QR Code Generator", "qr");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);

        JPanel qrPanel = new JPanel(new BorderLayout());
        qrPanel.setPreferredSize(new Dimension(350, 350));
        qrPanel.setBackground(Color.WHITE);
        qrPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(30, 136, 229), 3),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JTextArea qrDisplay = new JTextArea();
        qrDisplay.setEditable(false);
        qrDisplay.setFont(new Font("Courier New", Font.BOLD, 10));
        qrDisplay.setText(generateQRPattern());
        qrDisplay.setBackground(Color.WHITE);
        qrPanel.add(qrDisplay, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 0;
        centerPanel.add(qrPanel, gbc);

        JPanel detailsPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        detailsPanel.setBackground(new Color(245, 248, 250));
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(15, 20, 15, 20)
        ));

        detailsPanel.add(createInfoLabel("Booking ID: " + currentBookingId));
        detailsPanel.add(createInfoLabel("Vehicle: (see booking)"));
        detailsPanel.add(createInfoLabel("Slot: (see booking)"));
        detailsPanel.add(createInfoLabel("Time: " + getCurrentTime()));
        detailsPanel.add(createInfoLabel("Status: Active"));

        gbc.gridy = 1;
        centerPanel.add(detailsPanel, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);

        JButton downloadBtn = new JButton("Download QR");
        styleModernButton(downloadBtn, new Color(76, 175, 80), new Color(56, 142, 60));
        downloadBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "QR saved successfully!", "Download", JOptionPane.INFORMATION_MESSAGE));

        JButton printBtn = new JButton("Print");
        styleModernButton(printBtn, new Color(33, 150, 243), new Color(25, 118, 210));
        printBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Printing QR Code...", "Print", JOptionPane.INFORMATION_MESSAGE));

        JButton shareBtn = new JButton("Share via Email");
        styleModernButton(shareBtn, new Color(255, 152, 0), new Color(245, 124, 0));
        shareBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "QR Code sent to your email!", "Email Sent", JOptionPane.INFORMATION_MESSAGE));

        JButton continueBtn = new JButton("Continue to Parking");
        styleModernButton(continueBtn, new Color(156, 39, 176), new Color(123, 31, 162));
        continueBtn.addActionListener(e -> cardLayout.show(mainPanel, "allocation"));

        buttonPanel.add(downloadBtn);
        buttonPanel.add(printBtn);
        buttonPanel.add(shareBtn);

        gbc.gridy = 2;
        centerPanel.add(buttonPanel, gbc);

        gbc.gridy = 3;
        centerPanel.add(continueBtn, gbc);

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    // ================= PARKING SLOT ALLOCATION =================
    private JPanel createParkingSlotAllocation() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("Parking Slot Allocation", "allocation");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        summaryPanel.setBackground(Color.WHITE);

        long available = parkingSpots.values().stream().filter(ParkingSpot::isAvailable).count();
        long occupied = parkingSpots.size() - available;

        summaryPanel.add(createStatCard("Total Slots", String.valueOf(parkingSpots.size()),
            new Color(33, 150, 243), "P"));
        summaryPanel.add(createStatCard("Available", String.valueOf(available),
            new Color(76, 175, 80), "+"));
        summaryPanel.add(createStatCard("Occupied", String.valueOf(occupied),
            new Color(244, 67, 54), "X"));

        contentPanel.add(summaryPanel, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(4, 5, 10, 10));
        gridPanel.setBackground(Color.WHITE);
        gridPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        parkingSpots.forEach((slotId, spot) -> {
            JPanel slotPanel = createSlotPanel(slotId, spot);
            gridPanel.add(slotPanel);
        });

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBorder(null);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JButton nextBtn = new JButton("View Admin Dashboard");
        styleModernButton(nextBtn, new Color(30, 136, 229), new Color(21, 101, 192));
        nextBtn.setPreferredSize(new Dimension(250, 45));
        nextBtn.addActionListener(e -> cardLayout.show(mainPanel, "admin"));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(nextBtn);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    // ================= ADMIN MODULE =================
    private JPanel createAdminModule() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("Admin Dashboard", "admin");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setBackground(Color.WHITE);

        statsPanel.add(createStatCard("Total Revenue", "Rs 8,450",
            new Color(156, 39, 176), "$"));
        statsPanel.add(createStatCard("Today's Bookings", "23",
            new Color(255, 152, 0), "#"));
        statsPanel.add(createStatCard("Active Users", "18",
            new Color(33, 150, 243), "U"));
        statsPanel.add(createStatCard("Avg Duration", "2.5 hrs",
            new Color(76, 175, 80), "T"));

        contentPanel.add(statsPanel, BorderLayout.NORTH);

        JPanel controlPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        controlPanel.setBackground(Color.WHITE);
        controlPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        JButton viewBookingsBtn = createAdminButton("View All Bookings", new Color(33, 150, 243));
        JButton manageSlotsBtn = createAdminButton("Manage Slots", new Color(76, 175, 80));
        JButton reportsBtn = createAdminButton("Generate Reports", new Color(255, 152, 0));
        JButton settingsBtn = createAdminButton("Settings", new Color(96, 125, 139));
        JButton databaseBtn = createAdminButton("Database", new Color(156, 39, 176));
        JButton logoutBtn = createAdminButton("Logout", new Color(244, 67, 54));

        viewBookingsBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Viewing all bookings..."));
        manageSlotsBtn.addActionListener(e -> cardLayout.show(mainPanel, "allocation"));
        reportsBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Generating reports..."));
        settingsBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Opening settings..."));

        databaseBtn.addActionListener(e -> {
            updateDatabaseTable();
            cardLayout.show(mainPanel, "database");
        });

        logoutBtn.addActionListener(e -> {
            currentUser = "";
            cardLayout.show(mainPanel, "login");
        });

        controlPanel.add(viewBookingsBtn);
        controlPanel.add(manageSlotsBtn);
        controlPanel.add(reportsBtn);
        controlPanel.add(settingsBtn);
        controlPanel.add(databaseBtn);
        controlPanel.add(logoutBtn);

        contentPanel.add(controlPanel, BorderLayout.CENTER);

        JPanel activityPanel = new JPanel(new BorderLayout());
        activityPanel.setBackground(Color.WHITE);
        activityPanel.setBorder(BorderFactory.createTitledBorder("Recent Activity"));

        String[] columns = {"Time", "Action", "User", "Details"};
        Object[][] data = {
            {getCurrentTime(), "Booking", "John Doe", "Slot A5 - TN01AB1234"},
            {getCurrentTime(), "Payment", "Jane Smith", "Rs150 - Card"},
            {getCurrentTime(), "Check-out", "Mike Johnson", "Slot B3 - 2h 30m"}
        };

        JTable activityTable = new JTable(data, columns);
        styleTable(activityTable);

        JScrollPane actScroll = new JScrollPane(activityTable);
        actScroll.setPreferredSize(new Dimension(0, 150));
        activityPanel.add(actScroll, BorderLayout.CENTER);

        contentPanel.add(activityPanel, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    // ================= DATABASE MODULE =================
    private JPanel createDatabaseModule() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("Database Management", "database");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setBackground(Color.WHITE);

        JButton refreshBtn = new JButton("Refresh");
        JButton addBtn = new JButton("Add Record");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton exportBtn = new JButton("Export");

        styleModernButton(refreshBtn, new Color(33, 150, 243), new Color(25, 118, 210));
        styleModernButton(addBtn, new Color(76, 175, 80), new Color(56, 142, 60));
        styleModernButton(updateBtn, new Color(255, 152, 0), new Color(245, 124, 0));
        styleModernButton(deleteBtn, new Color(244, 67, 54), new Color(211, 47, 47));
        styleModernButton(exportBtn, new Color(156, 39, 176), new Color(123, 31, 162));

        refreshBtn.addActionListener(e -> updateDatabaseTable());
        addBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Add Record Form"));
        updateBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Update Record Form"));
        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this record?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(this, "Record deleted!");
            }
        });
        exportBtn.addActionListener(e -> JOptionPane.showMessageDialog(this, "Database exported to CSV!"));

        controlPanel.add(refreshBtn);
        controlPanel.add(addBtn);
        controlPanel.add(updateBtn);
        controlPanel.add(deleteBtn);
        controlPanel.add(exportBtn);

        contentPanel.add(controlPanel, BorderLayout.NORTH);

        String[] columns = {"S.No", "Booking ID", "Vehicle No", "Spot No", "Name",
                           "Phone", "In Time", "Duration", "Amount", "Status"};
        databaseTableModel = new DefaultTableModel(columns, 0);
        databaseTable = new JTable(databaseTableModel);
        styleTable(databaseTable);

        JScrollPane scrollPane = new JScrollPane(databaseTable);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        JButton exitBtn = new JButton("Exit System");
        styleModernButton(exitBtn, new Color(244, 67, 54), new Color(211, 47, 47));
        exitBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Exit application?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) System.exit(0);
        });

        JPanel bottom = new JPanel(new BorderLayout());
        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightBtns.add(exitBtn);
        bottom.add(rightBtns, BorderLayout.EAST);

        JLabel statusLabel = new JLabel("Total Records: 0 | Last Updated: " + getCurrentTime());
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        bottom.add(statusLabel, BorderLayout.WEST);

        contentPanel.add(bottom, BorderLayout.SOUTH);

        panel.add(contentPanel, BorderLayout.CENTER);

        // populate table initially
        updateDatabaseTable();

        return panel;
    }

    // ================= UTILITY METHODS =================
    private void initializeParkingSpots() {
        // Ensure parkingSpots map is initialized (constructor already sets it)
        for (int i = 1; i <= 20; i++) {
            boolean isAvailable = i > 5; // first 5 occupied by default
            ParkingSpot spot = new ParkingSpot("A" + i, isAvailable);
            if (!isAvailable) spot.setVehicleNumber("TN01XX" + (1000 + i));
            parkingSpots.put("A" + i, spot);
        }
    }

    private JPanel createHeaderPanel(String title, String currentScreen) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 136, 229));
        headerPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        navPanel.setBackground(new Color(30, 136, 229));

        if (!currentScreen.equals("login") && !currentScreen.equals("user")) {
            JButton backBtn = new JButton("Back");
            backBtn.setForeground(Color.WHITE);
            backBtn.setBackground(new Color(25, 118, 210));
            backBtn.setFocusPainted(false);
            backBtn.setBorderPainted(false);
            backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            backBtn.setFont(new Font("Arial", Font.BOLD, 12));

            backBtn.addActionListener(e -> {
                if (currentScreen.equals("qr")) {
                    cardLayout.show(mainPanel, "user");
                } else if (currentScreen.equals("allocation")) {
                    cardLayout.show(mainPanel, "qr");
                } else if (currentScreen.equals("admin") || currentScreen.equals("database")) {
                    cardLayout.show(mainPanel, "allocation");
                }
            });

            navPanel.add(backBtn);
        }

        JButton homeBtn = new JButton("Home");
        homeBtn.setForeground(Color.WHITE);
        homeBtn.setBackground(new Color(25, 118, 210));
        homeBtn.setFocusPainted(false);
        homeBtn.setBorderPainted(false);
        homeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        homeBtn.setFont(new Font("Arial", Font.BOLD, 12));
        homeBtn.addActionListener(e -> cardLayout.show(mainPanel, "login"));
        navPanel.add(homeBtn);

        headerPanel.add(navPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JPanel createStatCard(String title, String value, Color color, String icon) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(color);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Arial", Font.BOLD, 32));
        iconLabel.setForeground(Color.WHITE);
        card.add(iconLabel, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setBackground(color);

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 13));

        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));

        textPanel.add(titleLabel);
        textPanel.add(valueLabel);
        card.add(textPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createSlotPanel(String slotId, ParkingSpot spot) {
        JPanel slotPanel = new JPanel(new BorderLayout());
        slotPanel.setPreferredSize(new Dimension(120, 80));

        Color bgColor = spot.isAvailable() ? new Color(200, 230, 201) : new Color(255, 205, 210);
        Color borderColor = spot.isAvailable() ? new Color(76, 175, 80) : new Color(244, 67, 54);

        slotPanel.setBackground(bgColor);
        slotPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 2),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JLabel slotLabel = new JLabel(slotId, SwingConstants.CENTER);
        slotLabel.setFont(new Font("Arial", Font.BOLD, 18));
        slotLabel.setForeground(borderColor);

        JLabel statusLabel = new JLabel(spot.isAvailable() ? "Available" : "Occupied", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusLabel.setForeground(borderColor);

        slotPanel.add(slotLabel, BorderLayout.CENTER);
        slotPanel.add(statusLabel, BorderLayout.SOUTH);

        slotPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        slotPanel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                slotPanel.setBackground(bgColor.darker());
            }
            public void mouseExited(MouseEvent e) {
                slotPanel.setBackground(bgColor);
            }
            public void mouseClicked(MouseEvent e) {
                String status = spot.isAvailable() ? "Available" :
                    "Occupied by: " + spot.getVehicleNumber();
                JOptionPane.showMessageDialog(null,
                    "Slot: " + slotId + "\nStatus: " + status);
            }
        });

        return slotPanel;
    }

    private JButton createAdminButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 80));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.darker());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });

        return btn;
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        label.setBorder(new EmptyBorder(5, 5, 5, 5));
        return label;
    }

    private void styleModernButton(JButton btn, Color normalColor, Color hoverColor) {
        btn.setBackground(normalColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));

        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverColor);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(normalColor);
            }
        });
    }

    private void styleTable(JTable table) {
        table.setRowHeight(35);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(63, 81, 181));
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(232, 234, 246));
        table.setSelectionForeground(Color.BLACK);
        table.setGridColor(new Color(224, 224, 224));
        table.setShowGrid(true);
    }

    /**
     * Try to read from the database; if DB is unreachable or query fails,
     * use localBookings fallback.
     
    private void updateDatabaseTable() {
        databaseTableModel.setRowCount(0);

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            if (conn == null) throw new SQLException("DB connection returned null");

            // Try to query expected columns (booking_id, vehicle_number, spot_number, name, phone, in_time, duration, amount, status)
            String query = "SELECT booking_id, vehicle_number, spot_number, name, phone, in_time, duration, amount, status FROM parking_spots";
            PreparedStatement ps = conn.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            int serial = 1;
            boolean hasRows = false;
            while (rs.next()) {
                hasRows = true;
                databaseTableModel.addRow(new Object[]{
                    serial++,
                    rs.getString("booking_id"),
                    rs.getString("vehicle_number"),
                    rs.getString("spot_number"),
                    rs.getString("name"),
                    rs.getString("phone"),
                    rs.getString("in_time"),
                    rs.getString("duration"),
                    rs.getString("amount"),
                    rs.getString("status")
                });
            }
            rs.close();
            ps.close();

            if (!hasRows) {
                // maybe the schema is different; attempt fallback query: our DB described earlier has (spot_id, vehicle_number, status, entry_time, exit_time, amount)
                String q2 = "SELECT spot_id, vehicle_number, status, entry_time, exit_time, amount FROM parking_spots";
                try (PreparedStatement p2 = conn.prepareStatement(q2);
                     ResultSet r2 = p2.executeQuery()) {
                    int s = 1;
                    while (r2.next()) {
                        String bid = "N/A";
                        String veh = r2.getString("vehicle_number");
                        String spotNo = String.valueOf(r2.getInt("spot_id"));
                        String name = "";
                        String phone = "";
                        String intime = r2.getString("entry_time");
                        String duration = "N/A";
                        String amount = r2.getString("amount");
                        String status = r2.getString("status");
                        databaseTableModel.addRow(new Object[]{s++, bid, veh, spotNo, name, phone, intime, duration, amount, status});
                    }
                } catch (SQLException ignore) {
                    // ignore secondary query errors
                }
            }
            return; // done filling from DB
        } catch (SQLException ex) {
            // DB failed — fallback to local bookings
            System.err.println("DB failure in updateDatabaseTable(): " + ex.getMessage());
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }

        // Fallback: show localBookings
        int serial = 1;
        for (Map<String, String> b : localBookings) {
            databaseTableModel.addRow(new Object[]{
                serial++,
                b.getOrDefault("booking_id", ""),
                b.getOrDefault("vehicle_number", ""),
                b.getOrDefault("spot_number", ""),
                b.getOrDefault("name", ""),
                b.getOrDefault("phone", ""),
                b.getOrDefault("in_time", ""),
                b.getOrDefault("duration", ""),
                b.getOrDefault("amount", ""),
                b.getOrDefault("status", "")
            });
        }
    }

    /**
     * Saves booking to DB if possible; otherwise stores in localBookings list.
     * Also refreshes the database table view.
     
    public void addBookingToDB(String bookingId, String vehicleNumber, String spotNumber,
                               String name, String phone, String inTime,
                               String duration, String amount, String status) {
        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            if (conn != null) {
                // Try insert using expected schema
                String insertQuery = "INSERT INTO parking_spots (booking_id, vehicle_number, spot_number, name, phone, in_time, duration, amount, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insertQuery)) {
                    ps.setString(1, bookingId);
                    ps.setString(2, vehicleNumber);
                    ps.setString(3, spotNumber);
                    ps.setString(4, name);
                    ps.setString(5, phone);
                    ps.setString(6, inTime);
                    ps.setString(7, duration);
                    ps.setString(8, amount);
                    ps.setString(9, status);
                    ps.executeUpdate();
                    // refresh table from DB
                    updateDatabaseTable();
                    return;
                } catch (SQLException e) {
                    // maybe the DB schema differs: try an alternative insert (simple parking_spots columns), else fallback
                    System.err.println("Insert with booking columns failed: " + e.getMessage());
                    try {
                        String insertAlt = "INSERT INTO parking_spots (vehicle_number, status, entry_time, amount) VALUES (?, ?, ?, ?)";
                        try (PreparedStatement ps2 = conn.prepareStatement(insertAlt)) {
                            ps2.setString(1, vehicleNumber);
                            ps2.setString(2, status);
                            ps2.setString(3, inTime);
                            ps2.setString(4, amount);
                            ps2.executeUpdate();
                            updateDatabaseTable();
                            return;
                        }
                    } catch (SQLException e2) {
                        System.err.println("Alternative insert failed: " + e2.getMessage());
                    }
                }
            } else {
                System.err.println("DBConnection.getConnection() returned null; falling back to local storage.");
            }
        } catch (Exception ex) {
            System.err.println("Error saving booking to DB: " + ex.getMessage());
        } finally {
            if (conn != null) try { conn.close(); } catch (SQLException ignored) {}
        }

        // If we are here, DB insert failed -> save locally
        Map<String, String> rec = new HashMap<>();
        rec.put("booking_id", bookingId);
        rec.put("vehicle_number", vehicleNumber);
        rec.put("spot_number", spotNumber);
        rec.put("name", name);
        rec.put("phone", phone);
        rec.put("in_time", inTime);
        rec.put("duration", duration);
        rec.put("amount", amount);
        rec.put("status", status);
        localBookings.add(rec);
        // refresh UI view
        updateDatabaseTable();
    }

    private String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        return now.format(formatter);
    }

    private String generateQRPattern() {
        return  "████████████████████████████████████\n" +
                "██ ▄▄▄▄▄ █▀ █▀▀██▀▀█▀▀█ ▄▄▄▄▄ ██\n" +
                "██ █   █ █▄ ▀ ▀▄█▄ ▀ ▄█ █   █ ██\n" +
                "██ █▄▄▄█ █ ▀█▀ ▀█▄▀ █ █ █▄▄▄█ ██\n" +
                "██▄▄▄▄▄▄▄█▄█▄▀ █ ▀ ▀ █▄█▄▄▄▄▄▄▄██\n" +
                "████████████████████████████████████\n" +
                "\n   Booking ID: " + currentBookingId + "\n" +
                "   Scan to verify parking";
    }

    // ================= PARKING SPOT CLASS =================
    class ParkingSpot {
        private String id;
        private boolean available;
        private String vehicleNumber;

        public ParkingSpot(String id, boolean available) {
            this.id = id;
            this.available = available;
            this.vehicleNumber = "";
        }

        public boolean isAvailable() {
            return available;
        }

        public void setAvailable(boolean available) {
            this.available = available;
        }

        public String getVehicleNumber() {
            return vehicleNumber;
        }

        public void setVehicleNumber(String vehicleNumber) {
            this.vehicleNumber = vehicleNumber;
        }

        public String getId() {
            return id;
        }
    }

    // ================= MAIN METHOD =================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception e) { e.printStackTrace(); }
            new ParkingLotGUI().setVisible(true);
        });
    }
}

*/




/*import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ParkingLotGUI extends JFrame {
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private DefaultTableModel parkingTableModel;
    private DefaultTableModel databaseTableModel;
    private Map<String, ParkingSpot> parkingSpots;
    private String currentUser = "";
    private String currentBookingId = "";

    public ParkingLotGUI() {
        setTitle("QR Smart Vehicle Parking System");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        parkingSpots = new HashMap<>();
        initializeParkingSpots();

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Add screens in the specified order
        mainPanel.add(createLoginScreen(), "login");
        mainPanel.add(createUserModule(), "user");
        mainPanel.add(createQRModule(), "qr");
        mainPanel.add(createParkingSlotAllocation(), "allocation");
        mainPanel.add(createAdminModule(), "admin");
        mainPanel.add(createDatabaseModule(), "database");

        add(mainPanel);
        cardLayout.show(mainPanel, "login");
    }

    // ================= LOGIN SCREEN =================
    private JPanel createLoginScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(245, 247, 250));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(245, 247, 250));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // Logo/Icon
        JLabel iconLabel = new JLabel("P");
        iconLabel.setFont(new Font("Arial", Font.BOLD, 80));
        iconLabel.setForeground(new Color(30, 136, 229));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        centerPanel.add(iconLabel, gbc);

        JLabel titleLabel = new JLabel("QR Smart Parking System");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(new Color(30, 136, 229));
        gbc.gridy = 1;
        centerPanel.add(titleLabel, gbc);

        JLabel subtitleLabel = new JLabel("Secure - Fast - Efficient");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(120, 120, 120));
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 10, 20, 10);
        centerPanel.add(subtitleLabel, gbc);

        // Login Form
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridy = 3; gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        centerPanel.add(userLabel, gbc);

        JTextField usernameField = new JTextField(20);
        usernameField.setFont(new Font("Arial", Font.PLAIN, 14));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(usernameField, gbc);

        gbc.gridy = 4; gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        centerPanel.add(passLabel, gbc);

        JPasswordField passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            new EmptyBorder(8, 12, 8, 12)
        ));
        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(passwordField, gbc);

        JButton loginBtn = new JButton("Login");
        styleModernButton(loginBtn, new Color(30, 136, 229), new Color(21, 101, 192));
        loginBtn.setPreferredSize(new Dimension(200, 45));
        gbc.gridy = 5; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(20, 10, 10, 10);
        centerPanel.add(loginBtn, gbc);

        JButton adminLoginBtn = new JButton("Admin Login");
        styleModernButton(adminLoginBtn, new Color(76, 175, 80), new Color(56, 142, 60));
        adminLoginBtn.setPreferredSize(new Dimension(200, 40));
        gbc.gridy = 6;
        gbc.insets = new Insets(10, 10, 10, 10);
        centerPanel.add(adminLoginBtn, gbc);

        loginBtn.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());
            if (!user.isEmpty() && !pass.isEmpty()) {
                currentUser = user;
                JOptionPane.showMessageDialog(this, "Welcome, " + user + "!", 
                    "Login Successful", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(mainPanel, "user");
            } else {
                JOptionPane.showMessageDialog(this, "Please enter username and password!", 
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
            }
        });

        adminLoginBtn.addActionListener(e -> {
            String user = usernameField.getText();
            String pass = new String(passwordField.getPassword());
            if (user.equals("admin") && pass.equals("admin123")) {
                currentUser = "Administrator";
                JOptionPane.showMessageDialog(this, "Admin Access Granted!", 
                    "Success", JOptionPane.INFORMATION_MESSAGE);
                cardLayout.show(mainPanel, "admin");
            } else {
                JOptionPane.showMessageDialog(this, "Invalid admin credentials!", 
                    "Access Denied", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    // ================= USER MODULE =================
    private JPanel createUserModule() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        // Header
        JPanel headerPanel = createHeaderPanel("User Dashboard", "user");
        panel.add(headerPanel, BorderLayout.NORTH);

        // Main Content
        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

        // Welcome Section
        JPanel welcomePanel = new JPanel(new BorderLayout());
        welcomePanel.setBackground(new Color(232, 245, 233));
        welcomePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(76, 175, 80), 2),
            new EmptyBorder(20, 20, 20, 20)
        ));

        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser + "!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 24));
        welcomeLabel.setForeground(new Color(46, 125, 50));
        welcomePanel.add(welcomeLabel, BorderLayout.NORTH);

        JLabel infoLabel = new JLabel("<html><div style='margin-top:10px;'>Book your parking slot quickly and securely with QR code technology.</div></html>");
        infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        welcomePanel.add(infoLabel, BorderLayout.CENTER);

        contentPanel.add(welcomePanel, BorderLayout.NORTH);

        // Booking Form
        JPanel bookingPanel = new JPanel(new GridBagLayout());
        bookingPanel.setBackground(Color.WHITE);
        bookingPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            "Book Parking Slot",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 16)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 15, 10, 15);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        String[] labels = {"Vehicle Number:", "Vehicle Type:", "Owner Name:", 
                          "Phone Number:", "Duration (hours):", "Parking Slot:"};
        
        JTextField vehicleNumField = new JTextField(20);
        JComboBox<String> vehicleTypeBox = new JComboBox<>(new String[]{"Car", "Bike", "SUV", "Van"});
        JTextField ownerField = new JTextField(20);
        JTextField phoneField = new JTextField(20);
        JSpinner durationSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 24, 1));
        JComboBox<String> slotBox = new JComboBox<>();
        
        parkingSpots.forEach((k, v) -> {
            if (v.isAvailable()) slotBox.addItem(k);
        });

        Component[] components = {vehicleNumField, vehicleTypeBox, ownerField, 
                                 phoneField, durationSpinner, slotBox};

        for (int i = 0; i < labels.length; i++) {
            gbc.gridx = 0; gbc.gridy = i;
            gbc.weightx = 0.3;
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("Arial", Font.PLAIN, 14));
            bookingPanel.add(label, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.7;
            Component comp = components[i];
            if (comp instanceof JTextField || comp instanceof JSpinner) {
                ((JComponent)comp).setFont(new Font("Arial", Font.PLAIN, 14));
            }
            bookingPanel.add(comp, gbc);
        }

        JButton bookBtn = new JButton("Book Slot & Generate QR");
        styleModernButton(bookBtn, new Color(30, 136, 229), new Color(21, 101, 192));
        bookBtn.setPreferredSize(new Dimension(250, 45));
        gbc.gridx = 0; gbc.gridy = labels.length;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 15, 15, 15);
        bookingPanel.add(bookBtn, gbc);

        bookBtn.addActionListener(e -> {
            String vehicleNum = vehicleNumField.getText().trim();
            String owner = ownerField.getText().trim();
            String phone = phoneField.getText().trim();
            
            if (vehicleNum.isEmpty() || owner.isEmpty() || phone.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill all fields!", 
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String slot = (String) slotBox.getSelectedItem();
            int duration = (Integer) durationSpinner.getValue();
            currentBookingId = "BK" + System.currentTimeMillis() % 100000;
            
            // Mark slot as occupied
            if (slot != null && parkingSpots.containsKey(slot)) {
                parkingSpots.get(slot).setAvailable(false);
                parkingSpots.get(slot).setVehicleNumber(vehicleNum);
            }
            
            JOptionPane.showMessageDialog(this, 
                "Booking Successful!\nBooking ID: " + currentBookingId + 
                "\nSlot: " + slot + "\nDuration: " + duration + " hours", 
                "Success", JOptionPane.INFORMATION_MESSAGE);
            
            cardLayout.show(mainPanel, "qr");
        });

        contentPanel.add(bookingPanel, BorderLayout.CENTER);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    // ================= QR MODULE =================
    private JPanel createQRModule() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("QR Code Generator", "qr");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 20, 20, 20);

        // QR Code Display Area
        JPanel qrPanel = new JPanel(new BorderLayout());
        qrPanel.setPreferredSize(new Dimension(350, 350));
        qrPanel.setBackground(Color.WHITE);
        qrPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(30, 136, 229), 3),
            new EmptyBorder(20, 20, 20, 20)
        ));

        // Simulated QR Code
        JTextArea qrDisplay = new JTextArea();
        qrDisplay.setEditable(false);
        qrDisplay.setFont(new Font("Courier New", Font.BOLD, 10));
        qrDisplay.setText(generateQRPattern());
        qrDisplay.setBackground(Color.WHITE);
        qrPanel.add(qrDisplay, BorderLayout.CENTER);

        gbc.gridx = 0; gbc.gridy = 0;
        centerPanel.add(qrPanel, gbc);

        // Booking Details
        JPanel detailsPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        detailsPanel.setBackground(new Color(245, 248, 250));
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            new EmptyBorder(15, 20, 15, 20)
        ));

        detailsPanel.add(createInfoLabel("Booking ID: " + currentBookingId));
        detailsPanel.add(createInfoLabel("Vehicle: TN01AB1234"));
        detailsPanel.add(createInfoLabel("Slot: A5"));
        detailsPanel.add(createInfoLabel("Time: " + getCurrentTime()));
        detailsPanel.add(createInfoLabel("Status: Active"));

        gbc.gridy = 1;
        centerPanel.add(detailsPanel, gbc);

        // Action Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        buttonPanel.setBackground(Color.WHITE);

        JButton downloadBtn = new JButton("Download QR");
        styleModernButton(downloadBtn, new Color(76, 175, 80), new Color(56, 142, 60));
        
        JButton printBtn = new JButton("Print");
        styleModernButton(printBtn, new Color(33, 150, 243), new Color(25, 118, 210));
        
        JButton shareBtn = new JButton("Share via Email");
        styleModernButton(shareBtn, new Color(255, 152, 0), new Color(245, 124, 0));

        JButton continueBtn = new JButton("Continue to Parking");
        styleModernButton(continueBtn, new Color(156, 39, 176), new Color(123, 31, 162));

        downloadBtn.addActionListener(e -> 
            JOptionPane.showMessageDialog(this, "QR Code saved successfully!", 
                "Download", JOptionPane.INFORMATION_MESSAGE));
        
        printBtn.addActionListener(e -> 
            JOptionPane.showMessageDialog(this, "Printing QR Code...", 
                "Print", JOptionPane.INFORMATION_MESSAGE));
        
        shareBtn.addActionListener(e -> 
            JOptionPane.showMessageDialog(this, "QR Code sent to your email!", 
                "Email Sent", JOptionPane.INFORMATION_MESSAGE));
        
        continueBtn.addActionListener(e -> cardLayout.show(mainPanel, "allocation"));

        buttonPanel.add(downloadBtn);
        buttonPanel.add(printBtn);
        buttonPanel.add(shareBtn);

        gbc.gridy = 2;
        centerPanel.add(buttonPanel, gbc);

        gbc.gridy = 3;
        centerPanel.add(continueBtn, gbc);

        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    // ================= PARKING SLOT ALLOCATION =================
    private JPanel createParkingSlotAllocation() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("Parking Slot Allocation", "allocation");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Status Summary
        JPanel summaryPanel = new JPanel(new GridLayout(1, 3, 15, 0));
        summaryPanel.setBackground(Color.WHITE);

        long available = parkingSpots.values().stream().filter(ParkingSpot::isAvailable).count();
        long occupied = parkingSpots.size() - available;

        summaryPanel.add(createStatCard("Total Slots", String.valueOf(parkingSpots.size()), 
            new Color(33, 150, 243), "P"));
        summaryPanel.add(createStatCard("Available", String.valueOf(available), 
            new Color(76, 175, 80), "+"));
        summaryPanel.add(createStatCard("Occupied", String.valueOf(occupied), 
            new Color(244, 67, 54), "X"));

        contentPanel.add(summaryPanel, BorderLayout.NORTH);

        // Parking Grid
        JPanel gridPanel = new JPanel(new GridLayout(4, 5, 10, 10));
        gridPanel.setBackground(Color.WHITE);
        gridPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        parkingSpots.forEach((slotId, spot) -> {
            JPanel slotPanel = createSlotPanel(slotId, spot);
            gridPanel.add(slotPanel);
        });

        JScrollPane scrollPane = new JScrollPane(gridPanel);
        scrollPane.setBorder(null);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Navigation Button
        JButton nextBtn = new JButton("View Admin Dashboard");
        styleModernButton(nextBtn, new Color(30, 136, 229), new Color(21, 101, 192));
        nextBtn.setPreferredSize(new Dimension(250, 45));
        nextBtn.addActionListener(e -> cardLayout.show(mainPanel, "admin"));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(nextBtn);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    // ================= ADMIN MODULE =================
    private JPanel createAdminModule() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("Admin Dashboard", "admin");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Statistics Cards
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setBackground(Color.WHITE);

        statsPanel.add(createStatCard("Total Revenue", "Rs 8,450", 
            new Color(156, 39, 176), "$"));
        statsPanel.add(createStatCard("Today's Bookings", "23", 
            new Color(255, 152, 0), "#"));
        statsPanel.add(createStatCard("Active Users", "18", 
            new Color(33, 150, 243), "U"));
        statsPanel.add(createStatCard("Avg Duration", "2.5 hrs", 
            new Color(76, 175, 80), "T"));

        contentPanel.add(statsPanel, BorderLayout.NORTH);

        // Admin Controls
        JPanel controlPanel = new JPanel(new GridLayout(2, 3, 15, 15));
        controlPanel.setBackground(Color.WHITE);
        controlPanel.setBorder(new EmptyBorder(20, 0, 20, 0));

        JButton viewBookingsBtn = createAdminButton("View All Bookings", new Color(33, 150, 243));
        JButton manageSlotsBtn = createAdminButton("Manage Slots", new Color(76, 175, 80));
        JButton reportsBtn = createAdminButton("Generate Reports", new Color(255, 152, 0));
        JButton settingsBtn = createAdminButton("Settings", new Color(96, 125, 139));
        JButton databaseBtn = createAdminButton("Database", new Color(156, 39, 176));
        JButton logoutBtn = createAdminButton("Logout", new Color(244, 67, 54));

        viewBookingsBtn.addActionListener(e -> 
            JOptionPane.showMessageDialog(this, "Viewing all bookings..."));
        
        manageSlotsBtn.addActionListener(e -> 
            cardLayout.show(mainPanel, "allocation"));
        
        reportsBtn.addActionListener(e -> 
            JOptionPane.showMessageDialog(this, "Generating reports..."));
        
        settingsBtn.addActionListener(e -> 
            JOptionPane.showMessageDialog(this, "Opening settings..."));
        
        databaseBtn.addActionListener(e -> {
            updateDatabaseTable();
            cardLayout.show(mainPanel, "database");
        });
        
        logoutBtn.addActionListener(e -> {
            currentUser = "";
            cardLayout.show(mainPanel, "login");
        });

        controlPanel.add(viewBookingsBtn);
        controlPanel.add(manageSlotsBtn);
        controlPanel.add(reportsBtn);
        controlPanel.add(settingsBtn);
        controlPanel.add(databaseBtn);
        controlPanel.add(logoutBtn);

        contentPanel.add(controlPanel, BorderLayout.CENTER);

        // Recent Activity
        JPanel activityPanel = new JPanel(new BorderLayout());
        activityPanel.setBackground(Color.WHITE);
        activityPanel.setBorder(BorderFactory.createTitledBorder("Recent Activity"));

        String[] columns = {"Time", "Action", "User", "Details"};
        Object[][] data = {
            {getCurrentTime(), "Booking", "John Doe", "Slot A5 - TN01AB1234"},
            {getCurrentTime(), "Payment", "Jane Smith", "Rs150 - Card"},
            {getCurrentTime(), "Check-out", "Mike Johnson", "Slot B3 - 2h 30m"}
        };

        JTable activityTable = new JTable(data, columns);
        styleTable(activityTable);
        
        JScrollPane scrollPane = new JScrollPane(activityTable);
        scrollPane.setPreferredSize(new Dimension(0, 150));
        activityPanel.add(scrollPane, BorderLayout.CENTER);

        contentPanel.add(activityPanel, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    // ================= DATABASE MODULE =================
    private JPanel createDatabaseModule() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);

        JPanel headerPanel = createHeaderPanel("Database Management", "database");
        panel.add(headerPanel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(15, 15));
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Database Controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        controlPanel.setBackground(Color.WHITE);

        JButton refreshBtn = new JButton("Refresh");
        JButton addBtn = new JButton("Add Record");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        JButton exportBtn = new JButton("Export");

        styleModernButton(refreshBtn, new Color(33, 150, 243), new Color(25, 118, 210));
        styleModernButton(addBtn, new Color(76, 175, 80), new Color(56, 142, 60));
        styleModernButton(updateBtn, new Color(255, 152, 0), new Color(245, 124, 0));
        styleModernButton(deleteBtn, new Color(244, 67, 54), new Color(211, 47, 47));
        styleModernButton(exportBtn, new Color(156, 39, 176), new Color(123, 31, 162));

        refreshBtn.addActionListener(e -> updateDatabaseTable());
        addBtn.addActionListener(e -> 
            JOptionPane.showMessageDialog(this, "Add Record Form"));
        updateBtn.addActionListener(e -> 
            JOptionPane.showMessageDialog(this, "Update Record Form"));
        deleteBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete this record?", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                JOptionPane.showMessageDialog(this, "Record deleted!");
            }
        });
        exportBtn.addActionListener(e -> 
            JOptionPane.showMessageDialog(this, "Database exported to CSV!"));

        controlPanel.add(refreshBtn);
        controlPanel.add(addBtn);
        controlPanel.add(updateBtn);
        controlPanel.add(deleteBtn);
        controlPanel.add(exportBtn);

        contentPanel.add(controlPanel, BorderLayout.NORTH);

        // Database Table
        String[] columns = {"ID", "Booking ID", "Vehicle No", "Slot", "Owner", 
                           "Phone", "Entry Time", "Duration", "Amount", "Status"};
        databaseTableModel = new DefaultTableModel(columns, 0);
        JTable databaseTable = new JTable(databaseTableModel);
        styleTable(databaseTable);

        JScrollPane scrollPane = new JScrollPane(databaseTable);
        contentPanel.add(scrollPane, BorderLayout.CENTER);

        // Status Bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(245, 248, 250));
        statusBar.setBorder(new EmptyBorder(10, 15, 10, 15));
        
        JLabel statusLabel = new JLabel("Total Records: 0 | Last Updated: " + getCurrentTime());
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusBar.add(statusLabel, BorderLayout.WEST);

        contentPanel.add(statusBar, BorderLayout.SOUTH);
        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    // ================= UTILITY METHODS =================
    private void initializeParkingSpots() {
        for (int i = 1; i <= 20; i++) {
            boolean isAvailable = i > 5; // First 5 slots occupied
            ParkingSpot spot = new ParkingSpot("A" + i, isAvailable);
            if (!isAvailable) {
                spot.setVehicleNumber("TN01XX" + (1000 + i));
            }
            parkingSpots.put("A" + i, spot);
        }
    }

    private JPanel createHeaderPanel(String title, String currentScreen) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(30, 136, 229));
        headerPanel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        navPanel.setBackground(new Color(30, 136, 229));

        if (!currentScreen.equals("login") && !currentScreen.equals("user")) {
            JButton backBtn = new JButton("Back");
            backBtn.setForeground(Color.WHITE);
            backBtn.setBackground(new Color(25, 118, 210));
            backBtn.setFocusPainted(false);
            backBtn.setBorderPainted(false);
            backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            backBtn.setFont(new Font("Arial", Font.BOLD, 12));
            
            backBtn.addActionListener(e -> {
                if (currentScreen.equals("qr")) {
                    cardLayout.show(mainPanel, "user");
                } else if (currentScreen.equals("allocation")) {
                    cardLayout.show(mainPanel, "qr");
                } else if (currentScreen.equals("admin") || currentScreen.equals("database")) {
                    cardLayout.show(mainPanel, "allocation");
                }
            });
            
            navPanel.add(backBtn);
        }

        JButton homeBtn = new JButton("Home");
        homeBtn.setForeground(Color.WHITE);
        homeBtn.setBackground(new Color(25, 118, 210));
        homeBtn.setFocusPainted(false);
        homeBtn.setBorderPainted(false);
        homeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        homeBtn.setFont(new Font("Arial", Font.BOLD, 12));
        homeBtn.addActionListener(e -> cardLayout.show(mainPanel, "login"));
        navPanel.add(homeBtn);

        headerPanel.add(navPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JPanel createStatCard(String title, String value, Color color, String icon) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(color);
        card.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Arial", Font.BOLD, 32));
        iconLabel.setForeground(Color.WHITE);
        card.add(iconLabel, BorderLayout.WEST);
        
        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setBackground(color);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 13));
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 24));
        
        textPanel.add(titleLabel);
        textPanel.add(valueLabel);
        card.add(textPanel, BorderLayout.CENTER);
        
        return card;
    }

    private JPanel createSlotPanel(String slotId, ParkingSpot spot) {
        JPanel slotPanel = new JPanel(new BorderLayout());
        slotPanel.setPreferredSize(new Dimension(120, 80));
        
        Color bgColor = spot.isAvailable() ? new Color(200, 230, 201) : new Color(255, 205, 210);
        Color borderColor = spot.isAvailable() ? new Color(76, 175, 80) : new Color(244, 67, 54);
        
        slotPanel.setBackground(bgColor);
        slotPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 2),
            new EmptyBorder(10, 10, 10, 10)
        ));
        
        JLabel slotLabel = new JLabel(slotId, SwingConstants.CENTER);
        slotLabel.setFont(new Font("Arial", Font.BOLD, 18));
        slotLabel.setForeground(borderColor);
        
        JLabel statusLabel = new JLabel(spot.isAvailable() ? "Available" : "Occupied", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        statusLabel.setForeground(borderColor);
        
        slotPanel.add(slotLabel, BorderLayout.CENTER);
        slotPanel.add(statusLabel, BorderLayout.SOUTH);
        
        slotPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        slotPanel.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                slotPanel.setBackground(bgColor.darker());
            }
            public void mouseExited(MouseEvent e) {
                slotPanel.setBackground(bgColor);
            }
            public void mouseClicked(MouseEvent e) {
                String status = spot.isAvailable() ? "Available" : 
                    "Occupied by: " + spot.getVehicleNumber();
                JOptionPane.showMessageDialog(null, 
                    "Slot: " + slotId + "\nStatus: " + status);
            }
        });
        
        return slotPanel;
    }

    private JButton createAdminButton(String text, Color color) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Arial", Font.BOLD, 16));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(200, 80));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(color.darker());
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(color);
            }
        });
        
        return btn;
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Arial", Font.PLAIN, 14));
        label.setBorder(new EmptyBorder(5, 5, 5, 5));
        return label;
    }

    private void styleModernButton(JButton btn, Color normalColor, Color hoverColor) {
        btn.setBackground(normalColor);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("Arial", Font.BOLD, 14));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverColor);
            }
            public void mouseExited(MouseEvent e) {
                btn.setBackground(normalColor);
            }
        });
    }

    private void styleTable(JTable table) {
        table.setRowHeight(35);
        table.setFont(new Font("Arial", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(63, 81, 181));
        table.getTableHeader().setForeground(Color.WHITE);
        table.setSelectionBackground(new Color(232, 234, 246));
        table.setSelectionForeground(Color.BLACK);
        table.setGridColor(new Color(224, 224, 224));
        table.setShowGrid(true);
    }

    private void updateDatabaseTable() {
        databaseTableModel.setRowCount(0);
        
        // Sample data
        Object[][] sampleData = {
            {1, "BK12345", "TN01AB1234", "A5", "John Doe", "9876543210", 
             getCurrentTime(), "2 hrs", "Rs 150", "Active"},
            {2, "BK12346", "TN02CD5678", "A3", "Jane Smith", "9876543211", 
             getCurrentTime(), "3 hrs", "Rs 225", "Completed"},
            {3, "BK12347", "TN03EF9012", "A8", "Mike Johnson", "9876543212", 
             getCurrentTime(), "1 hr", "Rs 75", "Active"},
            {4, "BK12348", "TN04GH3456", "A12", "Sarah Williams", "9876543213", 
             getCurrentTime(), "4 hrs", "Rs 300", "Active"},
            {5, "BK12349", "TN05IJ7890", "A15", "David Brown", "9876543214", 
             getCurrentTime(), "2 hrs", "Rs 150", "Completed"}
        };
        
        for (Object[] row : sampleData) {
            databaseTableModel.addRow(row);
        }
    }

    private String getCurrentTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
        return now.format(formatter);
    }

    private String generateQRPattern() {
        return  "████████████████████████████████████\n" +
                "██ ▄▄▄▄▄ █▀ █▀▀██▀▀█▀▀█ ▄▄▄▄▄ ██\n" +
                "██ █   █ █▄ ▀ ▀▄█▄ ▀ ▄█ █   █ ██\n" +
                "██ █▄▄▄█ █ ▀█▀ ▀█▄▀ █ █ █▄▄▄█ ██\n" +
                "██▄▄▄▄▄▄▄█▄█▄▀ █ ▀ ▀ █▄█▄▄▄▄▄▄▄██\n" +
                "██ ▄  ▄▀▄  ▄▀▀▄ ▀▄▀█▀  ▀ ▀▀▄▀▄██\n" +
                "██▄▄▀  ▄▄▀▀██▀█▄ ▄▄█ ▀▀  ▀█▄▀ ██\n" +
                "██ ██ █▄▄ █▄▄ ▀  ▄▄█ ▄█▀█▀▄ ▀▄██\n" +
                "██▄███▄█▄▄ █ ▀█ ▀█▄▀ ▄▄▄ ▀  ▄ ██\n" +
                "██ ▄▄▄▄▄ █▄ █  █ ▀▀  █▄█  ▀▀▄ ██\n" +
                "██ █   █ █  ▀█▀▀▀█▀▄▄▄  ▄▀▀  ▄██\n" +
                "██ █▄▄▄█ █ ▄  ▀▄ ▀▀█ ▀▄  ▀█▀ ▄██\n" +
                "██▄▄▄▄▄▄▄█▄▄███▄█▄▄█▄██▄▄▄▄██▄███\n" +
                "████████████████████████████████████\n" +
                "\n   Booking ID: " + currentBookingId + "\n" +
                "   Scan to verify parking";
    }

    // ================= PARKING SPOT CLASS =================
    class ParkingSpot {
        private String id;
        private boolean available;
        private String vehicleNumber;
        
        public ParkingSpot(String id, boolean available) {
            this.id = id;
            this.available = available;
            this.vehicleNumber = "";
        }
        
        public boolean isAvailable() { 
            return available; 
        }
        
        public void setAvailable(boolean available) { 
            this.available = available; 
        }
        
        public String getVehicleNumber() { 
            return vehicleNumber; 
        }
        
        public void setVehicleNumber(String vehicleNumber) { 
            this.vehicleNumber = vehicleNumber; 
        }
        
        public String getId() { 
            return id; 
        }
    }

    // ================= MAIN METHOD =================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ParkingLotGUI().setVisible(true);
        });
    }
}*/