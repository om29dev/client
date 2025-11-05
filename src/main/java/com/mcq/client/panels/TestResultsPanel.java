package com.mcq.client.panels;

import com.mcq.client.Main;
import com.mcq.client.lib.ApiClient;
import com.mcq.client.lib.AuthService;
import com.mcq.client.lib.Models;
import com.mcq.client.lib.PdfUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class TestResultsPanel extends JPanel {

    private final Main mainFrame;
    private final ApiClient apiClient;
    private final AuthService authService;
    private final String classroomCode;
    private final String testname;

    private JLabel loadingLabel;

    public TestResultsPanel(Main mainFrame, String classroomCode, String testname) {
        this.mainFrame = mainFrame;
        this.apiClient = ApiClient.getInstance();
        this.authService = AuthService.getInstance();
        this.classroomCode = classroomCode;
        this.testname = testname;

        setLayout(new BorderLayout());
        setBackground(new Color(248, 250, 252));

        add(new NavbarPanel(), BorderLayout.NORTH);

        JPanel mainContent = new JPanel(new BorderLayout(10, 10));
        mainContent.setOpaque(false);
        mainContent.setBorder(new EmptyBorder(20, 40, 20, 40));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JButton backButton = new JButton("< Back to Classroom");
        backButton.setContentAreaFilled(false);
        backButton.setBorder(BorderFactory.createEmptyBorder());
        backButton.setForeground(Color.GRAY);
        backButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backButton.addActionListener(e -> mainFrame.showClassroomDetail(classroomCode));
        topBar.add(backButton, BorderLayout.WEST);
        mainContent.add(topBar, BorderLayout.NORTH);

        loadingLabel = new JLabel("Loading results...");
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainContent.add(loadingLabel, BorderLayout.CENTER);

        add(mainContent, BorderLayout.CENTER);

        fetchResults();
    }

    private void fetchResults() {
        new SwingWorker<Object, Void>() {
            @Override
            protected Object doInBackground() throws Exception {
                if (authService.isTeacher()) {
                    return apiClient.getAllSubmissions(classroomCode, testname);
                } else {
                    return apiClient.getMySubmission(classroomCode, testname);
                }
            }

            @Override
            protected void done() {
                loadingLabel.setVisible(false);
                JPanel mainContent = (JPanel) getComponent(1);
                try {
                    Object results = get();
                    if (authService.isTeacher()) {
                        buildTeacherUI((Models.TeacherResultsDTO) results);
                    } else {
                        buildStudentUI((Models.StudentResultDTO) results);
                    }
                } catch (Exception e) {
                    loadingLabel.setText("Error loading results: " + e.getMessage());
                    loadingLabel.setForeground(Color.RED);
                    loadingLabel.setVisible(true);
                    mainContent.add(loadingLabel, BorderLayout.CENTER);
                }
                mainContent.revalidate();
                mainContent.repaint();
            }
        }.execute();
    }

    private void buildTeacherUI(Models.TeacherResultsDTO results) {
        JPanel mainContent = (JPanel) getComponent(1);
        mainContent.remove(loadingLabel);

        JPanel teacherPanel = new JPanel(new BorderLayout(10, 10));
        teacherPanel.setOpaque(false);

        double avgScore = results.submissions().isEmpty() ? 0 :
                results.submissions().stream().mapToDouble(Models.StudentResultDTO::score).average().orElse(0);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0,0,10,0));

        JLabel title = new JLabel(testname + " Submissions");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        headerPanel.add(title, BorderLayout.WEST);

        JLabel stats = new JLabel(
                String.format("Submissions: %d | Total Qs: %d | Avg. Score: %.1f",
                        results.submissions().size(),
                        results.totalQuestions(),
                        avgScore)
        );
        stats.setFont(new Font("SansSerif", Font.BOLD, 16));
        headerPanel.add(stats, BorderLayout.EAST);

        teacherPanel.add(headerPanel, BorderLayout.NORTH);

        String[] columnNames = new String[results.totalQuestions() + 2];
        columnNames[0] = "Student";
        columnNames[1] = "Score";
        for (int i = 0; i < results.totalQuestions(); i++) {
            String correctAns = (results.correctAnswers() != null && i < results.correctAnswers().size()) ? results.correctAnswers().get(i) : "?";
            columnNames[i+2] = "Q" + (i + 1) + " (" + correctAns + ")";
        }

        Object[][] data = new Object[results.submissions().size()][columnNames.length];
        for(int i = 0; i < results.submissions().size(); i++) {
            Models.StudentResultDTO sub = results.submissions().get(i);
            data[i][0] = sub.user().firstname() + " " + sub.user().lastname();
            data[i][1] = sub.score() + "/" + sub.totalQuestions();
            for(int j = 0; j < results.totalQuestions(); j++) {
                data[i][j+2] = (sub.userAnswers() != null && sub.userAnswers().size() > j) ? sub.userAnswers().get(j) : "-";
            }
        }

        DefaultTableModel tableModel = new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(30);
        table.getTableHeader().setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new TeacherCellRenderer(results.correctAnswers()));

        teacherPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        mainContent.add(teacherPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private void buildStudentUI(Models.StudentResultDTO result) {
        JPanel mainContent = (JPanel) getComponent(1);
        mainContent.remove(loadingLabel);

        JPanel studentPanel = new JPanel(new BorderLayout(10, 10));
        studentPanel.setOpaque(false);

        double scorePercent = (result.totalQuestions() > 0) ? (double) result.score() / result.totalQuestions() * 100 : 0.0;
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0,0,10,0));

        JLabel title = new JLabel(testname + " Results");
        title.setFont(new Font("SansSerif", Font.BOLD, 24));
        headerPanel.add(title, BorderLayout.WEST);

        JLabel score = new JLabel(
                String.format("Your Score: %d / %d (%.0f%%)",
                        result.score(),
                        result.totalQuestions(),
                        scorePercent)
        );
        score.setFont(new Font("SansSerif", Font.BOLD, 20));
        score.setForeground(new Color(37, 99, 235));
        headerPanel.add(score, BorderLayout.EAST);

        studentPanel.add(headerPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.7);
        splitPane.setOpaque(false);

        JPanel pdfPanel = new JPanel(new BorderLayout());
        pdfPanel.setOpaque(false);
        JLabel pdfLabel = new JLabel("Loading PDF...");
        pdfLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pdfPanel.add(pdfLabel, BorderLayout.CENTER);

        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() throws Exception {
                return apiClient.getTestPDF(classroomCode, testname);
            }
            @Override
            protected void done() {
                try {
                    byte[] pdfData = get();
                    JTabbedPane pdfTabs = new JTabbedPane();
                    for(int i = 0; i < result.totalQuestions(); i++) {
                        JLabel pageLabel = new JLabel();
                        pageLabel.setHorizontalAlignment(SwingConstants.CENTER);
                        int parentWidth = Math.max(splitPane.getLeftComponent().getWidth() - 40, 400);
                        ImageIcon icon = PdfUtil.getScaledPdfPage(pdfData, i, parentWidth);
                        pageLabel.setIcon(icon);
                        pdfTabs.addTab("Q " + (i+1), new JScrollPane(pageLabel));
                    }
                    pdfPanel.removeAll();
                    pdfPanel.add(pdfTabs, BorderLayout.CENTER);
                    pdfPanel.revalidate();
                    pdfPanel.repaint();
                } catch (Exception e) {
                    pdfLabel.setText("Failed to load PDF: " + e.getMessage());
                }
            }
        }.execute();

        JPanel answerListPanel = new JPanel();
        answerListPanel.setLayout(new BoxLayout(answerListPanel, BoxLayout.Y_AXIS));
        answerListPanel.setBackground(Color.WHITE);

        for (int i = 0; i < result.totalQuestions(); i++) {
            String userAnswer = (result.userAnswers() != null && result.userAnswers().size() > i) ? result.userAnswers().get(i) : "N/A";
            String correctAnswer = (result.correctAnswers() != null && result.correctAnswers().size() > i) ? result.correctAnswers().get(i) : "?";
            boolean isCorrect = userAnswer.equals(correctAnswer);

            JPanel answerCard = new JPanel();
            answerCard.setLayout(new BoxLayout(answerCard, BoxLayout.Y_AXIS));
            answerCard.setBackground(isCorrect ? new Color(240, 253, 244) : new Color(254, 242, 242));
            answerCard.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(isCorrect ? new Color(134, 239, 172) : new Color(254, 202, 202), 2),
                    new EmptyBorder(10, 10, 10, 10)
            ));

            JLabel qLabel = new JLabel("Question " + (i + 1));
            qLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

            JLabel your = new JLabel("Your Answer: " + userAnswer);
            your.setFont(new Font("SansSerif", Font.BOLD, 14));

            answerCard.add(qLabel);
            answerCard.add(your);

            if (!isCorrect) {
                JLabel correct = new JLabel("Correct Answer: " + correctAnswer);
                correct.setFont(new Font("SansSerif", Font.PLAIN, 14));
                correct.setForeground(new Color(22, 163, 74));
                answerCard.add(correct);
            }

            answerListPanel.add(answerCard);
            answerListPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        JScrollPane answerScrollPane = new JScrollPane(answerListPanel);
        answerScrollPane.setBorder(BorderFactory.createTitledBorder("Your Results"));

        splitPane.setLeftComponent(pdfPanel);
        splitPane.setRightComponent(answerScrollPane);

        studentPanel.add(splitPane, BorderLayout.CENTER);
        mainContent.add(studentPanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    class TeacherCellRenderer extends DefaultTableCellRenderer {
        private final java.util.List<String> correctAnswers;

        public TeacherCellRenderer(java.util.List<String> correctAnswers) {
            this.correctAnswers = correctAnswers;
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            c.setBackground(Color.WHITE);
            c.setForeground(Color.BLACK);

            if (col > 1) {
                String userAnswer = (String) value;
                if (correctAnswers != null && col - 2 < correctAnswers.size()) {
                    String correctAnswer = correctAnswers.get(col - 2);
                    if (userAnswer.equals(correctAnswer)) {
                        c.setForeground(new Color(22, 163, 74));
                        c.setFont(new Font("SansSerif", Font.BOLD, 14));
                    } else if (!userAnswer.equals("-")) {
                        c.setForeground(new Color(220, 38, 38));
                    } else {
                        c.setForeground(Color.GRAY);
                    }
                }
            } else if (col == 1) {
                c.setFont(new Font("SansSerif", Font.BOLD, 14));
            }

            return c;
        }
    }
}