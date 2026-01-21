package siwes.project.school_website;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class DebugPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String storedHash = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi";
        String inputPassword = "admin123";

        boolean matches = encoder.matches(inputPassword, storedHash);
        System.out.println("Password 'admin123' matches stored hash: " + matches);

        // Test with wrong password
        boolean wrongMatches = encoder.matches("admin", storedHash);
        System.out.println("Password 'admin' matches stored hash: " + wrongMatches);

        // Generate a new hash for admin123 to compare
        String newHash = encoder.encode("admin123");
        System.out.println("New hash for 'admin123': " + newHash);

        boolean newMatches = encoder.matches("admin123", newHash);
        System.out.println("Password 'admin123' matches new hash: " + newMatches);
    }
}