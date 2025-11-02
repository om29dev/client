// src/main/java/com/mcq/client/panels/SecureTestFrame.java
package com.mcq.client.panels;

import com.mcq.client.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;

public class SecureTestFrame extends JFrame {

    private Main mainFrame;
    private TestViewerPanel testViewerPanel;
    private int warningCount = 0;
    private final int MAX_WARNINGS = 3;
    private boolean isLockedOut = false;
    private boolean isPaused = false;
    private GraphicsDevice gd;

    public SecureTestFrame(Main mainFrame, String classroomCode, String testname) {
        this.mainFrame = mainFrame;
        this.testViewerPanel = new TestViewerPanel(this, classroomCode, testname);

        setUndecorated(true);
        add(testViewerPanel);

        gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

        if (gd.isFullScreenSupported()) {
            gd.setFullScreenWindow(this);
        } else {
            System.err.println("Fullscreen not supported");
            setSize(getToolkit().getScreenSize()); // Fallback
        }

        addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                // User is back in the window
                if (isPaused) {
                    resumeTest();
                }
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                // User has tabbed away or minimized
                if (!isLockedOut && !isPaused) {
                    handleWarning();
                }
            }
        });

        // Handle explicit close attempts (Alt+F4)
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!isLockedOut) {
                    handleWarning();
                }
            }
            // Prevent minimizing
            @Override
            public void windowIconified(WindowEvent e) {
                if(!isLockedOut) {
                    handleWarning();
                    // Force window back
                    setState(Frame.NORMAL);
                }
            }
        });

        // Ensure focus
        this.requestFocusInWindow();
    }

    private void handleWarning() {
        if (isLockedOut) return;

        warningCount++;
        isPaused = true;

        if (warningCount >= MAX_WARNINGS) {
            lockOut();
        } else {
            pauseTest();
        }
    }

    private void pauseTest() {
        // Show a modal dialog on top
        String message = "You have left the fullscreen test environment.\n" +
                "You must return to continue.\n\n" +
                "Warning: " + warningCount + " / " + MAX_WARNINGS;

        JDialog dialog = new JDialog(this, "Test Paused", true);
        dialog.setLayout(new BorderLayout(20, 20));
        dialog.setUndecorated(true);
        dialog.getRootPane().setBorder(BorderFactory.createLineBorder(Color.YELLOW, 4));

        JLabel msgLabel = new JLabel("<html><div style='text-align: center; padding: 20px;'>" + message.replace("\n", "<br>") + "</div></html>");
        msgLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        msgLabel.setHorizontalAlignment(SwingConstants.CENTER);

        JButton resumeButton = new JButton("Resume Test");
        resumeButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        resumeButton.addActionListener(e -> dialog.dispose());

        dialog.add(msgLabel, BorderLayout.CENTER);
        dialog.add(resumeButton, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true); // This blocks until the dialog is closed

        // After dialog is closed
        resumeTest();
    }

    private void resumeTest() {
        isPaused = false;
        // Re-assert fullscreen
        if (gd.isFullScreenSupported() && gd.getFullScreenWindow() != this) {
            gd.setFullScreenWindow(this);
        }
        this.requestFocus();
    }

    private void lockOut() {
        isLockedOut = true;
        testViewerPanel.stopPolling(); // Stop the test

        // Submit whatever answers we have
        testViewerPanel.submitTest(true); // true = locked out

        String message = "You have been locked out for exiting fullscreen 3 times.\n" +
                "Your partial results have been submitted.\n" +
                "Please contact your teacher.";

        JOptionPane.showMessageDialog(
                this,
                message,
                "Test Locked",
                JOptionPane.ERROR_MESSAGE
        );

        // Close this test window and go back to the dashboard
        exitTest();
    }

    public void exitTest() {
        // Called by TestViewerPanel on success, or by lockout
        if (gd.getFullScreenWindow() == this) {
            gd.setFullScreenWindow(null);
        }
        this.dispose();
        mainFrame.returnToMainWindow();
    }
}