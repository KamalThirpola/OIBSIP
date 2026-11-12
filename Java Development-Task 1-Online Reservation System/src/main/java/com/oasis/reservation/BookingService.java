package com.oasis.reservation;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Reservation operations. This demo does not connect to a live railway system. */
public final class BookingService {
    private static final SecureRandom RANDOM = new SecureRandom();

    public record Booking(String pnr, String passengerName, int age,
                          String trainNumber, String trainName,
                          String source, String destination, String journeyDate,
                          String travelClass, String status) { }

    public Booking book(int userId, String passengerName, int age,
                        String trainNumber, String trainName,
                        String source, String destination, LocalDate journeyDate,
                        String travelClass) throws SQLException {
        String passenger = required(passengerName, "Passenger name");
        String number = required(trainNumber, "Train number");
        String name = required(trainName, "Train name");
        String from = required(source, "Source station");
        String to = required(destination, "Destination station");
        String coach = required(travelClass, "Travel class");

        if (age < 1 || age > 120) throw new IllegalArgumentException("Age must be between 1 and 120.");
        if (!number.matches("[A-Za-z0-9-]{1,15}")) throw new IllegalArgumentException("Train number is invalid.");
        if (from.equalsIgnoreCase(to)) throw new IllegalArgumentException("Source and destination must be different.");
        if (journeyDate == null || journeyDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Journey date must be today or a future date.");
        }
        if (!List.of("Sleeper", "AC 3 Tier", "AC 2 Tier", "AC First Class", "Second Sitting")
                .contains(coach)) throw new IllegalArgumentException("Select a valid travel class.");

        String pnr = generatePnr();
        String sql = """
            INSERT INTO bookings(pnr, user_id, passenger_name, age, train_number,
                train_name, source_station, destination_station, journey_date, travel_class)
            VALUES(?,?,?,?,?,?,?,?,?,?)
            """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pnr);
            statement.setInt(2, userId);
            statement.setString(3, passenger);
            statement.setInt(4, age);
            statement.setString(5, number);
            statement.setString(6, name);
            statement.setString(7, from);
            statement.setString(8, to);
            statement.setString(9, journeyDate.toString());
            statement.setString(10, coach);
            statement.executeUpdate();
        }
        return findByPnr(pnr, userId).orElseThrow(() -> new SQLException("Booking saved but could not be loaded."));
    }

    public boolean cancel(String pnr, int userId) throws SQLException {
        String sql = "UPDATE bookings SET status='CANCELLED' WHERE pnr=? AND user_id=? AND status='CONFIRMED'";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pnr == null ? "" : pnr.trim());
            statement.setInt(2, userId);
            return statement.executeUpdate() == 1;
        }
    }

    public List<Booking> history(int userId) throws SQLException {
        List<Booking> bookings = new ArrayList<>();
        String sql = """
            SELECT pnr, passenger_name, age, train_number, train_name, source_station,
                   destination_station, journey_date, travel_class, status
            FROM bookings WHERE user_id=? ORDER BY created_at DESC, id DESC
            """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) bookings.add(readBooking(result));
            }
        }
        return bookings;
    }

    public java.util.Optional<Booking> findByPnr(String pnr, int userId) throws SQLException {
        String sql = """
            SELECT pnr, passenger_name, age, train_number, train_name, source_station,
                   destination_station, journey_date, travel_class, status
            FROM bookings WHERE pnr=? AND user_id=?
            """;
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pnr);
            statement.setInt(2, userId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? java.util.Optional.of(readBooking(result))
                                     : java.util.Optional.empty();
            }
        }
    }

    private static Booking readBooking(ResultSet result) throws SQLException {
        return new Booking(result.getString("pnr"), result.getString("passenger_name"),
                result.getInt("age"), result.getString("train_number"),
                result.getString("train_name"), result.getString("source_station"),
                result.getString("destination_station"), result.getString("journey_date"),
                result.getString("travel_class"), result.getString("status"));
    }

    private static String generatePnr() {
        // Ten digits; uniqueness is enforced by the database constraint.
        return String.format("%010d", RANDOM.nextLong(1_000_000_000L, 10_000_000_000L));
    }

    private static String required(String value, String label) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty() || clean.length() > 100) {
            throw new IllegalArgumentException(label + " is required (maximum 100 characters).");
        }
        return clean;
    }
}
