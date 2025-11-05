package com.mcq.client.panels;

import com.mcq.client.lib.AuthService;
import com.mcq.client.lib.Models.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class NavbarPanel extends JPanel {

    private final AuthService authService;

    public NavbarPanel() {
        this.authService = AuthService.getInstance();
        User user = authService.getUser();

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                new EmptyBorder(10, 20, 10, 20)
        ));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel("B");
        iconLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setOpaque(true);
        iconLabel.setBackground(new Color(37, 99, 235));
        iconLabel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        JLabel title = new JLabel("MCQ Test Platform");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));

        String role = "Student Dashboard";
        if(authService.isTeacher()) {
            role = "Teacher Dashboard";
        }
        JLabel subtitle = new JLabel(role);
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(Color.GRAY);

        textPanel.add(title);
        textPanel.add(subtitle);

        titlePanel.add(iconLabel);
        titlePanel.add(textPanel);
        add(titlePanel, BorderLayout.WEST);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        userPanel.setOpaque(false);

        JPanel userTextPanel = new JPanel();
        userTextPanel.setLayout(new BoxLayout(userTextPanel, BoxLayout.Y_AXIS));
        userTextPanel.setOpaque(false);
        userTextPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);

        if (user != null) {
            JLabel nameLabel = new JLabel(user.firstname() + " " + user.lastname());
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            JLabel usernameLabel = new JLabel("@" + user.username());
            usernameLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
            usernameLabel.setForeground(Color.GRAY);
            usernameLabel.setHorizontalAlignment(SwingConstants.RIGHT);

            userTextPanel.add(nameLabel);
            userTextPanel.add(usernameLabel);
        }

        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(new Color(254, 242, 242));
        logoutButton.setForeground(new Color(220, 38, 38));
        logoutButton.addActionListener(e -> authService.logout());

        userPanel.add(userTextPanel);
        userPanel.add(logoutButton);
        add(userPanel, BorderLayout.EAST);
    }
}