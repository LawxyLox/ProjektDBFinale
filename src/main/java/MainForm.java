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

    private LocalDate selectedDate1;
    private LocalDate selectedDate2;

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
    public MainForm() {
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
                // Get selected dates
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

                // Save to database
                Connection connection = connectToDatabase();
                if (connection != null) {
                    try {
                        String sql = "INSERT INTO najemnine (datum_izposoje, datum_vracila) VALUES (?, ?)";
                        PreparedStatement stmt = connection.prepareStatement(sql);
                        stmt.setDate(1, java.sql.Date.valueOf(datumIzposoje));
                        stmt.setDate(2, java.sql.Date.valueOf(datumVracila));

                        stmt.executeUpdate();
                        stmt.close();
                        connection.close();

                        JOptionPane.showMessageDialog(null, "Reservation saved!");
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(null, "Failed to save reservation.");
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
        daycomboBox1 = new JComboBox<>();
        monthcomboBox1 = new JComboBox<>();
        yearcomboBox1 = new JComboBox<>();
        daycomboBox2 = new JComboBox<>();
        monthcomboBox2 = new JComboBox<>();
        yearcomboBox2 = new JComboBox<>();

        avtocomboBox1 = new JComboBox<>();
        rezervirajButton = new JButton("Rezerviraj");

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
