import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DatabaseHelper {
    private final Connection connection;

    public DatabaseHelper(Connection connection) {
        this.connection = connection;
    }

    // Check if the email already exists in the table
    public boolean emailExists(String email) {
        String sql = "SELECT 1 FROM najemniki WHERE email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next(); // returns true if at least one row is found
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // or throw exception if preferred
        }
    }

    // Insert a new user (only if the email doesn't exist)
    public boolean saveUser(String email, String geslo, String username) {
        if (emailExists(email)) {
            return false; // already exists, skip insert
        }

        String sql = "INSERT INTO najemniki (email, geslo, username) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, geslo);
            stmt.setString(3, username);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
