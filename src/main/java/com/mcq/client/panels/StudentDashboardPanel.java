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

public class StudentDashboardPanel extends JPanel {

    private final Main mainFrame;
    private final ApiClient apiClient;
    private JPanel classroomGridPanel;
    private JLabel loadingLabel;

    public StudentDashboardPanel(Main mainFrame) {
        this.mainFrame = mainFrame;
        this.apiClient = ApiClient.getInstance();

        setLayout(new BorderLayout());
        setBackground(new Color(248, 250, 252));

        add(new NavbarPanel(), BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(20, 40, 20, 40));

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));

        JLabel title = new JLabel("My Classrooms");
        title.setFont(new Font("SansSerif", Font.BOLD, 30));
        headerPanel.add(title, BorderLayout.WEST);

        JButton joinButton = new JButton("+ Join Classroom");
        joinButton.setBackground(new Color(37, 99, 235));
        joinButton.setForeground(Color.WHITE);
        joinButton.addActionListener(e -> handleJoinClassroom());
        headerPanel.add(joinButton, BorderLayout.EAST);

        mainContent.add(headerPanel, BorderLayout.NORTH);

        classroomGridPanel = new JPanel(new GridLayout(0, 3, 20, 20));
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

        classroomGridPanel.removeAll();

        JPanel mainContent = (JPanel) getComponent(1);

        Component centerComponent = ((BorderLayout) mainContent.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComponent != null && centerComponent instanceof JScrollPane) {
            mainContent.remove(centerComponent);
        }
        mainContent.add(loadingLabel, BorderLayout.CENTER);
        mainContent.revalidate();
        mainContent.repaint();


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

        JPanel mainContent = (JPanel) getComponent(1);

        Component centerComponent = ((BorderLayout) mainContent.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComponent != null && centerComponent == loadingLabel) {
            mainContent.remove(loadingLabel);
        }

        mainContent.add(scrollPane, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    private JPanel createClassroomCard(ClassroomDTO classroom) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                new EmptyBorder(20, 20, 20, 20)
        ));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel name = new JLabel(classroom.classroomname());
        name.setFont(new Font("SansSerif", Font.BOLD, 20));

        JLabel code = new JLabel(classroom.code());
        code.setFont(new Font("Monospaced", Font.BOLD, 16));
        code.setForeground(Color.BLACK);
        code.setBackground(new Color(243, 244, 246));
        code.setOpaque(true);
        code.setBorder(new EmptyBorder(5, 8, 5, 8));

        JLabel teacher = new JLabel("Teacher: " + classroom.classroomteacher().firstname() + " " + classroom.classroomteacher().lastname());
        teacher.setFont(new Font("SansSerif", Font.PLAIN, 14));
        teacher.setForeground(Color.DARK_GRAY);

        JLabel students = new JLabel(classroom.classroomstudents().size() + " students enrolled");
        students.setFont(new Font("SansSerif", Font.PLAIN, 14));
        students.setForeground(Color.GRAY);

        content.add(name);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(code);
        content.add(Box.createRigidArea(new Dimension(0, 12)));
        content.add(teacher);
        content.add(Box.createRigidArea(new Dimension(0, 2)));
        content.add(students);

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
        loadingLabel.setText("No classrooms yet. Join one to get started!");
        loadingLabel.setVisible(true);

        Component centerComponent = ((BorderLayout) mainContent.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComponent != null && centerComponent != loadingLabel) {
            mainContent.remove(centerComponent);
            mainContent.add(loadingLabel, BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }

    private void showErrorMessage(String message) {
        JPanel mainContent = (JPanel) getComponent(1);
        loadingLabel.setText("Error: " + message);
        loadingLabel.setForeground(Color.RED);
        loadingLabel.setVisible(true);

        Component centerComponent = ((BorderLayout) mainContent.getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComponent != null && centerComponent != loadingLabel) {
            mainContent.remove(centerComponent);
            mainContent.add(loadingLabel, BorderLayout.CENTER);
        }

        revalidate();
        repaint();
    }

    private void handleJoinClassroom() {
        String code = JOptionPane.showInputDialog(
                mainFrame,
                "Enter the classroom code:",
                "Join Classroom",
                JOptionPane.PLAIN_MESSAGE
        );

        if (code != null && !code.trim().isEmpty()) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    apiClient.joinClassroom(code.trim());
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        fetchClassrooms();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(
                                mainFrame,
                                "Failed to join classroom: " + e.getCause().getMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }.execute();
        }
    }
}