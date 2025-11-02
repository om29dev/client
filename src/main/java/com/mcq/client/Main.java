// src/main/java/com/mcq/client/Main.java
package com.mcq.client;

import com.mcq.client.lib.AuthService;
import com.mcq.client.panels.*;

import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private LoginPanel loginPanel;
    private RegisterPanel registerPanel;
    private StudentViewPanel studentViewPanel;
    private TeacherDashboardPanel teacherDashboardPanel;
    private ClassroomDetailPanel classroomDetailPanel;
    private TestResultsPanel testResultsPanel;

    // A separate frame for the secure test environment
    private SecureTestFrame secureTestFrame;

    public Main() {
        setTitle("MCQ Test Platform");

        // --- MODIFICATIONS FOR FULLSCREEN ---
        setUndecorated(true); // Remove window borders and title bar
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            gd.setFullScreenWindow(this); // Set to full-screen
        } else {
            System.err.println("Full-screen mode not supported.");
            setSize(getToolkit().getScreenSize()); // Fallback to maximized
        }
        // --- END MODIFICATIONS ---

        setMinimumSize(new Dimension(1024, 768));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Create panels
        loginPanel = new LoginPanel(this);
        registerPanel = new RegisterPanel(this);

        // Add panels to the main card layout
        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(registerPanel, "REGISTER");
        // Dashboard panels will be created and added when user logs in

        add(mainPanel);

        // Listen to auth changes
        AuthService.getInstance().addPropertyChangeListener(evt -> {
            if ("user".equals(evt.getPropertyName())) {
                // Clear old panels on auth change
                if (studentViewPanel != null) mainPanel.remove(studentViewPanel);
                if (teacherDashboardPanel != null) mainPanel.remove(teacherDashboardPanel);
                if (classroomDetailPanel != null) mainPanel.remove(classroomDetailPanel);
                if (testResultsPanel != null) mainPanel.remove(testResultsPanel);

                studentViewPanel = null;
                teacherDashboardPanel = null;
                classroomDetailPanel = null;
                testResultsPanel = null;

                if (evt.getNewValue() != null) {
                    showDashboard();
                } else {
                    showLogin();
                }
            }
        });

        // Show initial panel
        showLogin();
    }

    public void showLogin() {
        cardLayout.show(mainPanel, "LOGIN");
    }

    public void showRegister() {
        cardLayout.show(mainPanel, "REGISTER");
    }

    public void showDashboard() {
        var user = AuthService.getInstance().getUser();
        if (user == null) {
            showLogin();
            return;
        }

        if (user.role().equals("ROLE_TEACHER") || user.role().equals("ROLE_ADMIN")) {
            if (teacherDashboardPanel == null) {
                teacherDashboardPanel = new TeacherDashboardPanel(this);
                mainPanel.add(teacherDashboardPanel, "TEACHER_DASHBOARD");
            }
            cardLayout.show(mainPanel, "TEACHER_DASHBOARD");
        } else {
            // Student dashboard logic is more complex due to active test checking
            studentViewPanel = new StudentViewPanel(this);
            mainPanel.add(studentViewPanel, "STUDENT_VIEW");
            cardLayout.show(mainPanel, "STUDENT_VIEW");
        }
    }

    public void showClassroomDetail(String classroomCode) {
        if (classroomDetailPanel != null) {
            mainPanel.remove(classroomDetailPanel);
        }
        classroomDetailPanel = new ClassroomDetailPanel(this, classroomCode);
        mainPanel.add(classroomDetailPanel, "CLASSROOM_DETAIL");
        cardLayout.show(mainPanel, "CLASSROOM_DETAIL");
    }

    public void showTestViewer(String classroomCode, String testname) {
        // The secure test environment is a separate, undecorated, fullscreen window
        if (secureTestFrame != null && secureTestFrame.isVisible()) {
            secureTestFrame.dispose();
        }
        this.setVisible(false); // Hide the main window
        secureTestFrame = new SecureTestFrame(this, classroomCode, testname);
        secureTestFrame.setVisible(true);
    }

    public void showTestResults(String classroomCode, String testname) {
        if (testResultsPanel != null) {
            mainPanel.remove(testResultsPanel);
        }
        testResultsPanel = new TestResultsPanel(this, classroomCode, testname);
        mainPanel.add(testResultsPanel, "TEST_RESULTS");
        cardLayout.show(mainPanel, "TEST_RESULTS");
    }

    // Called by SecureTestFrame when it closes
    public void returnToMainWindow() {
        this.setVisible(true);
        this.toFront();
        this.requestFocus();
        // Refresh dashboard to show test is over
        showDashboard();
    }

    public static void main(String[] args) {
        // Set a modern Look and Feel (Nimbus)
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, fall back to default
        }

        // Set global font styles to match the web app
        UIManager.put("Label.font", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("Button.font", new Font("SansSerif", Font.BOLD, 14));
        UIManager.put("TextField.font", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("PasswordField.font", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("ComboBox.font", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("Table.font", new Font("SansSerif", Font.PLAIN, 14));
        UIManager.put("TableHeader.font", new Font("SansSerif", Font.BOLD, 12));

        SwingUtilities.invokeLater(() -> new Main().setVisible(true));
    }
}