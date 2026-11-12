package com.oasis.reservation;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Swing user interface for the OIBSIP train reservation demonstration. */
public final class Main {
    private static final Color NAVY = new Color(24, 43, 75);
    private static final Color BLUE = new Color(37, 99, 235);
    private static final AuthService AUTH = new AuthService();
    private static final BookingService BOOKINGS = new BookingService();
    // Small demonstration catalogue; this is not a live railway timetable.
    private static final Map<String, String> TRAIN_CATALOG = Map.of(
            "12001", "Bhopal Shatabdi Express",
            "12951", "Mumbai Rajdhani Express",
            "12301", "Howrah Rajdhani Express");

    private Main() { }

    public static void main(String[] args) {
        try {
            Database.initialize();
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(null,
                    "Could not initialize the database.\n" + exception.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        SwingUtilities.invokeLater(Main::showLogin);
    }

    private static void showLogin() {
        JTextField username = new JTextField(18);
        JPasswordField password = new JPasswordField(18);
        JPanel form = formPanel();
        addField(form, "Username", username);
        addField(form, "Password", password);

        Object[] options = {"Login", "Create account", "Exit"};
        while (true) {
            int choice = JOptionPane.showOptionDialog(null, form,
                    "Online Reservation System — Sign in",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE,
                    null, options, options[0]);
            if (choice == 0) {
                try {
                    var user = AUTH.login(username.getText(), password.getPassword());
                    if (user.isPresent()) {
                        openDashboard(user.get());
                        return;
                    }
                    showError("Invalid username or password.");
                    password.setText("");
                } catch (SQLException | GeneralSecurityException exception) {
                    showError("Login failed: " + exception.getMessage());
                }
            } else if (choice == 1) {
                showRegistration();
                password.setText("");
            } else {
                return;
            }
        }
    }

    private static void showRegistration() {
        JTextField fullName = new JTextField(18);
        JTextField username = new JTextField(18);
        JPasswordField password = new JPasswordField(18);
        JPanel form = formPanel();
        addField(form, "Full name", fullName);
        addField(form, "Username (3–30 characters)", username);
        addField(form, "Password (at least 8 characters)", password);
        int choice = JOptionPane.showConfirmDialog(null, form, "Create account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        try {
            AuthService.User user = AUTH.register(username.getText(), fullName.getText(),
                    password.getPassword());
            JOptionPane.showMessageDialog(null, "Account created. Welcome, "
                    + user.fullName() + "!", "Registration successful",
                    JOptionPane.INFORMATION_MESSAGE);
            openDashboard(user);
        } catch (SQLException | GeneralSecurityException | IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private static void openDashboard(AuthService.User user) {
        JFrame frame = new JFrame("Online Reservation System");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(760, 520);
        frame.setMinimumSize(new Dimension(650, 440));
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(18, 18));
        root.setBorder(new EmptyBorder(22, 26, 22, 26));
        root.setBackground(new Color(245, 247, 251));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(20, 22, 20, 22));
        JLabel title = new JLabel("ONLINE RESERVATION SYSTEM");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel welcome = new JLabel("Welcome, " + user.fullName());
        welcome.setForeground(new Color(220, 230, 245));
        header.add(title, BorderLayout.NORTH);
        header.add(welcome, BorderLayout.SOUTH);

        JPanel actions = new JPanel(new GridLayout(2, 2, 14, 14));
        actions.setOpaque(false);
        JButton book = actionButton("Book a ticket", "Enter passenger and journey details");
        JButton cancel = actionButton("Cancel a ticket", "Cancel a confirmed booking using PNR");
        JButton history = actionButton("Booking history", "View your reservations");
        JButton logout = actionButton("Log out", "Return to sign-in");
        actions.add(book);
        actions.add(cancel);
        actions.add(history);
        actions.add(logout);

        JLabel note = new JLabel("<html><b>Demo application:</b> this project does not connect "
                + "to IRCTC or issue real railway tickets.</html>");
        note.setForeground(new Color(90, 100, 115));
        root.add(header, BorderLayout.NORTH);
        root.add(actions, BorderLayout.CENTER);
        root.add(note, BorderLayout.SOUTH);

        book.addActionListener(event -> showBooking(frame, user));
        cancel.addActionListener(event -> showCancellation(frame, user));
        history.addActionListener(event -> showHistory(frame, user));
        logout.addActionListener(event -> {
            frame.dispose();
            showLogin();
        });

        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private static void showBooking(JFrame parent, AuthService.User user) {
        JTextField passenger = new JTextField(20);
        JTextField age = new JTextField(8);
        JTextField trainNumber = new JTextField(12);
        JTextField trainName = new JTextField(20);
        trainName.setEditable(false);
        trainName.setToolTipText("Automatically filled from the demonstration train catalogue");
        trainNumber.getDocument().addDocumentListener(new DocumentListener() {
            private void updateTrainName() {
                String number = trainNumber.getText().trim();
                trainName.setText(TRAIN_CATALOG.getOrDefault(number, ""));
            }
            @Override public void insertUpdate(DocumentEvent event) { updateTrainName(); }
            @Override public void removeUpdate(DocumentEvent event) { updateTrainName(); }
            @Override public void changedUpdate(DocumentEvent event) { updateTrainName(); }
        });
        JTextField source = new JTextField(16);
        JTextField destination = new JTextField(16);
        JTextField date = new JTextField(LocalDate.now().plusDays(1).toString(), 12);
        JComboBox<String> travelClass = new JComboBox<>(new String[]{
                "Sleeper", "AC 3 Tier", "AC 2 Tier", "AC First Class", "Second Sitting"});

        JPanel form = formPanel();
        addField(form, "Passenger name", passenger);
        addField(form, "Passenger age", age);
        addField(form, "Train number", trainNumber);
        addField(form, "Train name (auto-filled)", trainName);
        addField(form, "Source station", source);
        addField(form, "Destination station", destination);
        addField(form, "Journey date (YYYY-MM-DD)", date);
        addField(form, "Travel class", travelClass);

        int choice = JOptionPane.showConfirmDialog(parent, form, "Book a train ticket",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;

        try {
            int passengerAge = Integer.parseInt(age.getText().trim());
            if (trainName.getText().isBlank()) {
                throw new IllegalArgumentException(
                        "Enter a supported demo train number: 12001, 12951 or 12301.");
            }
            LocalDate journeyDate = LocalDate.parse(date.getText().trim());
            BookingService.Booking booking = BOOKINGS.book(user.id(), passenger.getText(),
                    passengerAge, trainNumber.getText(), trainName.getText(),
                    source.getText(), destination.getText(), journeyDate,
                    (String) travelClass.getSelectedItem());
            JOptionPane.showMessageDialog(parent, bookingDetails(booking),
                    "Booking confirmed — PNR " + booking.pnr(),
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException exception) {
            showError("Age must be a whole number.");
        } catch (DateTimeParseException exception) {
            showError("Enter the journey date in YYYY-MM-DD format.");
        } catch (SQLException | IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private static void showCancellation(JFrame parent, AuthService.User user) {
        JTextField pnr = new JTextField(14);
        JPanel form = formPanel();
        addField(form, "10-digit PNR", pnr);
        int choice = JOptionPane.showConfirmDialog(parent, form, "Cancel ticket",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choice != JOptionPane.OK_OPTION) return;
        String code = pnr.getText().trim();
        if (!code.matches("\\d{10}")) {
            showError("Enter a valid 10-digit PNR.");
            return;
        }
        try {
            var booking = BOOKINGS.findByPnr(code, user.id());
            if (booking.isEmpty()) {
                showError("No booking found for this PNR in your account.");
                return;
            }
            if (!"CONFIRMED".equals(booking.get().status())) {
                showError("This ticket is already cancelled.");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(parent,
                    "Cancel this booking?\n\n" + bookingDetails(booking.get()),
                    "Confirm cancellation", JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                boolean cancelled = BOOKINGS.cancel(code, user.id());
                JOptionPane.showMessageDialog(parent, cancelled
                        ? "Ticket " + code + " has been cancelled."
                        : "The ticket could not be cancelled. Refresh and try again.");
            }
        } catch (SQLException exception) {
            showError("Cancellation failed: " + exception.getMessage());
        }
    }

    private static void showHistory(JFrame parent, AuthService.User user) {
        try {
            List<BookingService.Booking> items = BOOKINGS.history(user.id());
            if (items.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "You have no bookings yet.",
                        "Booking history", JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            String[] columns = {"PNR", "Passenger", "Train", "From", "To",
                    "Journey date", "Class", "Status"};
            Object[][] rows = new Object[items.size()][columns.length];
            for (int i = 0; i < items.size(); i++) {
                var b = items.get(i);
                rows[i] = new Object[]{b.pnr(), b.passengerName(),
                        b.trainNumber() + " - " + b.trainName(), b.source(),
                        b.destination(), b.journeyDate(), b.travelClass(), b.status()};
            }
            JTable table = new JTable(rows, columns);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.setRowHeight(26);
            JScrollPane scroll = new JScrollPane(table);
            scroll.setPreferredSize(new Dimension(690, 260));
            JOptionPane.showMessageDialog(parent, scroll, "Your booking history",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (SQLException exception) {
            showError("Could not load history: " + exception.getMessage());
        }
    }

    private static JPanel formPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        return panel;
    }

    private static void addField(JPanel panel, String label, JComponent input) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 6, 6, 6);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = panel.getComponentCount() / 2;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        panel.add(input, c);
    }

    private static JButton actionButton(String title, String description) {
        JButton button = new JButton("<html><div style='text-align:left;padding:8px'>"
                + "<b style='font-size:15px'>" + title + "</b><br>"
                + "<span style='color:#596579'>" + description + "</span></div></html>");
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBackground(Color.WHITE);
        button.setForeground(NAVY);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 235)),
                new EmptyBorder(10, 12, 10, 12)));
        return button;
    }

    private static String bookingDetails(BookingService.Booking b) {
        return "PNR: " + b.pnr()
                + "\nPassenger: " + b.passengerName() + " (Age " + b.age() + ")"
                + "\nTrain: " + b.trainNumber() + " — " + b.trainName()
                + "\nRoute: " + b.source() + " → " + b.destination()
                + "\nJourney date: " + b.journeyDate()
                + "\nClass: " + b.travelClass()
                + "\nStatus: " + b.status();
    }

    private static void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Online Reservation System",
                JOptionPane.ERROR_MESSAGE);
    }
}
