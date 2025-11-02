// src/main/java/com/mcq/client/panels/TeacherDashboardPanel.java
package com.mcq.client.panels;

import com.mcq.client.Main;
import com.mcq.client.lib.ApiClient;
import com.mcq.client.lib.Models.ClassroomDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class TeacherDashboardPanel extends JPanel {

    private final Main mainFrame;
    private final ApiClient apiClient;
    private JPanel classroomGridPanel;
    private JLabel loadingLabel;

    public TeacherDashboardPanel(Main mainFrame) {
        this.mainFrame = mainFrame;
        this.apiClient = ApiClient.getInstance();

        setLayout(new BorderLayout());
        setBackground(new Color(248, 250, 252)); // Gray-50

        // Add Navbar
        add(new NavbarPanel(), BorderLayout.NORTH);

        // Main content area
        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(20, 40, 20, 40));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("My Classrooms");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        headerPanel.add(title, BorderLayout.WEST);

        JButton createButton = new JButton("+ Create Classroom");
        createButton.setBackground(new Color(22, 163, 74)); // Green
        createButton.setForeground(Color.WHITE);
        createButton.addActionListener(e -> handleCreateClassroom());
        headerPanel.add(createButton, BorderLayout.EAST);

        mainContent.add(headerPanel, BorderLayout.NORTH);

        // Classroom Grid
        classroomGridPanel = new JPanel(new GridLayout(0, 3, 20, 20)); // 3 columns
        classroomGridPanel.setOpaque(false);

        loadingLabel = new JLabel("Loading classrooms...");
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);

        mainContent.add(loadingLabel, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);

        fetchClassrooms();
    }

    private void fetchClassrooms() {
        loadingLabel.setVisible(true);
        loadingLabel.setText("Loading classrooms...");

        // Clear old classrooms before fetching
        classroomGridPanel.removeAll();
        // Ensure scrollpane is removed if it exists
        Component[] components = ((JPanel) getComponent(1)).getComponents();
        for(Component c : components) {
            if(c instanceof JScrollPane) {
                ((JPanel) getComponent(1)).remove(c);
            }
        }

        new SwingWorker<List<ClassroomDTO>, Void>() {
            @Override
            protected List<ClassroomDTO> doInBackground() throws Exception {
                return apiClient.getClassrooms("mine");
            }

            @Override
            protected void done() {
                loadingLabel.setVisible(false);
                try {
                    List<ClassroomDTO> classrooms = get();
                    if (classrooms.isEmpty()) {
                        showEmptyMessage();
                    } else {
                        populateClassroomGrid(classrooms);
                    }
                } catch (Exception e) {
                    showErrorMessage(e.getMessage());
                }
            }
        }.execute();
    }

    private void populateClassroomGrid(List<ClassroomDTO> classrooms) {
        classroomGridPanel.removeAll();
        for (ClassroomDTO classroom : classrooms) {
            classroomGridPanel.add(createClassroomCard(classroom));
        }

        JScrollPane scrollPane = new JScrollPane(classroomGridPanel);
        scrollPane.getViewport().setBackground(new Color(248, 250, 252));
        scrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel mainContent = (JPanel) getComponent(1); // Get main content panel
        mainContent.add(scrollPane, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JPanel createClassroomCard(ClassroomDTO classroom) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel icon = new JLabel("B"); // Icon
        icon.setOpaque(true);
        icon.setBackground(new Color(220, 252, 231)); // Green-100
        icon.setForeground(new Color(22, 163, 74)); // Green-600
        icon.setFont(new Font("SansSerif", Font.BOLD, 18));
        icon.setBorder(new EmptyBorder(8, 12, 8, 12));

        JLabel code = new JLabel(classroom.code());
        code.setFont(new Font("Monospaced", Font.PLAIN, 12));
        code.setForeground(Color.GRAY);

        header.add(icon, BorderLayout.WEST);
        header.add(code, BorderLayout.EAST);

        // Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel name = new JLabel(classroom.classroomname());
        name.setFont(new Font("SansSerif", Font.BOLD, 20));

        JLabel students = new JLabel(classroom.classroomstudents().size() + " students");
        students.setFont(new Font("SansSerif", Font.PLAIN, 14));
        students.setForeground(Color.GRAY);

        content.add(name);
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(students);

        card.add(header, BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);

        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                mainFrame.showClassroomDetail(classroom.code());
            }
        });

        return card;
    }

    private void showEmptyMessage() {
        JPanel mainContent = (JPanel) getComponent(1);
        loadingLabel.setText("No classrooms yet. Create one to get started!");
        loadingLabel.setVisible(true);
        mainContent.add(loadingLabel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void showErrorMessage(String message) {
        JPanel mainContent = (JPanel) getComponent(1);
        loadingLabel.setText("Error: " + message);
        loadingLabel.setForeground(Color.RED);
        loadingLabel.setVisible(true);
        mainContent.add(loadingLabel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void handleCreateClassroom() {
        String name = JOptionPane.showInputDialog(
                mainFrame,
                "Enter the classroom name:",
                "Create Classroom",
                JOptionPane.PLAIN_MESSAGE
        );

        if (name != null && !name.trim().isEmpty()) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    apiClient.createClassroom(name.trim());
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        fetchClassrooms(); // Refresh the list
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                mainFrame,
                                "Failed to create classroom: " + e.getCause().getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }.execute();
        }
    }
}