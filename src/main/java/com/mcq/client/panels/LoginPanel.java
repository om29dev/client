package com.mcq.client.panels;

import com.mcq.client.Main;
import com.mcq.client.lib.AuthService;

import javax.swing.*;
import java.awt.*;

public class LoginPanel extends JPanel {

    private final Main mainFrame;
    private final AuthService authService;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JLabel errorLabel;

    public LoginPanel(Main mainFrame) {
        this.mainFrame = mainFrame;
        this.authService = AuthService.getInstance();

        setLayout(new GridBagLayout());
        setBackground(new Color(240, 245, 255));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(Color.WHITE);
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(40, 40, 40, 40)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("Welcome Back");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(title, gbc);

        gbc.gridy++;
        JLabel subtitle = new JLabel("Sign in to your account");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
        subtitle.setForeground(Color.GRAY);
        formPanel.add(subtitle, gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        formPanel.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        formPanel.add(usernameField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        formPanel.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 16));
        formPanel.add(passwordField, gbc);

        gbc.gridy++;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        loginButton = new JButton("Sign In");
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        loginButton.setBackground(new Color(37, 99, 235));
        loginButton.setForeground(Color.WHITE);
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        formPanel.add(loginButton, gbc);

        gbc.gridy++;
        errorLabel = new JLabel(" ");
        errorLabel.setForeground(Color.RED);
        formPanel.add(errorLabel, gbc);

        gbc.gridy++;
        registerButton = new JButton("Don't have an account? Register here");
        registerButton.setFont(new Font("SansSerif", Font.PLAIN, 12));
        registerButton.setForeground(new Color(37, 99, 235));
        registerButton.setBorder(BorderFactory.createEmptyBorder());
        registerButton.setContentAreaFilled(false);
        registerButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        formPanel.add(registerButton, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(20, 10, 10, 10);
        JButton quitButton = new JButton("Quit Application");
        quitButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        quitButton.setBackground(new Color(220, 38, 38));
        quitButton.setForeground(Color.WHITE);
        quitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        formPanel.add(quitButton, gbc);

        add(formPanel);

        loginButton.addActionListener(e -> handleLogin());
        passwordField.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> mainFrame.showRegister());
        quitButton.addActionListener(e -> System.exit(0));
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username and password are required.");
            return;
        }

        loginButton.setText("Signing in...");
        loginButton.setEnabled(false);
        errorLabel.setText(" ");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                authService.login(username, password);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception ex) {
                    errorLabel.setText(ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                } finally {
                    loginButton.setText("Sign In");
                    loginButton.setEnabled(true);
                }
            }
        }.execute();
    }
}