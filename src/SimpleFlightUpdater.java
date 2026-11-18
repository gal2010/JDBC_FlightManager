import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Secure updater for the delay status of flight {@code LX354} in the {@code Arrivals} table.
 *
 * <h2>Security audit (SQL Injection remediation)</h2>
 * <p>
 * <b>Previous (vulnerable) implementation:</b> the class used a plain {@link java.sql.Statement}
 * and built the query by raw string concatenation:
 * </p>
 * <pre>
 * "UPDATE Arrivals SET delay_status = '" + userInput + "' WHERE flight_number = 'LX354'"
 * </pre>
 * <p>
 * Because the user input was pasted directly into the SQL text, the database could not tell
 * data apart from code. The payload {@code Landed' #} produced:
 * </p>
 * <pre>
 * UPDATE Arrivals SET delay_status = 'Landed' #' WHERE flight_number = 'LX354'
 * </pre>
 * <ul>
 *   <li>The single quote ({@code '}) closed the string literal early.</li>
 *   <li>The hash ({@code #}) opened a MySQL single-line comment, so the whole
 *       {@code WHERE} clause was ignored.</li>
 *   <li>Result: all 299 rows of {@code Arrivals} were overwritten with {@code 'Landed'}.</li>
 * </ul>
 *
 * <p>
 * <b>Current (secure) implementation:</b> the query is a pre-compiled
 * {@link PreparedStatement} with {@code ?} placeholders. The SQL structure is sent to and parsed
 * by the server <i>before</i> any user data is bound. Values passed through
 * {@link PreparedStatement#setString(int, String)} are always treated as literal data, never as
 * SQL syntax, so quotes and comment characters lose any special meaning. The same payload now
 * updates exactly one row (LX354) and stores the literal text {@code Landed' #}.
 * </p>
 *
 * @author Gal Florenthal, Segev Morag
 * @version 2.0 (secured)
 */
public class SimpleFlightUpdater {

    /** JDBC connection URL for the normalized FlightDB schema. */
    private static final String URL =
            "jdbc:mysql://localhost:3306/FlightDB?allowLoadLocalInfile=true&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jerusalem&useSSL=false";

    /** MySQL administrative user. */
    private static final String USER = "root";

    /** Parameterized update query: placeholders are bound as data, never parsed as SQL. */
    private static final String UPDATE_SQL =
            "UPDATE Arrivals SET delay_status = ? WHERE flight_number = ?";

    /** The flight whose status this utility updates. */
    private static final String TARGET_FLIGHT = "LX354";

    /**
     * Prompts for the MySQL root password and a new delay status, then safely updates
     * flight {@code LX354} using a {@link PreparedStatement} inside try-with-resources.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.print("Please enter your MySQL root password: ");
            String password = scanner.nextLine();

            System.out.print("Enter new delay status for flight '" + TARGET_FLIGHT + "': ");
            String userInput = scanner.nextLine();
            System.out.println("\nNew status entered: " + userInput);

            int affectedRows = updateDelayStatus(password, userInput);
            System.out.println("Rows affected: " + affectedRows);
        }
    }

    /**
     * Updates the delay status of {@link #TARGET_FLIGHT} with a parameterized query.
     *
     * @param password  MySQL root password
     * @param newStatus new delay status; bound as a literal value, so it cannot alter the query
     * @return number of rows updated (expected: 1), or 0 if a database error occurred
     */
    static int updateDelayStatus(String password, String newStatus) {
        try (Connection conn = DriverManager.getConnection(URL, USER, password);
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SQL)) {

            System.out.println("Executing SQL (parameterized):");
            System.out.println(UPDATE_SQL);

            pstmt.setString(1, newStatus);
            pstmt.setString(2, TARGET_FLIGHT);
            return pstmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            return 0;
        }
    }
}
