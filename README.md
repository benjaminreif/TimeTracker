# 🕒 Time Tracker - Programming

This Java-based Time Tracker is a simple desktop application for tracking and analyzing the time you spend on programming (or any other activity). It's built using Java Swing and stores daily tracked times locally in a file.

## 📌 Features

- Start, pause, and stop session time tracking
- Automatically logs daily programming time
- Add or adjust time manually for any date
- Delete specific entries
- Toggle between displaying time in `hh:mm` or `hh:mm:ss`
- Clean and responsive GUI with a dark mode theme
- Saves your data automatically when the application is closed

## 🛠 Tech Stack

- **Java** (Standard Edition)
- **Swing** for GUI
- No external libraries required

## 💾 Data Storage

Tracked time is saved in a local file named `timetracker_data.txt` located in the same directory as the application. Each line contains: dd.MM.yyyy;milliseconds

## 🚀 Getting Started

### Run Locally

1. Clone or download the repository
2. Open the project in your preferred Java IDE
3. Run `TimeTracker.java`

Alternatively, compile and run from terminal:

```bash
javac TimeTracker.java
java TimeTracker
```

## 📂 File Structure

/TimeTracker
├── src/
│ └── TimeTracker.java # Main application source code
├── timetracker_data.txt # Generated automatically to save daily time
├── README.md # Project documentation
└── LICENSE # License file (MIT by default)

## 🙋‍♂️ Credits

This project was created with the help of online resources and guidance from ChatGPT, as part of my learning journey in Java.

## 📄 License

This project is licensed under the MIT License – see the [LICENSE](LICENSE) file for details.
