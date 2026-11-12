package com.oasis.reservation;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Optional;

/** Registration and authentication using PBKDF2 password hashes. */
public final class AuthService {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    public record User(int id, String username, String fullName) { }

    public User register(String username, String fullName, char[] password)
            throws SQLException, GeneralSecurityException {
        String cleanUsername = username == null ? "" : username.trim();
        String cleanName = fullName == null ? "" : fullName.trim();
        if (!cleanUsername.matches("[A-Za-z0-9_.-]{3,30}")) {
            throw new IllegalArgumentException(
                    "Username must be 3–30 characters (letters, digits, ., _ or -).");
        }
        if (cleanName.length() < 2 || cleanName.length() > 80) {
            throw new IllegalArgumentException("Enter a name between 2 and 80 characters.");
        }
        if (password == null || password.length < 8) {
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        }

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        String hash = derive(password, salt);
        String saltText = Base64.getEncoder().encodeToString(salt);

        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO users(username, full_name, password_hash, password_salt) VALUES(?,?,?,?)")) {
            statement.setString(1, cleanUsername);
            statement.setString(2, cleanName);
            statement.setString(3, hash);
            statement.setString(4, saltText);
            statement.executeUpdate();
        } catch (SQLException exception) {
            if (exception.getMessage() != null
                    && exception.getMessage().toLowerCase().contains("unique")) {
                throw new IllegalArgumentException("That username is already registered.");
            }
            throw exception;
        }
        return login(cleanUsername, password)
                .orElseThrow(() -> new SQLException("Account was created but could not be loaded."));
    }

    public Optional<User> login(String username, char[] password)
            throws SQLException, GeneralSecurityException {
        if (username == null || password == null) return Optional.empty();
        String sql = "SELECT id, username, full_name, password_hash, password_salt FROM users WHERE username = ?";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) return Optional.empty();
                byte[] salt = Base64.getDecoder().decode(result.getString("password_salt"));
                byte[] expected = Base64.getDecoder().decode(result.getString("password_hash"));
                byte[] actual = Base64.getDecoder().decode(derive(password, salt));
                if (!MessageDigest.isEqual(expected, actual)) return Optional.empty();
                return Optional.of(new User(result.getInt("id"),
                        result.getString("username"), result.getString("full_name")));
            }
        }
    }

    private static String derive(char[] password, byte[] salt)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH);
        try {
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(hash);
        } finally {
            spec.clearPassword();
        }
    }
}
