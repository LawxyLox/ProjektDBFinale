import io.github.cdimascio.dotenv.Dotenv;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;

public class test1234 extends JFrame {
    private JButton button1;
    private JPanel Panel1;
    private JTextField gesloField;
    private JTextField emailField;
    private JTextField userField;

    private DatabaseHelper dbHelper;

    public test1234() {

        Dotenv dotenv = Dotenv.load();

        // Get environment variables
        String dbUrl = dotenv.get("DB_URL");
        String dbUsername = dotenv.get("DB_USERNAME");
        String dbPassword = dotenv.get("DB_PASSWORD");

        try {
            Connection conn = DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
            dbHelper = new DatabaseHelper(conn);
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database connection failed!");
            return;
        }

        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String email = emailField.getText().trim();
                String geslo = gesloField.getText().trim();
                String username = userField.getText().trim();

                if (!isValidEmail(email)) {
                    JOptionPane.showMessageDialog(button1, "Invalid email address.");
                    return;
                }

                if (geslo.length() < 6) {
                    JOptionPane.showMessageDialog(button1, "Password must be at least 6 characters.");
                    return;
                }

                if (dbHelper.saveUser(email, geslo, username)) {
                    JOptionPane.showMessageDialog(button1, "User saved successfully!");

                    // After successful registration, open MainForm
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            // Close the registration window
                            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(button1);
                            topFrame.dispose();

                            // Open the MainForm
                            MainForm mainForm = new MainForm();  // Create MainForm instance
                            JFrame frame = new JFrame("Main Dashboard");
                            frame.setContentPane(mainForm.getMainPanel());  // Set the panel of MainForm
                            frame.setSize(800, 600);
                            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                            frame.setVisible(true);
                        }
                    });
                } else {
                    JOptionPane.showMessageDialog(button1, "Error saving user.");
                }
            }
        });
    }

    // Email validation using regex
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }

    public static void main(String[] args) {

        Dotenv dotenv = Dotenv.load();

        test1234 h = new test1234();
        h.setContentPane(h.Panel1);
        h.setTitle("User Registration");
        h.setSize(800, 600);
        h.setVisible(true);
        h.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }
}
