import io.github.cdimascio.dotenv.Dotenv;

import java.sql.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;

public class MainForm {
    private JPanel mainPanel;  // JPanel for the GUI layout
    private JComboBox<Integer> daycomboBox1;
    private JComboBox<Integer> monthcomboBox1;
    private JComboBox<Integer> yearcomboBox1;
    private JComboBox<Integer> daycomboBox2;
    private JComboBox<Integer> monthcomboBox2;
    private JComboBox<Integer> yearcomboBox2;
    private JComboBox avtocomboBox1;
    private JButton rezervirajButton;
    private JTabbedPane tabbedPane1;
    private JPanel najemTab;
    private JPanel najetoTab;
    private JButton refreshButton;
    private JComboBox najetiComboBox1;
    private JButton odstraniRezervacijoButton;

    private LocalDate selectedDate1;
    private LocalDate selectedDate2;

    private String username;
    private String email;

    // Getter for the mainPanel to display it in the JFrame
    public JPanel getMainPanel() {
        return mainPanel;
    }

    private Connection connectToDatabase() {
        try {
            Dotenv dotenv = Dotenv.load();

            // Get environment variables
            String dbUrl = dotenv.get("DB_URL");
            String dbUsername = dotenv.get("DB_USERNAME");
            String dbPassword = dotenv.get("DB_PASSWORD");

            // Establish connection to the database
            Connection connection = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            return connection;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }


    // Constructor: Called when the form is initialized
    public MainForm(String username, String email) {

        this.username = username;
        this.email = email;
        // Initialize the combo boxes with the current date values
        LocalDate today = LocalDate.now();

        // Set initial values in combo boxes for today
        daycomboBox1.setSelectedItem(today.getDayOfMonth());
        monthcomboBox1.setSelectedItem(today.getMonthValue());
        yearcomboBox1.setSelectedItem(today.getYear());
        daycomboBox2.setSelectedItem(today.getDayOfMonth());
        monthcomboBox2.setSelectedItem(today.getMonthValue());
        yearcomboBox2.setSelectedItem(today.getYear());

        // Set the initial dates
        selectedDate1 = today;
        selectedDate2 = today;

        // Add action listeners for combo boxes
        addActionListeners();

        populateAvtoComboBox();

        rezervirajButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1. Get selected dates
                Integer day1 = (Integer) daycomboBox1.getSelectedItem();
                Integer month1 = (Integer) monthcomboBox1.getSelectedItem();
                Integer year1 = (Integer) yearcomboBox1.getSelectedItem();

                Integer day2 = (Integer) daycomboBox2.getSelectedItem();
                Integer month2 = (Integer) monthcomboBox2.getSelectedItem();
                Integer year2 = (Integer) yearcomboBox2.getSelectedItem();

                if (day1 == null || month1 == null || year1 == null ||
                        day2 == null || month2 == null || year2 == null) {
                    JOptionPane.showMessageDialog(null, "Please select valid dates.");
                    return;
                }

                LocalDate datumIzposoje = LocalDate.of(year1, month1, day1);
                LocalDate datumVracila = LocalDate.of(year2, month2, day2);

                // 2. Get selected car string from combo box
                String selectedItem = (String) avtocomboBox1.getSelectedItem();
                if (selectedItem == null || !selectedItem.contains("Tablica:")) {
                    JOptionPane.showMessageDialog(null, "Please select a vehicle.");
                    return;
                }

                // 3. Extract license plate from the selected item
                String[] parts = selectedItem.split("Tablica:");
                String tablica = parts[1].trim();  // Get the part after "Tablica:"

                // 4. Connect to database
                Connection connection = connectToDatabase();
                if (connection != null) {
                    try {
                        // 5. Get najemnik_id based on email
                        String getNajemnikIdQuery = "SELECT id FROM najemniki WHERE email = ?";
                        PreparedStatement getStmt = connection.prepareStatement(getNajemnikIdQuery);
                        getStmt.setString(1, email);
                        ResultSet rs = getStmt.executeQuery();

                        int najemnikId = -1;
                        if (rs.next()) {
                            najemnikId = rs.getInt("id");
                        }

                        if (najemnikId != -1) {
                            // 6. Insert into najemnine
                            String insertSql = "INSERT INTO najemnine (datum_izposoje, datum_vracila, najemnik_id) VALUES (?, ?, ?)";
                            PreparedStatement insertStmt = connection.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
                            insertStmt.setDate(1, java.sql.Date.valueOf(datumIzposoje));
                            insertStmt.setDate(2, java.sql.Date.valueOf(datumVracila));
                            insertStmt.setInt(3, najemnikId);
                            insertStmt.executeUpdate();

                            ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                            int najemninaId = -1;
                            if (generatedKeys.next()) {
                                najemninaId = generatedKeys.getInt(1);
                            }

                            insertStmt.close();

                            // 7. Update avtomobili with najemnina_id
                            if (najemninaId != -1) {
                                String updateSql = "UPDATE avtomobili SET najemnina_id = ? WHERE registerska_tablica = ?";
                                PreparedStatement updateStmt = connection.prepareStatement(updateSql);
                                updateStmt.setInt(1, najemninaId);
                                updateStmt.setString(2, tablica);
                                int affectedRows = updateStmt.executeUpdate();

                                if (affectedRows > 0) {
                                    JOptionPane.showMessageDialog(null, "Reservation and vehicle update successful!");
                                } else {
                                    JOptionPane.showMessageDialog(null, "Reservation saved, but vehicle update failed.");
                                }

                                updateStmt.close();
                            }
                        } else {
                            JOptionPane.showMessageDialog(null, "No user found with the provided email.");
                        }

                        populateAvtoComboBox();

                        rs.close();
                        getStmt.close();
                        connection.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Failed to save reservation.");
                    }
                }
            }
        });


        odstraniRezervacijoButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get selected vehicle from the najetiComboBox1
                String selectedCar = (String) najetiComboBox1.getSelectedItem();

                if (selectedCar == null || selectedCar.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Please select a vehicle to remove the reservation.");
                    return;
                }

                // Extract the "registerska_tablica" from the display string
                String[] parts = selectedCar.split(" | ");  // Assuming the format is: "Model: <model> | Barva: <barva> | Tablica: <registerska_tablica>"
                String registerskaTablica = parts[parts.length - 1];  // Get the last part as the "registerska_tablica"

                // Connect to the database
                Connection connection = connectToDatabase();
                if (connection != null) {
                    try {
                        // SQL query to update the najemnina_id to NULL for the selected car (based on registerska_tablica)
                        String sql = "UPDATE avtomobili SET najemnina_id = NULL WHERE registerska_tablica = ?";
                        PreparedStatement stmt = connection.prepareStatement(sql);
                        stmt.setString(1, registerskaTablica);  // Set the "registerska_tablica" to match the selected car

                        populateAvtoComboBox();
                        refreshNajetiComboBox();
                        int rowsAffected = stmt.executeUpdate();

                        if (rowsAffected > 0) {
                            JOptionPane.showMessageDialog(null, "Reservation successfully removed for the vehicle.");

                            // Optionally, refresh the combo box to reflect the update
                            populateAvtoComboBox();  // Call this method to refresh the combo box

                        } else {
                            JOptionPane.showMessageDialog(null, "No vehicle found with the provided registerska_tablica.");
                        }

                        stmt.close();
                        connection.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Failed to remove reservation.");
                    }
                }
            }
        });



        refreshButton.addActionListener(new ActionListener() {
            /**
             * Invoked when an action occurs.
             *
             * @param e the event to be processed
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                Connection connection = connectToDatabase();

                if (connection != null) {
                    try {
                        // 1. Get the 'najemnik_id' for the current user (email)
                        String getNajemnikIdQuery = "SELECT id FROM najemniki WHERE email = ?";
                        PreparedStatement getStmt = connection.prepareStatement(getNajemnikIdQuery);
                        getStmt.setString(1, email);  // Set the email for search
                        ResultSet rs = getStmt.executeQuery();

                        // 2. Retrieve the 'najemnik_id'
                        int najemnikId = -1;
                        if (rs.next()) {
                            najemnikId = rs.getInt("id");
                        }

                        // 3. If we found the 'najemnik_id', proceed with fetching associated cars
                        if (najemnikId != -1) {
                            // Query to get all vehicles ('avtomobili') associated with the user's reservations ('najemnine')
                            String getAvtomobiliQuery = "SELECT a.registerska_tablica " +
                                    "FROM avtomobili a " +
                                    "JOIN najemnine n ON a.najemnina_id = n.id " +  // Assuming 'avto_id' is the FK in 'najemnine' for 'avtomobili'
                                    "WHERE n.najemnik_id = ?";
                            PreparedStatement getAvtomobiliStmt = connection.prepareStatement(getAvtomobiliQuery);
                            getAvtomobiliStmt.setInt(1, najemnikId);  // Set the 'najemnik_id' to get the vehicles associated with this user
                            ResultSet carRs = getAvtomobiliStmt.executeQuery();

                            // Clear any existing items in the combo box
                            najetiComboBox1.removeAllItems();

                            // 4. Add the retrieved vehicles to the combo box
                            while (carRs.next()) {
                                String registerskaTablica = carRs.getString("registerska_tablica");
                                najetiComboBox1.addItem(registerskaTablica);  // Add vehicle to combo box
                            }

                            // Close resources
                            carRs.close();
                            getAvtomobiliStmt.close();
                        } else {
                            JOptionPane.showMessageDialog(null, "No user found with the provided email.");
                        }

                        rs.close();
                        getStmt.close();
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Failed to fetch vehicles.");
                    }
                }

            }
        });
    }

    private void populateAvtoComboBox() {
        Connection connection = connectToDatabase();

        if (connection != null) {
            try {
                // JOIN avtomobili with modeli and barve to get the names
                String sql = """
                SELECT a.registerska_tablica, m.ime AS model_ime, b.ime AS barva_ime
                FROM avtomobili a
                JOIN modeli m ON a.model_id = m.id
                JOIN barve b ON a.barva_id = b.id
                WHERE a.najemnina_id IS NULL
            """;

                PreparedStatement stmt = connection.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();

                avtocomboBox1.removeAllItems();

                while (rs.next()) {
                    String modelIme = rs.getString("model_ime");
                    String barvaIme = rs.getString("barva_ime");
                    String tablica = rs.getString("registerska_tablica");

                    // Corrected: only 3 placeholders
                    String displayText = String.format("Model: %s | Barva: %s | Tablica: %s", modelIme, barvaIme, tablica);
                    avtocomboBox1.addItem(displayText);
                }

                rs.close();
                stmt.close();
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }




    // Method to add action listeners to the combo boxes
    private void addActionListeners() {
        // Listener for daycomboBox1 (first date selection)
        ActionListener dateSelectionListener1 = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSelectedDate1();
                updateDateRestrictions();  // Ensure comboBox2 is updated when comboBox1 changes
            }
        };

        daycomboBox1.addActionListener(dateSelectionListener1);
        monthcomboBox1.addActionListener(dateSelectionListener1);
        yearcomboBox1.addActionListener(dateSelectionListener1);

        // Listener for daycomboBox2 (second date selection)
        ActionListener dateSelectionListener2 = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSelectedDate2();
            }
        };

        daycomboBox2.addActionListener(dateSelectionListener2);
        monthcomboBox2.addActionListener(dateSelectionListener2);
        yearcomboBox2.addActionListener(dateSelectionListener2);

        // Update date restrictions when the form is first initialized
        updateDateRestrictions();
    }

    // Method to update the selected date for comboBox1
    private void updateSelectedDate1() {
        // Add null checks for selected items
        Integer day = (Integer) daycomboBox1.getSelectedItem();
        Integer month = (Integer) monthcomboBox1.getSelectedItem();
        Integer year = (Integer) yearcomboBox1.getSelectedItem();

        if (day != null && month != null && year != null) {
            selectedDate1 = LocalDate.of(year, month, day);
        }
    }

    // Method to update the selected date for comboBox2
    private void updateSelectedDate2() {
        // Add null checks for selected items
        Integer day = (Integer) daycomboBox2.getSelectedItem();
        Integer month = (Integer) monthcomboBox2.getSelectedItem();
        Integer year = (Integer) yearcomboBox2.getSelectedItem();

        if (day != null && month != null && year != null) {
            selectedDate2 = LocalDate.of(year, month, day);
        }
    }

    private void refreshNajetiComboBox() {
        Connection connection = connectToDatabase();
        if (connection != null) {
            try {
                // Query to get all reserved vehicles for the logged-in user
                String getReservedVehiclesQuery = "SELECT a.registerska_tablica " +
                        "FROM avtomobili a " +
                        "JOIN najemnine n ON a.najemnina_id = n.id " +
                        "WHERE n.najemnik_id = (SELECT id FROM najemniki WHERE email = ?)";
                PreparedStatement stmt = connection.prepareStatement(getReservedVehiclesQuery);
                stmt.setString(1, email);  // Pass the logged-in user's email to filter by user

                ResultSet rs = stmt.executeQuery();

                // Clear the combo box before populating it with new data
                najetiComboBox1.removeAllItems();

                // Populate najetiComboBox1 with reserved vehicles
                while (rs.next()) {
                    String registerskaTablica = rs.getString("registerska_tablica");
                    najetiComboBox1.addItem(registerskaTablica);
                }

                rs.close();
                stmt.close();
                connection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to fetch reserved vehicles.");
            }
        }
    }


    // Method to update the restrictions for comboBox2 based on comboBox1's selected date
    private void updateDateRestrictions() {
        // Ensure combo box values are valid
        Integer selectedDay1 = (Integer) daycomboBox1.getSelectedItem();
        Integer selectedMonth1 = (Integer) monthcomboBox1.getSelectedItem();
        Integer selectedYear1 = (Integer) yearcomboBox1.getSelectedItem();

        // If any values are null, return early
        if (selectedDay1 == null || selectedMonth1 == null || selectedYear1 == null) {
            return;
        }

        LocalDate date1 = LocalDate.of(selectedYear1, selectedMonth1, selectedDay1);

        // Set the date1 in comboBox1 to the valid date
        if (date1.isBefore(LocalDate.now())) {
            daycomboBox1.setSelectedItem(LocalDate.now().getDayOfMonth());
            monthcomboBox1.setSelectedItem(LocalDate.now().getMonthValue());
            yearcomboBox1.setSelectedItem(LocalDate.now().getYear());
            date1 = LocalDate.now();  // Reset to today if date1 is in the past
        }

        // If selectedDate2 is before selectedDate1, update comboBox2 to match comboBox1
        if (selectedDate2.isBefore(date1)) {
            daycomboBox2.setSelectedItem(date1.getDayOfMonth());
            monthcomboBox2.setSelectedItem(date1.getMonthValue());
            yearcomboBox2.setSelectedItem(date1.getYear());
        }

        // Clear invalid items in daycomboBox2 based on selectedDate1
        // First, remove items that are invalid for the selected month and year
        for (int i = 0; i < daycomboBox2.getItemCount(); i++) {
            Integer day = (Integer) daycomboBox2.getItemAt(i);
            Integer month = (Integer) monthcomboBox2.getSelectedItem();
            Integer year = (Integer) yearcomboBox2.getSelectedItem();

            if (day != null && month != null && year != null) {
                try {
                    // Try to create a valid LocalDate with the selected day, month, and year
                    LocalDate date2 = LocalDate.of(year, month, day);

                    // If date2 is before date1, remove the invalid day from comboBox2
                    if (date2.isBefore(date1)) {
                        daycomboBox2.removeItemAt(i);
                        i--;  // Adjust index after removal
                    }
                } catch (java.time.DateTimeException e) {
                    // Handle invalid date (e.g., February 30 or September 31)
                    daycomboBox2.removeItemAt(i);
                    i--;  // Adjust index after removal
                }
            }
        }
    }


    // The `createUIComponents()` method is required by the Designer
    private void createUIComponents() {
        // Ensure all components are initialized before performing actions on them
        tabbedPane1 = new javax.swing.JTabbedPane();
        najemTab = new javax.swing.JPanel();
        najetoTab = new javax.swing.JPanel();

        najetiComboBox1 = new javax.swing.JComboBox<>();

        daycomboBox1 = new JComboBox<>();
        monthcomboBox1 = new JComboBox<>();
        yearcomboBox1 = new JComboBox<>();
        daycomboBox2 = new JComboBox<>();
        monthcomboBox2 = new JComboBox<>();
        yearcomboBox2 = new JComboBox<>();

        avtocomboBox1 = new JComboBox<>();
        rezervirajButton = new JButton("Rezerviraj");

        odstraniRezervacijoButton = new JButton("Odstrani Rezervacijo");


        // Populate months (1 to 12)
        for (int i = 1; i <= 12; i++) {
            monthcomboBox1.addItem(i);  // 1, 2, ..., 12
            monthcomboBox2.addItem(i);
        }

        // Populate years starting from the current year (dynamic)
        int currentYear = LocalDate.now().getYear();
        for (int i = currentYear; i <= currentYear + 10; i++) { // Adding 10 years
            yearcomboBox1.addItem(i);
            yearcomboBox2.addItem(i);
        }

        // Populate days (1 to 31)
        for (int i = 1; i <= 31; i++) {
            daycomboBox1.addItem(i);
            daycomboBox2.addItem(i);
        }

        // Set combo boxes to be editable
        daycomboBox1.setEditable(true);
        monthcomboBox1.setEditable(true);
        yearcomboBox1.setEditable(true);
        daycomboBox2.setEditable(true);
        monthcomboBox2.setEditable(true);
        yearcomboBox2.setEditable(true);
    }
}
