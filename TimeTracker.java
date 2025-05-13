import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class TimeTracker extends JFrame implements ActionListener {
    // UI components
    private JButton startButton, pauseButton, stopButton, adjustButton, addDayButton, deleteDayButton;
    private JLabel sessionLabel;
    private JLabel totalLabel;
    private JTable table;
    private DefaultTableModel tableModel;
    private Timer timer;

    // Layout panels and scroll pane
    private JPanel topPanel;
    private JPanel labelPanel;
    private JScrollPane scrollPane;

    // Time tracking variables
    private boolean isRunning = false;
    private boolean isPaused = false;
    private long sessionStartTime = 0;
    private long sessionElapsedTime = 0;

    // Map for daily tracked time (date -> milliseconds)
    private Map<LocalDate, Long> dailyTimes = new HashMap<>();

    // Controls whether seconds should be shown in the table (default: only hh:mm)
    private boolean showSecondsInTable = false;
    // Formatter for displaying the date in "dd.MM.yyyy" format
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // File path where data will be saved
    private final String DATA_FILE = "timetracker_data.txt";

    public TimeTracker() {
        super("Time Tracker - Programming");
        initComponents();
        loadData();
        updateTable();
        updateTotalLabel();

        // Save data when the window is closing
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveData();
            }
        });
    }

    /**
     * Initializes all GUI components, adds the buttons, and starts the timer.
     */
    private void initComponents() {
        // --- Menu bar (Dark Mode, toggle to show seconds) ---
        JMenuBar menuBar = new JMenuBar();
        JMenu viewMenu = new JMenu("View");
        // Toggle to enable showing seconds in the time display
        JCheckBoxMenuItem secondsToggle = new JCheckBoxMenuItem("Show seconds in time display");
        secondsToggle.addActionListener(e -> {
            showSecondsInTable = secondsToggle.isSelected();
            updateTableHeader();
            updateTable();
        });
        viewMenu.add(secondsToggle);
        menuBar.add(viewMenu);
        setJMenuBar(menuBar);

        // --- Create buttons ---
        startButton = new JButton("Start");
        pauseButton = new JButton("Pause");
        stopButton = new JButton("Stop");
        adjustButton = new JButton("Adjust time");
        addDayButton = new JButton("Add time");
        deleteDayButton = new JButton("Delete day");

        // --- Button styling (Dark Mode) ---
        Color primaryButtonColor = new Color(70, 130, 180);
        Color stopButtonColor = new Color(180, 70, 70);
        Color deleteButtonColor = new Color(200, 50, 50);

        startButton.setBackground(primaryButtonColor);
        startButton.setForeground(Color.WHITE);
        pauseButton.setBackground(primaryButtonColor);
        pauseButton.setForeground(Color.WHITE);
        stopButton.setBackground(stopButtonColor);
        stopButton.setForeground(Color.WHITE);
        adjustButton.setBackground(primaryButtonColor);
        adjustButton.setForeground(Color.WHITE);
        addDayButton.setBackground(primaryButtonColor);
        addDayButton.setForeground(Color.WHITE);
        deleteDayButton.setBackground(deleteButtonColor);
        deleteDayButton.setForeground(Color.WHITE);

        // Disable focus highlights
        startButton.setFocusPainted(false);
        pauseButton.setFocusPainted(false);
        stopButton.setFocusPainted(false);
        adjustButton.setFocusPainted(false);
        addDayButton.setFocusPainted(false);
        deleteDayButton.setFocusPainted(false);

        // Attach ActionListeners
        startButton.addActionListener(this);
        pauseButton.addActionListener(this);
        stopButton.addActionListener(this);
        adjustButton.addActionListener(this);
        addDayButton.addActionListener(this);
        deleteDayButton.addActionListener(this);

        // --- Labels to show time ---
        sessionLabel = new JLabel("Current session: 00:00:00");
        totalLabel = new JLabel("Total time: 00:00:00");
        sessionLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        // --- Table to show daily tracked time ---
        tableModel = new DefaultTableModel(new Object[]{"Date", "Tracked time"}, 0);
        table = new JTable(tableModel);
        // Allow manual column resizing
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Layout panels ---
        topPanel = new JPanel();
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        topPanel.add(startButton);
        topPanel.add(pauseButton);
        topPanel.add(stopButton);
        topPanel.add(adjustButton);
        topPanel.add(addDayButton);
        topPanel.add(deleteDayButton);

        labelPanel = new JPanel(new GridLayout(2, 1));
        labelPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        labelPanel.add(sessionLabel);
        labelPanel.add(totalLabel);

        // Combine buttons & labels into upper panel
        JPanel upperPanel = new JPanel(new BorderLayout());
        upperPanel.add(topPanel, BorderLayout.NORTH);
        upperPanel.add(labelPanel, BorderLayout.CENTER);

        // --- JSplitPane to separate top and bottom areas ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upperPanel, scrollPane);
        splitPane.setResizeWeight(0.3); // 30% top, 70% bottom

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splitPane, BorderLayout.CENTER);

        // Timer updates the current session every second
        timer = new Timer(1000, e -> updateSessionTime());

        // Apply dark theme to all components
        applyDarkTheme();

        // Window settings
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null); // Center the window
        setResizable(true);
        setVisible(true);
    }

    /**
     * Applies a full dark mode theme to all components.
     */
    private void applyDarkTheme() {
        // Background colors
        getContentPane().setBackground(new Color(35, 35, 35));
        topPanel.setBackground(new Color(35, 35, 35));
        labelPanel.setBackground(new Color(55, 55, 55));

        // Labels in white
        sessionLabel.setForeground(Color.WHITE);
        totalLabel.setForeground(Color.WHITE);

        // Table background and viewport
        table.setBackground(new Color(60, 60, 60));
        table.setForeground(Color.WHITE);
        scrollPane.getViewport().setBackground(new Color(60, 60, 60));

        // Table header: lighter background, black text for contrast
        table.getTableHeader().setBackground(new Color(120, 120, 120));
        table.getTableHeader().setForeground(Color.BLACK);
    }

    /**
     * Updates the display of the current session.
     */
    private void updateSessionTime() {
        if (isRunning) {
            long currentTime = System.currentTimeMillis();
            long currentSessionTime = sessionElapsedTime + (currentTime - sessionStartTime);
            sessionLabel.setText("Current session: " + formatTime(currentSessionTime));
        }
    }

    /**
     * Formats the given time (in milliseconds) into hh:mm:ss.
     * (Used for the current session display.)
     */
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long hh = seconds / 3600;
        long mm = (seconds % 3600) / 60;
        long ss = seconds % 60;
        return String.format("%02d:%02d:%02d", hh, mm, ss);
    }

    /**
     * Formats time for the table display.
     * Depending on the setting (showSecondsInTable), shows hh:mm or hh:mm:ss.
     */
    private String formatTimeTable(long millis) {
        long seconds = millis / 1000;
        long hh = seconds / 3600;
        long mm = (seconds % 3600) / 60;
        long ss = seconds % 60;
        if (showSecondsInTable) {
            return String.format("%02d:%02d:%02d", hh, mm, ss);
        } else {
            return String.format("%02d:%02d", hh, mm);
        }
    }

    /**
     * Updates the table header to indicate whether seconds are shown.
     */
    private void updateTableHeader() {
        String headerTime = showSecondsInTable ? "Tracked time (hh:mm:ss)" : "Tracked time (hh:mm)";
        table.getColumnModel().getColumn(1).setHeaderValue(headerTime);
        table.getTableHeader().repaint();
    }

    /**
     * Handles button click events.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            handleStart();
        } else if (e.getSource() == pauseButton) {
            handlePause();
        } else if (e.getSource() == stopButton) {
            handleStop();
        } else if (e.getSource() == adjustButton) {
            handleTimeAdjustment();
        } else if (e.getSource() == addDayButton) {
            handleAddDay();
        } else if (e.getSource() == deleteDayButton) {
            handleDeleteDay();
        }
    }

    /**
     * Starts or resumes a paused session.
     */
    private void handleStart() {
        if (!isRunning) {
            sessionStartTime = System.currentTimeMillis();
            isRunning = true;
            isPaused = false;
            timer.start();
        }
    }

    /**
     * Pauses the running session and saves elapsed time.
     */
    private void handlePause() {
        if (isRunning) {
            long currentTime = System.currentTimeMillis();
            sessionElapsedTime += (currentTime - sessionStartTime);
            isRunning = false;
            isPaused = true;
            timer.stop();
            sessionLabel.setText("Current session: " + formatTime(sessionElapsedTime));
        }
    }

    /**
     * Stops the current session, saves the time for today,
     * and updates the overview.
     */
    private void handleStop() {
        long totalSessionTime = sessionElapsedTime;
        if (isRunning) {
            totalSessionTime += (System.currentTimeMillis() - sessionStartTime);
        }
        LocalDate today = LocalDate.now();
        dailyTimes.put(today, dailyTimes.getOrDefault(today, 0L) + totalSessionTime);
        isRunning = false;
        isPaused = false;
        sessionElapsedTime = 0;
        timer.stop();
        sessionLabel.setText("Current session: 00:00:00");

        updateTable();
        updateTotalLabel();
    }

    /**
     * Allows manual adjustment of the tracked time for a selected day.
     */
    private void handleTimeAdjustment() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a day from the table to adjust the time.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String dateStr = tableModel.getValueAt(selectedRow, 0).toString();
        LocalDate date = LocalDate.parse(dateStr, dateFormatter);
        String newTimeStr = JOptionPane.showInputDialog(this,
                "New time value for " + dateStr + " (format HH:mm:ss):",
                tableModel.getValueAt(selectedRow, 1).toString());
        if (newTimeStr == null || newTimeStr.trim().isEmpty()) {
            return;
        }
        String[] parts = newTimeStr.split(":");
        if (parts.length != 3) {
            JOptionPane.showMessageDialog(this,
                    "Invalid format. Please use HH:mm:ss.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            long hh = Long.parseLong(parts[0]);
            long mm = Long.parseLong(parts[1]);
            long ss = Long.parseLong(parts[2]);
            long newMillis = (hh * 3600 + mm * 60 + ss) * 1000;
            dailyTimes.put(date, newMillis);
            updateTable();
            updateTotalLabel();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error parsing time. Please ensure valid numbers.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Allows adding time for any date.
     * If the date already exists, the new time is added.
     */
    private void handleAddDay() {
        String dateInput = JOptionPane.showInputDialog(this,
                "Enter date (format dd.MM.yyyy):",
                "Add date", JOptionPane.QUESTION_MESSAGE);
        if (dateInput == null || dateInput.trim().isEmpty()) {
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateInput, dateFormatter);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid date. Please use format dd.MM.yyyy.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String timeInput = JOptionPane.showInputDialog(this,
                "Add time (format HH:mm:ss):",
                "00:00:00");
        if (timeInput == null || timeInput.trim().isEmpty()) {
            return;
        }
        String[] parts = timeInput.split(":");
        if (parts.length != 3) {
            JOptionPane.showMessageDialog(this,
                    "Invalid time format. Please use HH:mm:ss.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            long hh = Long.parseLong(parts[0]);
            long mm = Long.parseLong(parts[1]);
            long ss = Long.parseLong(parts[2]);
            long addedMillis = (hh * 3600 + mm * 60 + ss) * 1000;
            long currentMillis = dailyTimes.getOrDefault(date, 0L);
            dailyTimes.put(date, currentMillis + addedMillis);
            updateTable();
            updateTotalLabel();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not read time. Please enter valid numbers.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Deletes the selected day from the table.
     */
    private void handleDeleteDay() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Please select a day in the table you want to delete.",
                    "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String dateStr = tableModel.getValueAt(selectedRow, 0).toString();
        LocalDate date = LocalDate.parse(dateStr, dateFormatter);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete " + dateStr + "?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dailyTimes.remove(date);
            updateTable();
            updateTotalLabel();
        }
    }

    /**
     * Updates the table with daily tracked times.
     * Dates are shown in "dd.MM.yyyy" and times according to the setting.
     */
    private void updateTable() {
        tableModel.setRowCount(0);
        for (Map.Entry<LocalDate, Long> entry : dailyTimes.entrySet()) {
            String dateStr = dateFormatter.format(entry.getKey());
            String timeStr = formatTimeTable(entry.getValue());
            tableModel.addRow(new Object[]{dateStr, timeStr});
        }
    }

    /**
     * Updates the label showing the total tracked time.
     */
    private void updateTotalLabel() {
        long total = 0;
        for (Long time : dailyTimes.values()) {
            total += time;
        }
        totalLabel.setText("Total time: " + formatTime(total));
    }

    /**
     * Saves the daily tracked times to a file.
     */
    private void saveData() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DATA_FILE))) {
            for (Map.Entry<LocalDate, Long> entry : dailyTimes.entrySet()) {
                writer.write(dateFormatter.format(entry.getKey()) + ";" + entry.getValue());
                writer.newLine();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Loads the daily tracked times from a file.
     */
    private void loadData() {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length == 2) {
                    LocalDate date = LocalDate.parse(parts[0], dateFormatter);
                    long millis = Long.parseLong(parts[1]);
                    dailyTimes.put(date, millis);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        try {
            // Set Nimbus Look and Feel, if available (for consistent appearance)
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // Ignore and use default
        }
        SwingUtilities.invokeLater(() -> new TimeTracker());
    }
}
