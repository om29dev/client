package com.mcq.client.panels;

import com.mcq.client.Main;
import com.mcq.client.lib.ApiClient;
import com.mcq.client.lib.Models.Test;

import javax.swing.*;
import java.awt.*;

public class StudentViewPanel extends JPanel {

    private Main mainFrame;
    private ApiClient apiClient;

    public StudentViewPanel(Main mainFrame) {
        this.mainFrame = mainFrame;
        this.apiClient = ApiClient.getInstance();
        setLayout(new BorderLayout());
        setBackground(new Color(248, 250, 252));

        JLabel loadingLabel = new JLabel("Checking for active test...");
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingLabel.setFont(new Font("SansSerif", Font.PLAIN, 18));
        add(loadingLabel, BorderLayout.CENTER);

        checkForActiveTest();
    }

    private void checkForActiveTest() {
        new SwingWorker<Test, Void>() {
            @Override
            protected Test doInBackground() throws Exception {
                return apiClient.getActiveTestForStudent();
            }

            @Override
            protected void done() {
                removeAll();
                try {
                    Test activeTest = get();
                    if (activeTest != null) {
                        showTestInterstitial(activeTest);
                    } else {
                        showStudentDashboard();
                    }
                } catch (Exception e) {
                    String causeMessage = (e.getCause() != null) ? e.getCause().getMessage() : null;

                    if (causeMessage != null && causeMessage.contains("204")) {
                        showStudentDashboard();
                    } else {
                        e.printStackTrace();
                        add(new JLabel("Error checking for test: " + e.getMessage()), BorderLayout.CENTER);
                    }
                }
                revalidate();
                repaint();
            }
        }.execute();
    }

    private void showStudentDashboard() {
        StudentDashboardPanel dashboard = new StudentDashboardPanel(mainFrame);
        add(dashboard, BorderLayout.CENTER);
    }

    private void showTestInterstitial(Test activeTest) {
        setLayout(new GridBagLayout());
        setBackground(new Color(240, 245, 255));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 1;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridy = 0;

        JLabel title = new JLabel("Test Ready");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        panel.add(title, gbc);

        gbc.gridy++;
        JLabel subtitle = new JLabel("You are about to start the test: " + activeTest.testname());
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
        panel.add(subtitle, gbc);

        gbc.gridy++;
        JTextArea warning = new JTextArea("This test will launch in fullscreen mode.\nExiting fullscreen multiple times will lock your test.");
        warning.setFont(new Font("SansSerif", Font.PLAIN, 14));
        warning.setBackground(new Color(255, 253, 235));
        warning.setForeground(new Color(185, 137, 0));
        warning.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        warning.setEditable(false);
        panel.add(warning, gbc);

        gbc.gridy++;
        JButton startButton = new JButton("Start Test");
        startButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        startButton.setBackground(new Color(37, 99, 235));
        startButton.setForeground(Color.WHITE);
        panel.add(startButton, gbc);

        startButton.addActionListener(e -> {
            mainFrame.showTestViewer(activeTest.classroom().code(), activeTest.testname());
        });

        add(panel);
    }
}