# OIBSIP – Online Reservation System

A desktop train-reservation application built with **Java 17, Swing, JDBC and SQLite** for the OASIS INFOBYTE Java Development Task 1.

## Features

- User registration and login
- Password hashing with PBKDF2 (no plain-text passwords stored)
- Train reservation form with passenger, train, class, route and journey-date validation
- Demo train-name auto-fill for train numbers 12001, 12951 and 12301 (not a live railway lookup)
- Numeric train-number validation
- Unique 10-digit PNR generation
- Booking confirmation with booking details
- PNR-based booking lookup with a dedicated Fetch button
- Confirmation-based cancellation that deletes the selected booking from SQLite
- Personal booking history
- SQLite database created automatically on first launch
- Prepared SQL statements and input validation

## Requirements

- JDK 17 or newer
- Maven 3.8 or newer
- Windows, macOS or Linux with a graphical desktop

## Run in VS Code

1. Open this project folder in VS Code:
   `OIBSIP/Java Development-Task 1-Online Reservation System`
2. Open the integrated terminal in that folder.
3. Check Java and Maven:

   ```bash
   java -version
   mvn -version
   ```

4. Start the application:

   ```bash
   mvn clean compile exec:java
   ```

The application creates `reservation.db` in the project folder automatically. Keep this file private; it contains account and reservation data and is excluded from Git.

## Project structure

```text
Java Development-Task 1-Online Reservation System/
├── pom.xml
├── README.md
├── .gitignore
└── src/
    └── main/
        └── java/
            └── com/
                └── oasis/
                    └── reservation/
                        ├── Main.java
                        ├── Database.java
                        ├── AuthService.java
                        └── BookingService.java
```

## Notes

- This is an educational demonstration, not a connection to IRCTC or a live railway reservation network.
- Train numbers and availability are entered for demonstration; no real tickets are issued.
- Use a strong, unique password when registering.
- To reset local demo data, close the application and delete `reservation.db`. This permanently deletes local users and bookings.

## Task checklist

- [x] Login and registration
- [x] Reservation form
- [x] PNR generation
- [x] Confirmation dialog
- [x] PNR lookup using Fetch
- [x] Confirmation-based cancellation and database deletion
- [x] Booking history
- [x] SQLite persistence
- [x] Input validation

## Verification status

The source code includes the features listed above. Run the application locally and complete the manual test checklist before treating the project as fully tested or submitting it. GUI behavior has not been verified in this repository update.
