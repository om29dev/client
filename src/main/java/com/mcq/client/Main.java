package com.mcq.client;

import com.mcq.client.lib.AuthService;
import com.mcq.client.panels.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

    private GraphicsDevice gd;

    public Main() {
        setTitle("MCQ Test Platform");

        setUndecorated(true);
        setAlwaysOnTop(true);

        gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        if (gd.isFullScreenSupported()) {
            gd.setFullScreenWindow(this);
        } else {
            System.err.println("Full-screen mode not supported.");
            setSize(getToolkit().getScreenSize());
        }

        setMinimumSize(new Dimension(1024, 768));

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowIconified(WindowEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (gd != null && gd.isFullScreenSupported()) {
                        gd.setFullScreenWindow(Main.this);
                    } else {
                        setState(Frame.NORMAL);
                        setExtendedState(JFrame.MAXIMIZED_BOTH);
                    }
                    toFront();
                    requestFocus();
                });
            }
        });

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        loginPanel = new LoginPanel(this);
        registerPanel = new RegisterPanel(this);

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(registerPanel, "REGISTER");

        add(mainPanel);

        AuthService.getInstance().addPropertyChangeListener(evt -> {
            if ("user".equals(evt.getPropertyName())) {
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
        if (testViewerPanel != null) {
            testViewerPanel.stopPolling();
            mainPanel.remove(testViewerPanel);
        }
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

    public void returnToMainWindow() {
        this.setVisible(true);
        this.toFront();
        this.requestFocus();
        showDashboard();
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
        }

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