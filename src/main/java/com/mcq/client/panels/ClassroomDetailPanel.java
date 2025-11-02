// src/main/java/com/mcq/client/panels/ClassroomDetailPanel.java
package com.mcq.client.panels;

import com.mcq.client.Main;
import com.mcq.client.lib.ApiClient;
import com.mcq.client.lib.AuthService;
import com.mcq.client.lib.Models;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class ClassroomDetailPanel extends JPanel {

    private final Main mainFrame;
    private final ApiClient apiClient;
    private final AuthService authService;
    private final String classroomCode;

    private JLabel classroomNameLabel;
    private JLabel classroomCodeLabel;
    private JPanel testsPanel;
    private JList<String> studentList;
    private DefaultListModel<String> studentListModel;
    private JScrollPane testsScrollPane;
    private JLabel loadingLabel;

    public ClassroomDetailPanel(Main mainFrame, String classroomCode) {
        this.mainFrame = mainFrame;
        this.apiClient = ApiClient.getInstance();
        this.authService = AuthService.getInstance();
        this.classroomCode = classroomCode;

        setLayout(new BorderLayout());
        setBackground(new Color(248, 250, 252));

        // Navbar
        add(new NavbarPanel(), BorderLayout.NORTH);

        // Main content
        JPanel mainContent = new JPanel(new BorderLayout(20, 20));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(20, 40, 20, 40));

        // Back button
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JButton backButton = new JButton("< Back to Dashboard");
        backButton.setContentAreaFilled(false);
        backButton.setBorder(BorderFactory.createEmptyBorder());
        backButton.setForeground(Color.GRAY);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> mainFrame.showDashboard());
        topBar.add(backButton, BorderLayout.WEST);
        mainContent.add(topBar, BorderLayout.NORTH);

        // Center Panel (Loading)
        loadingLabel = new JLabel("Loading classroom details...");
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainContent.add(loadingLabel, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);

        fetchData();
    }

    private void fetchData() {
        new SwingWorker<Void, Void>() {
            Models.ClassroomDTO classroomData;
            List<Models.Test> testsData;

            @Override
            protected Void doInBackground() throws Exception {
                classroomData = apiClient.getClassroom(classroomCode);
                testsData = apiClient.getTests(classroomCode);
                return null;
            }

            @Override
            protected void done() {
                loadingLabel.setVisible(false);
                try {
                    get();
                    buildUI(classroomData, testsData);
                } catch (Exception e) {
                    loadingLabel.setText("Error: " + e.getMessage());
                    loadingLabel.setForeground(Color.RED);
                    loadingLabel.setVisible(true);
                }
            }
        }.execute();
    }

    private void buildUI(Models.ClassroomDTO classroom, List<Models.Test> tests) {
        JPanel mainContent = (JPanel) getComponent(1); // Get main content panel
        mainContent.remove(loadingLabel); // Remove loading label

        // Split Pane for Students and Tests
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.3); // Give 30% to students list
        splitPane.setOpaque(false);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        // --- Left Side: Students ---
        JPanel studentPanel = new JPanel(new BorderLayout(10, 10));
        studentPanel.setOpaque(false);
        studentPanel.setBorder(new TitledBorder("Students (" + classroom.classroomstudents().size() + ")"));

        studentListModel = new DefaultListModel<>();
        for (String student : classroom.classroomstudents()) {
            studentListModel.addElement(student);
        }
        studentList = new JList<>(studentListModel);
        studentList.setCellRenderer(new StudentCellRenderer(authService.isTeacher()));

        if (authService.isTeacher()) {
            studentList.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    if (evt.getClickCount() == 2) {
                        String student = studentList.getSelectedValue();
                        if (student != null) {
                            handleRemoveStudent(student);
                        }
                    }
                }
            });
        }
        studentPanel.add(new JScrollPane(studentList), BorderLayout.CENTER);

        // --- Right Side: Classroom Info & Tests ---
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setOpaque(false);

        // Classroom Info
        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);
        infoPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(15, 15, 15, 15)
        ));

        classroomNameLabel = new JLabel(classroom.classroomname());
        classroomNameLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        infoPanel.add(classroomNameLabel, BorderLayout.NORTH);

        classroomCodeLabel = new JLabel("Code: " + classroom.code());
        classroomCodeLabel.setFont(new Font("Monospaced", Font.PLAIN, 14));
        infoPanel.add(classroomCodeLabel, BorderLayout.CENTER);

        if (authService.isTeacher()) {
            JButton uploadTestButton = new JButton("+ Upload Test");
            uploadTestButton.setBackground(new Color(22, 163, 74));
            uploadTestButton.setForeground(Color.WHITE);
            uploadTestButton.addActionListener(e -> handleCreateTest());
            infoPanel.add(uploadTestButton, BorderLayout.EAST);
        }
        rightPanel.add(infoPanel, BorderLayout.NORTH);

        // Tests List
        testsPanel = new JPanel();
        testsPanel.setLayout(new BoxLayout(testsPanel, BoxLayout.Y_AXIS));
        testsPanel.setOpaque(false);

        populateTests(tests);

        testsScrollPane = new JScrollPane(testsPanel);
        testsScrollPane.setBorder(new TitledBorder("Tests (" + tests.size() + ")"));
        testsScrollPane.getViewport().setBackground(new Color(248, 250, 252));
        rightPanel.add(testsScrollPane, BorderLayout.CENTER);

        // Add to split pane
        splitPane.setLeftComponent(studentPanel);
        splitPane.setRightComponent(rightPanel);

        mainContent.add(splitPane, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void populateTests(List<Models.Test> tests) {
        testsPanel.removeAll();
        if (tests.isEmpty()) {
            JLabel noTests = new JLabel("No tests available yet.");
            noTests.setHorizontalAlignment(SwingConstants.CENTER);
            testsPanel.add(noTests);
        } else {
            for (Models.Test test : tests) {
                testsPanel.add(createTestCard(test));
                testsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            }
        }
        testsPanel.revalidate();
        testsPanel.repaint();
    }

    private JPanel createTestCard(Models.Test test) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                new EmptyBorder(15, 15, 15, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100)); // Constrain height

        // Left side: Info
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);

        JLabel name = new JLabel(test.testname());
        name.setFont(new Font("SansSerif", Font.BOLD, 18));

        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        meta.setOpaque(false);

        JLabel status = new JLabel(test.status());
        status.setFont(new Font("SansSerif", Font.BOLD, 12));
        if (test.status().equals("ACTIVE")) {
            status.setForeground(new Color(22, 163, 74)); // Green
        } else if (test.status().equals("ENDED")) {
            status.setForeground(new Color(217, 119, 6)); // Yellow
        } else {
            status.setForeground(Color.GRAY);
        }

        JLabel questions = new JLabel(test.questionCount() + " questions");
        questions.setForeground(Color.GRAY);

        meta.add(status);
        meta.add(questions);

        info.add(name);
        info.add(Box.createRigidArea(new Dimension(0, 5)));
        info.add(meta);

        // Right side: Actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        actions.setOpaque(false);

        if (authService.isTeacher()) {
            if (test.status().equals("NOT_STARTED")) {
                JButton start = new JButton("Start");
                start.setBackground(new Color(22, 163, 74));
                start.setForeground(Color.WHITE);
                start.addActionListener(e -> handleStartTest(test.testname()));
                actions.add(start);
            }
            if (test.status().equals("ACTIVE")) {
                JButton end = new JButton("End");
                end.setBackground(new Color(217, 119, 6));
                end.setForeground(Color.WHITE);
                end.addActionListener(e -> handleEndTest(test.testname()));
                actions.add(end);
            }
            JButton view = new JButton("Submissions");
            view.setBackground(new Color(37, 99, 235));
            view.setForeground(Color.WHITE);
            view.addActionListener(e -> mainFrame.showTestResults(classroomCode, test.testname()));
            actions.add(view);

            JButton delete = new JButton("X");
            delete.setForeground(new Color(220, 38, 38));
            delete.addActionListener(e -> handleDeleteTest(test.testname()));
            actions.add(delete);

        } else { // Student
            if (test.status().equals("ACTIVE")) {
                // This button is a safeguard, but StudentViewPanel should catch active tests first
                JButton take = new JButton("Take Test");
                take.setBackground(new Color(37, 99, 235));
                take.setForeground(Color.WHITE);
                take.addActionListener(e -> mainFrame.showTestViewer(classroomCode, test.testname()));
                actions.add(take);
            }
            if (test.status().equals("ENDED")) {
                JButton view = new JButton("View Results");
                view.setBackground(Color.GRAY);
                view.setForeground(Color.WHITE);
                view.addActionListener(e -> mainFrame.showTestResults(classroomCode, test.testname()));
                actions.add(view);
            }
        }

        card.add(info, BorderLayout.CENTER);
        card.add(actions, BorderLayout.EAST);
        return card;
    }

    // --- Action Handlers (Teacher) ---

    private void handleRemoveStudent(String studentUsername) {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to remove @" + studentUsername + "?",
                "Confirm Removal",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm == JOptionPane.YES_OPTION) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    apiClient.removeStudent(classroomCode, studentUsername);
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                        studentListModel.removeElement(studentUsername);
                        // Update title
                        ((TitledBorder)((JPanel)studentList.getParent().getParent()).getBorder())
                                .setTitle("Students (" + studentListModel.getSize() + ")");
                        studentList.getParent().getParent().repaint();
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(ClassroomDetailPanel.this, "Failed to remove student: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void handleStartTest(String testname) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                apiClient.startTest(classroomCode, testname);
                return null;
            }
            @Override
            protected void done() {
                try { get(); fetchData(); } // Refresh everything
                catch (Exception e) { JOptionPane.showMessageDialog(ClassroomDetailPanel.this, "Failed to start test: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }

    private void handleEndTest(String testname) {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                apiClient.endTest(classroomCode, testname);
                return null;
            }
            @Override
            protected void done() {
                try { get(); fetchData(); } // Refresh everything
                catch (Exception e) { JOptionPane.showMessageDialog(ClassroomDetailPanel.this, "Failed to end test: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }

    private void handleDeleteTest(String testname) {
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this test? This is permanent.", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                apiClient.deleteTest(classroomCode, testname);
                return null;
            }
            @Override
            protected void done() {
                try { get(); fetchData(); } // Refresh everything
                catch (Exception e) { JOptionPane.showMessageDialog(ClassroomDetailPanel.this, "Failed to delete test: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
            }
        }.execute();
    }

    private void handleCreateTest() {
        // Create a custom dialog for test creation
        JTextField testNameField = new JTextField();
        JTextField answersField = new JTextField();
        JButton fileButton = new JButton("Select PDF");
        JLabel fileLabel = new JLabel("No file selected.");
        final File[] pdfFile = {null};

        fileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                pdfFile[0] = fileChooser.getSelectedFile();
                fileLabel.setText(pdfFile[0].getName());
            }
        });

        JPanel panel = new JPanel(new GridLayout(0, 1, 10, 10));
        panel.add(new JLabel("Test Name:"));
        panel.add(testNameField);
        panel.add(new JLabel("Correct Answers (A,B,C...):"));
        panel.add(answersField);
        panel.add(fileButton);
        panel.add(fileLabel);

        int result = JOptionPane.showConfirmDialog(this, panel, "Create Test", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String name = testNameField.getText();
            String answers = answersField.getText().toUpperCase().replaceAll("[^A-D,]", "");
            List<String> answerList = Arrays.asList(answers.split(",")).stream()
                    .filter(s -> !s.isEmpty())
                    .collect(java.util.stream.Collectors.toList());

            // Validation
            Pattern answerPattern = Pattern.compile("^[A-D]$");
            if(name.isEmpty() || answers.isEmpty() || pdfFile[0] == null || answerList.stream().anyMatch(a -> !answerPattern.matcher(a).matches())) {
                JOptionPane.showMessageDialog(this, "Invalid input. Check all fields.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    // --- FIXED PARAMETER ORDER ---
                    apiClient.createTest(classroomCode, name, pdfFile[0], answerList);
                    return null;
                }
                @Override
                protected void done() {
                    try { get(); fetchData(); }
                    catch (Exception e) { JOptionPane.showMessageDialog(ClassroomDetailPanel.this, "Failed to create test: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE); }
                }
            }.execute();
        }
    }

    // Custom renderer to show a "delete" icon for teachers
    class StudentCellRenderer extends DefaultListCellRenderer {
        private final boolean isTeacher;
        public StudentCellRenderer(boolean isTeacher) {
            this.isTeacher = isTeacher;
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (isTeacher) {
                setText(value + " (Double-click to remove)");
                setForeground(Color.DARK_GRAY);
            }
            return c;
        }
    }
}