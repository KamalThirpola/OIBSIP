package com.oasis.reservation;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/** Creates and opens the local SQLite database used by the demo application. */
public final class Database {
    private static final String DB_URL =
            "jdbc:sqlite:" + Path.of("reservation.db").toAbsolutePath();

    private Database() { }

    public static Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(DB_URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    public static void initialize() throws SQLException {
        try (Connection connection = connect();
             Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL COLLATE NOCASE UNIQUE,
                    full_name TEXT NOT NULL,
                    password_hash TEXT NOT NULL,
                    password_salt TEXT NOT NULL,
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
            statement.execute("""
                CREATE TABLE IF NOT EXISTS bookings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    pnr TEXT NOT NULL UNIQUE,
                    user_id INTEGER NOT NULL,
                    passenger_name TEXT NOT NULL,
                    age INTEGER NOT NULL,
                    train_number TEXT NOT NULL,
                    train_name TEXT NOT NULL,
                    source_station TEXT NOT NULL,
                    destination_station TEXT NOT NULL,
                    journey_date TEXT NOT NULL,
                    travel_class TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'CONFIRMED'
                        CHECK(status IN ('CONFIRMED', 'CANCELLED')),
                    created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                )
                """);
            statement.execute("""
                CREATE INDEX IF NOT EXISTS idx_bookings_user
                ON bookings(user_id, created_at DESC)
                """);
        }
    }
}
