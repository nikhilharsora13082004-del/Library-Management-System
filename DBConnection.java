import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
public class DBConnection {
    private static final String URL      = "jdbc:mysql://localhost:3306/library_db";
    private static final String USERNAME = "root";       // your MySQL username
    private static final String PASSWORD = "Nikhil13082004"; // your MySQL password
    public static Connection getConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(URL, USERNAME, PASSWORD);
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL Driver not found! Did you add the JAR?");
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            System.out.println("Cannot connect to database! Check username/password.");
            e.printStackTrace();
            return null;
        }
    }
}
