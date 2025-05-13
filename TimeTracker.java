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
    // UI-Komponenten
    private JButton startButton, pauseButton, stopButton, adjustButton, addDayButton, deleteDayButton;
    private JLabel sessionLabel;
    private JLabel totalLabel;
    private JTable table;
    private DefaultTableModel tableModel;
    private Timer timer;

    // Layout Panels und ScrollPane
    private JPanel topPanel;
    private JPanel labelPanel;
    private JScrollPane scrollPane;

    // Variablen zur Zeiterfassung
    private boolean isRunning = false;
    private boolean isPaused = false;
    private long sessionStartTime = 0;
    private long sessionElapsedTime = 0;

    // Map für täglich investierte Zeiten (Datum -> Zeit in Millisekunden)
    private Map<LocalDate, Long> dailyTimes = new HashMap<>();

    // Steuerung der Zeitanzeige in der Tabelle (Standard: nur hh:mm)
    private boolean showSecondsInTable = false;
    // Formatter für Datum im Format "dd.MM.yyyy"
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    // Dateipfad, in dem die Daten gespeichert werden
    private final String DATA_FILE = "timetracker_data.txt";

    public TimeTracker() {
        super("Time Tracker - Programmieren");
        initComponents();
        loadData();
        updateTable();
        updateTotalLabel();

        // Daten speichern beim Schließen des Fensters
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveData();
            }
        });
    }

    /**
     * Initialisiert alle GUI-Komponenten, fügt die Buttons hinzu und startet den Timer.
     */
    private void initComponents() {
        // --- Menüleiste (Dark Mode, nur Toggle für Zeitanzeige mit Sekunden) ---
        JMenuBar menuBar = new JMenuBar();
        JMenu viewMenu = new JMenu("Ansicht");
        // Toggle zur Auswahl, ob in der Tabelle auch Sekunden angezeigt werden sollen
        JCheckBoxMenuItem secondsToggle = new JCheckBoxMenuItem("Zeitanzeige mit Sekunden");
        secondsToggle.addActionListener(e -> {
            showSecondsInTable = secondsToggle.isSelected();
            updateTableHeader();
            updateTable();
        });
        viewMenu.add(secondsToggle);
        menuBar.add(viewMenu);
        setJMenuBar(menuBar);

        // --- Buttons erstellen ---
        startButton = new JButton("Start");
        pauseButton = new JButton("Pause");
        stopButton = new JButton("Stop");
        adjustButton = new JButton("Zeit anpassen");
        addDayButton = new JButton("Zeit hinzufügen");
        deleteDayButton = new JButton("Tag löschen");

        // --- Button-Styling (Dark Mode) ---
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

        // Fokus-Hervorhebungen deaktivieren
        startButton.setFocusPainted(false);
        pauseButton.setFocusPainted(false);
        stopButton.setFocusPainted(false);
        adjustButton.setFocusPainted(false);
        addDayButton.setFocusPainted(false);
        deleteDayButton.setFocusPainted(false);

        // ActionListener anhängen
        startButton.addActionListener(this);
        pauseButton.addActionListener(this);
        stopButton.addActionListener(this);
        adjustButton.addActionListener(this);
        addDayButton.addActionListener(this);
        deleteDayButton.addActionListener(this);

        // --- Labels für die Zeitanzeige ---
        sessionLabel = new JLabel("Aktuelle Sitzung: 00:00:00");
        totalLabel = new JLabel("Gesamte Zeit: 00:00:00");
        sessionLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        // --- Tabelle zur Darstellung der täglich investierten Zeiten ---
        tableModel = new DefaultTableModel(new Object[]{"Datum", "Investierte Zeit"}, 0);
        table = new JTable(tableModel);
        // Erlaubt manuelles Anpassen der Spaltenbreiten
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        scrollPane = new JScrollPane(table);
        scrollPane.setBorder(new EmptyBorder(10, 10, 10, 10));

        // --- Panels (Layout) ---
        topPanel = new JPanel();
        topPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        // Buttons im oberen Panel hinzufügen
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

        // Oberen Bereich (Buttons & Labels) in einem Panel zusammenfassen
        JPanel upperPanel = new JPanel(new BorderLayout());
        upperPanel.add(topPanel, BorderLayout.NORTH);
        upperPanel.add(labelPanel, BorderLayout.CENTER);

        // --- JSplitPane einfügen, um obere und untere Bereiche flexibel anzupassen ---
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, upperPanel, scrollPane);
        splitPane.setResizeWeight(0.3); // Obere 30 % und untere 70 % als Startverhältnis

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(splitPane, BorderLayout.CENTER);

        // --- Timer, der jede Sekunde die aktuelle Sitzung aktualisiert ---
        timer = new Timer(1000, e -> updateSessionTime());

        // --- Dark Mode anwenden (kompletter Stil) ---
        applyDarkTheme();

        // --- Fenster-Einstellungen ---
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 500);
        setLocationRelativeTo(null); // Zentriert das Fenster
        setResizable(true);
        setVisible(true);
    }

    /**
     * Wendet ein vollständiges Dark Mode Theme auf alle Komponenten an.
     */
    private void applyDarkTheme() {
        // Hintergrundfarben
        getContentPane().setBackground(new Color(35, 35, 35));
        topPanel.setBackground(new Color(35, 35, 35));
        labelPanel.setBackground(new Color(55, 55, 55));

        // Labels in Weiß
        sessionLabel.setForeground(Color.WHITE);
        totalLabel.setForeground(Color.WHITE);

        // Tabelle: Hintergrund und Scrollbereich
        table.setBackground(new Color(60, 60, 60));
        table.setForeground(Color.WHITE);
        scrollPane.getViewport().setBackground(new Color(60, 60, 60));

        // Table Header: Heller Hintergrund, schwarze Schrift für guten Kontrast
        table.getTableHeader().setBackground(new Color(120, 120, 120));
        table.getTableHeader().setForeground(Color.BLACK);
    }

    /**
     * Aktualisiert die Anzeige der aktuellen Sitzung.
     */
    private void updateSessionTime() {
        if (isRunning) {
            long currentTime = System.currentTimeMillis();
            long currentSessionTime = sessionElapsedTime + (currentTime - sessionStartTime);
            sessionLabel.setText("Aktuelle Sitzung: " + formatTime(currentSessionTime));
        }
    }

    /**
     * Formatiert die übergebene Zeit (Millisekunden) in das Format hh:mm:ss.
     * (Wird für die Anzeige der aktuellen Sitzung genutzt.)
     */
    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long hh = seconds / 3600;
        long mm = (seconds % 3600) / 60;
        long ss = seconds % 60;
        return String.format("%02d:%02d:%02d", hh, mm, ss);
    }

    /**
     * Formatiert die Zeit für die Anzeige in der Tabelle.
     * Je nach Einstellung (showSecondsInTable) werden entweder nur Stunden und Minuten
     * oder auch Sekunden angezeigt.
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
     * Aktualisiert den Table Header, sodass in der Zeit-Spalte angezeigt wird,
     * ob Sekunden mit angezeigt werden.
     */
    private void updateTableHeader() {
        String headerTime = showSecondsInTable ? "Investierte Zeit (hh:mm:ss)" : "Investierte Zeit (hh:mm)";
        table.getColumnModel().getColumn(1).setHeaderValue(headerTime);
        table.getTableHeader().repaint();
    }

    /**
     * Behandelt alle Button-Events.
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
     * Startet bzw. setzt eine pausierte Sitzung fort.
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
     * Pausiert die laufende Sitzung und speichert die bisher verstrichene Zeit.
     */
    private void handlePause() {
        if (isRunning) {
            long currentTime = System.currentTimeMillis();
            sessionElapsedTime += (currentTime - sessionStartTime);
            isRunning = false;
            isPaused = true;
            timer.stop();
            sessionLabel.setText("Aktuelle Sitzung: " + formatTime(sessionElapsedTime));
        }
    }

    /**
     * Beendet die aktuelle Sitzung, speichert die Zeit für das heutige Datum
     * und aktualisiert die Übersicht.
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
        sessionLabel.setText("Aktuelle Sitzung: 00:00:00");

        updateTable();
        updateTotalLabel();
    }

    /**
     * Ermöglicht das manuelle Anpassen der investierten Zeit eines ausgewählten Tages.
     */
    private void handleTimeAdjustment() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Bitte wählen Sie einen Tag aus der Tabelle aus, um die Zeit anzupassen.",
                    "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String dateStr = tableModel.getValueAt(selectedRow, 0).toString();
        // Datum aus dem String im Format "dd.MM.yyyy" parsen:
        LocalDate date = LocalDate.parse(dateStr, dateFormatter);
        String newTimeStr = JOptionPane.showInputDialog(this,
                "Neuer Zeitwert für " + dateStr + " (Format HH:mm:ss):",
                tableModel.getValueAt(selectedRow, 1).toString());
        if (newTimeStr == null || newTimeStr.trim().isEmpty()) {
            return;
        }
        String[] parts = newTimeStr.split(":");
        if (parts.length != 3) {
            JOptionPane.showMessageDialog(this,
                    "Ungültiges Format. Bitte verwenden Sie HH:mm:ss.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
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
                    "Fehler beim Parsen des Zeitwerts. Bitte stellen Sie sicher, dass Sie gültige Zahlen eingeben.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Ermöglicht das Hinzufügen einer Zeit für ein beliebiges Datum.
     * Falls bereits eine Zeit für das Datum existiert, wird die neue Zeit addiert.
     */
    private void handleAddDay() {
        String dateInput = JOptionPane.showInputDialog(this,
                "Datum eingeben (Format dd.MM.yyyy):",
                "Datum hinzufügen", JOptionPane.QUESTION_MESSAGE);
        if (dateInput == null || dateInput.trim().isEmpty()) {
            return;
        }
        LocalDate date;
        try {
            date = LocalDate.parse(dateInput, dateFormatter);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Ungültiges Datum. Bitte verwenden Sie das Format dd.MM.yyyy.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String timeInput = JOptionPane.showInputDialog(this,
                "Zeit hinzufügen (Format HH:mm:ss):",
                "00:00:00");
        if (timeInput == null || timeInput.trim().isEmpty()) {
            return;
        }
        String[] parts = timeInput.split(":");
        if (parts.length != 3) {
            JOptionPane.showMessageDialog(this,
                    "Ungültiges Zeitformat. Bitte verwenden Sie HH:mm:ss.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
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
                    "Zeit konnte nicht gelesen werden. Bitte geben Sie gültige Zahlen ein.",
                    "Fehler", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Löscht den in der Tabelle ausgewählten Tag.
     */
    private void handleDeleteDay() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this,
                    "Bitte wählen Sie einen Tag aus der Tabelle, den Sie löschen möchten.",
                    "Keine Auswahl", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String dateStr = tableModel.getValueAt(selectedRow, 0).toString();
        LocalDate date = LocalDate.parse(dateStr, dateFormatter);
        int confirm = JOptionPane.showConfirmDialog(this,
                "Soll der Tag " + dateStr + " wirklich gelöscht werden?",
                "Löschen bestätigen", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            dailyTimes.remove(date);
            updateTable();
            updateTotalLabel();
        }
    }

    /**
     * Aktualisiert die Tabelle mit den täglich investierten Zeiten.
     * Das Datum wird im Format "dd.MM.yyyy" und die Zeit gemäß der Einstellung (hh:mm oder hh:mm:ss) angezeigt.
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
     * Aktualisiert das Label für die Gesamtdauer.
     */
    private void updateTotalLabel() {
        long total = 0;
        for (Long time : dailyTimes.values()) {
            total += time;
        }
        totalLabel.setText("Gesamte Zeit: " + formatTime(total));
    }

    /**
     * Speichert die täglich investierten Zeiten in einer Datei.
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
     * Lädt die täglich investierten Zeiten aus einer Datei.
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
            // Setze Nimbus Look and Feel, falls verfügbar (für ein konsistentes Erscheinungsbild)
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) { }
        SwingUtilities.invokeLater(() -> new TimeTracker());
    }
}