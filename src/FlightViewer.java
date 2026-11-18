import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class FlightViewer {

    private static final String URL =
            "jdbc:mysql://localhost:3306/FlightDB?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";
    private static final String USER = "root";

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Please enter your MySQL root password: ");
            String password = scanner.nextLine();

            System.out.print("Enter terminal number (2, 3, 4, 5): ");
            String terminal = scanner.nextLine().trim();

            String sql = "SELECT a.id, a.flight_number, r.from_city, a.scheduled, a.delay_status, a.terminal " +
                         "FROM Arrivals a " +
                         "INNER JOIN FlightRoutes r ON a.flight_number = r.flight_number " +
                         "WHERE a.terminal = ? " +
                         "ORDER BY a.scheduled ASC";

            try (Connection conn = DriverManager.getConnection(URL, USER, password);
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, terminal);

                try (ResultSet rs = pstmt.executeQuery()) {
                    System.out.println("\n --- Arrivals at Terminal " + terminal + " (sorted by scheduled time) ---");
                    System.out.printf("%-5s | %-10s | %-15s | %-10s | %-20s | %-8s%n",
                            "ID", "Flight", "From", "Scheduled", "Status", "Terminal");
                    System.out.println("------------------------------------------------------------------------------------");

                    int count = 0;
                    while (rs.next()) {
                        System.out.printf("%-5d | %-10s | %-15s | %-10s | %-20s | %-8s%n",
                                rs.getInt("id"),
                                rs.getString("flight_number"),
                                rs.getString("from_city"),
                                rs.getString("scheduled"),
                                rs.getString("delay_status"),
                                rs.getString("terminal"));
                        count++;
                    }
                    System.out.println("------------------------------------------------------------------------------------");
                    System.out.println("Total flights at terminal " + terminal + ": " + count);
                }
            } catch (SQLException e) {
                System.err.println("Database error: " + e.getMessage());
            }
        }
    }
}
