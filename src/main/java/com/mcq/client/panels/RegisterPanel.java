// src/main/java/com/mcq/client/panels/RegisterPanel.java
package com.mcq.client.panels;

import com.mcq.client.Main;
import com.mcq.client.lib.AuthService;
import com.mcq.client.lib.Models.RegisterRequest;

import javax.swing.*;
import java.awt.*;

public class RegisterPanel extends JPanel {

    private final Main mainFrame;
    private final AuthService authService;
    private JTextField firstnameField, lastnameField, emailField, usernameField;
    private JPasswordField passwordField, confirmPasswordField;
    private JComboBox<String> roleComboBox;
    private JButton registerButton, loginButton;
    private JLabel errorLabel;

    public RegisterPanel(Main mainFrame) {
        this.mainFrame = mainFrame;
        this.authService = AuthService.getInstance();

        setLayout(new GridBagLayout());
        setBackground(new Color(236, 253, 245)); // Light green gradient

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(20, 40, 20, 40)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Create Account");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        gbc.gridwidth = 4;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(title, gbc);

        // --- Row 1: First and Last Name ---
        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("First Name:"), gbc);

        gbc.gridx = 1;
        firstnameField = new JTextField(15);
        formPanel.add(firstnameField, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("Last Name:"), gbc);

        gbc.gridx = 3;
        lastnameField = new JTextField(15);
        formPanel.add(lastnameField, gbc);

        // --- Row 2: Email ---
        gbc.gridy++;
        gbc.gridx = 0;
        formPanel.add(new JLabel("Email:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        emailField = new JTextField(30);
        formPanel.add(emailField, gbc);

        // --- Row 3: Username ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        usernameField = new JTextField(30);
        formPanel.add(usernameField, gbc);

        // --- Row 4: Role ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Role:"), gbc);

        gbc.gridx = 1;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        roleComboBox = new JComboBox<>(new String[]{"Student", "Teacher"});
        formPanel.add(roleComboBox, gbc);

        // --- Row 5: Password ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(15);
        formPanel.add(passwordField, gbc);

        gbc.gridx = 2;
        formPanel.add(new JLabel("Confirm:"), gbc);

        gbc.gridx = 3;
        confirmPasswordField = new JPasswordField(15);
        formPanel.add(confirmPasswordField, gbc);

        // --- Row 6: Error Label ---
        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        formPanel.add(errorLabel, gbc);

        // --- Row 7: Buttons ---
        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;
        registerButton = new JButton("Create Account");
        registerButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        registerButton.setBackground(new Color(22, 163, 74)); // Green color
        registerButton.setForeground(Color.WHITE);
        formPanel.add(registerButton, gbc);

        gbc.gridy++;
        loginButton = new JButton("Already have an account? Sign in");
        loginButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        loginButton.setForeground(new Color(22, 163, 74));
        loginButton.setBorder(BorderFactory.createEmptyBorder());
        loginButton.setContentAreaFilled(false);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        formPanel.add(loginButton, gbc);

        // --- NEW QUIT BUTTON ---
        gbc.gridy++;
        gbc.insets = new Insets(20, 8, 8, 8); // Add top margin
        JButton quitButton = new JButton("Quit Application");
        quitButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        quitButton.setBackground(new Color(220, 38, 38)); // Red-600
        quitButton.setForeground(Color.WHITE);
        quitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        formPanel.add(quitButton, gbc);
        // --- END NEW BUTTON ---

        add(formPanel);

        // --- Action Listeners ---
        registerButton.addActionListener(e -> handleRegister());
        loginButton.addActionListener(e -> mainFrame.showLogin());
        quitButton.addActionListener(e -> System.exit(0)); // <-- ADDED
    }

    private void handleRegister() {
        String fname = firstnameField.getText();
        String lname = lastnameField.getText();
        String email = emailField.getText();
        String uname = usernameField.getText();
        String pass = new String(passwordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());
        String role = roleComboBox.getSelectedItem().equals("Student") ? "ROLE_STUDENT" : "ROLE_TEACHER";

        if (!pass.equals(confirmPass)) {
            errorLabel.setText("Passwords do not match.");
            return;
        }

        RegisterRequest request = new RegisterRequest(fname, lname, email, uname, pass, role);

        registerButton.setText("Creating...");
        registerButton.setEnabled(false);
        errorLabel.setText(" ");

        new SwingWorker<Void, Void>() {
            private String successMsg = null;

            @Override
            protected Void doInBackground() throws Exception {
                authService.register(request);
                successMsg = "Registration successful! Redirecting to login...";
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    errorLabel.setForeground(new Color(22, 163, 74));
                    errorLabel.setText(successMsg);
                    // Redirect to login after 2 seconds
                    new Timer(2000, e -> mainFrame.showLogin()) {{
                        setRepeats(false);
                        start();
                    }};
                } catch (Exception ex) {
                    errorLabel.setForeground(Color.RED);
                    errorLabel.setText(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                    registerButton.setText("Create Account");
                    registerButton.setEnabled(true);
                }
            }
        }.execute();
    }
}