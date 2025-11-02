// src/main/java/com/mcq/client/Main.java
package com.mcq.client;

import com.mcq.client.lib.AuthService;
import com.mcq.client.panels.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
// import java.awt.event.WindowFocusListener; // <-- No longer needed

public class Main extends JFrame {

    private CardLayout cardLayout;
    private JPanel mainPanel;
    private LoginPanel loginPanel;
    private RegisterPanel registerPanel;
    private StudentViewPanel studentViewPanel;
    private TeacherDashboardPanel teacherDashboardPanel;
    private ClassroomDetailPanel classroomDetailPanel;
    private TestResultsPanel testResultsPanel;
    private TestViewerPanel testViewerPanel;

    private GraphicsDevice gd; // <-- MODIFIED: Promoted to class field

    public Main() {
        setTitle("MCQ Test Platform");

        // --- MODIFICATIONS FOR FULLSCREEN ---
        setUndecorated(true); // Remove window borders and title bar
        setAlwaysOnTop(true);

        // --- MODIFIED: Initialize class field 'gd' ---
        gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            gd.setFullScreenWindow(this); // Set to full-screen
        } else {
            System.err.println("Full-screen mode not supported.");
            setSize(getToolkit().getScreenSize()); // Fallback to maximized
        }
        // --- END MODIFICATIONS ---

        setMinimumSize(new Dimension(1024, 768));

        // This prevents Alt+F4 or other system-level close commands
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        // --- KIOSK MODIFICATIONS ---
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                // --- THIS IS THE FIX ---
                // When minimized (e.g., Win+D), the OS exits full-screen.
                // We must re-assert it.
                // We use invokeLater to let the minimize animation finish,
                // which prevents the "flicker" and "short" window.
                SwingUtilities.invokeLater(() -> {
                    if (gd != null && gd.isFullScreenSupported()) {
                        gd.setFullScreenWindow(Main.this); // Re-enter full-screen
                    } else {
                        setState(Frame.NORMAL); // Fallback
                        setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                    toFront();
                    requestFocus();
                });
            }
        });
        // --- END KIOSK MODIFICATIONS ---

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
                if (testViewerPanel != null) {
                    testViewerPanel.stopPolling();
                    mainPanel.remove(testViewerPanel);
                }

                studentViewPanel = null;
                teacherDashboardPanel = null;
                classroomDetailPanel = null;
                testResultsPanel = null;
                testViewerPanel = null;

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
        // The secure test environment is now just another panel in the main CardLayout
        if (testViewerPanel != null) {
            testViewerPanel.stopPolling();
            mainPanel.remove(testViewerPanel);
        }
        // Pass 'this' (the Main frame) to the panel's constructor
        testViewerPanel = new TestViewerPanel(this, classroomCode, testname);
        mainPanel.add(testViewerPanel, "TEST_VIEWER");
        cardLayout.show(mainPanel, "TEST_VIEWER");
    }

    public void showTestResults(String classroomCode, String testname) {
        if (testResultsPanel != null) {
            mainPanel.remove(testResultsPanel);
        }
        testResultsPanel = new TestResultsPanel(this, classroomCode, testname);
        mainPanel.add(testResultsPanel, "TEST_RESULTS");
        cardLayout.show(mainPanel, "TEST_RESULTS");
    }

    // Called by TestViewerPanel when it closes
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