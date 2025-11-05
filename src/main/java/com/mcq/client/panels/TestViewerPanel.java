package com.mcq.client.panels;

import com.mcq.client.Main;
import com.mcq.client.lib.ApiClient;
import com.mcq.client.lib.AuthService;
import com.mcq.client.lib.Models.Test;
import com.mcq.client.lib.Models.User;
import com.mcq.client.lib.PdfUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestViewerPanel extends JPanel {

    private final Main mainFrame;
    private final ApiClient apiClient;
    private final String classroomCode;
    private final String testname;

    private Test test;
    private byte[] pdfData;
    private int currentPage = 0;
    private Map<Integer, String> answers = new HashMap<>();
    private int totalQuestions = 0;
    private List<Integer> questionOrder;

    private JLabel pdfLabel;
    private JScrollPane pdfScrollPane;
    private JLabel pageLabel;
    private JButton prevButton, nextButton;
    private JPanel answerButtonPanel;
    private JPanel questionNavPanel;
    private Timer pollingTimer;
    private boolean isSubmitting = false;

    public TestViewerPanel(Main mainFrame, String classroomCode, String testname) {
        this.mainFrame = mainFrame;
        this.apiClient = ApiClient.getInstance();
        this.classroomCode = classroomCode;
        this.testname = testname;

        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(248, 250, 252));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        createUI();
        loadTest();
    }

    private void createUI() {
        add(createLockedNavbar(), BorderLayout.NORTH);

        JPanel pdfViewerPanel = new JPanel(new BorderLayout(10, 10));
        pdfViewerPanel.setOpaque(false);

        pdfLabel = new JLabel("Loading test...", SwingConstants.CENTER);
        pdfLabel.setFont(new Font("SansSerif", Font.PLAIN, 24));
        pdfLabel.setOpaque(true);
        pdfLabel.setBackground(Color.WHITE);

        pdfScrollPane = new JScrollPane(pdfLabel);
        pdfScrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        JPanel pdfControls = new JPanel(new BorderLayout());
        pdfControls.setOpaque(false);
        pageLabel = new JLabel("Q: - / -", SwingConstants.CENTER);
        pageLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        prevButton = new JButton("< Prev");
        nextButton = new JButton("Next >");
        pdfControls.add(prevButton, BorderLayout.WEST);
        pdfControls.add(pageLabel, BorderLayout.CENTER);
        pdfControls.add(nextButton, BorderLayout.EAST);

        pdfViewerPanel.add(pdfControls, BorderLayout.NORTH);
        pdfViewerPanel.add(pdfScrollPane, BorderLayout.CENTER);

        answerButtonPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        answerButtonPanel.setOpaque(false);
        answerButtonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        for (String option : List.of("A", "B", "C", "D")) {
            JButton btn = new JButton(option);
            btn.setFont(new Font("SansSerif", Font.BOLD, 32));
            btn.setMargin(new Insets(15, 15, 15, 15));
            btn.addActionListener(e -> selectAnswer(option));
            answerButtonPanel.add(btn);
        }
        pdfViewerPanel.add(answerButtonPanel, BorderLayout.SOUTH);

        add(pdfViewerPanel, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(280, 0));

        questionNavPanel = new JPanel(new GridLayout(0, 5, 5, 5));
        questionNavPanel.setOpaque(false);

        JScrollPane navScrollPane = new JScrollPane(questionNavPanel);
        navScrollPane.setBorder(new EmptyBorder(0,0,0,0));
        navScrollPane.getViewport().setBackground(new Color(248, 250, 252));

        JPanel infoBox = new JPanel();
        infoBox.setLayout(new BoxLayout(infoBox, BoxLayout.Y_AXIS));
        infoBox.setBackground(new Color(227, 242, 253));
        infoBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(147, 197, 253)),
                new EmptyBorder(10,10,10,10)
        ));
        JLabel infoLabel = new JLabel("<html>Your answers are being saved. The test will submit automatically when your teacher ends it.</html>");
        infoLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        infoBox.add(infoLabel);

        rightPanel.add(new JLabel("Question Navigator", SwingConstants.CENTER), BorderLayout.NORTH);
        rightPanel.add(navScrollPane, BorderLayout.CENTER);
        rightPanel.add(infoBox, BorderLayout.SOUTH);

        add(rightPanel, BorderLayout.EAST);

        prevButton.addActionListener(e -> navigatePage(currentPage - 1));
        nextButton.addActionListener(e -> navigatePage(currentPage + 1));
    }

    private JPanel createLockedNavbar() {
        JPanel navbar = new JPanel(new BorderLayout());
        navbar.setBackground(Color.WHITE);
        navbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                new EmptyBorder(10, 20, 10, 20)
        ));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);

        JLabel iconLabel = new JLabel("L");
        iconLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        iconLabel.setForeground(Color.WHITE);
        iconLabel.setOpaque(true);
        iconLabel.setBackground(Color.RED);
        iconLabel.setBorder(new EmptyBorder(5, 10, 5, 10));

        JLabel title = new JLabel("Test in Progress");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        titlePanel.add(iconLabel);
        titlePanel.add(title);

        navbar.add(titlePanel, BorderLayout.WEST);

        User user = AuthService.getInstance().getUser();
        if (user != null) {
            JLabel nameLabel = new JLabel(user.firstname() + " " + user.lastname() + " (@" + user.username() + ")");
            nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
            nameLabel.setHorizontalAlignment(SwingConstants.RIGHT);
            navbar.add(nameLabel, BorderLayout.EAST);
        }
        return navbar;
    }

    private void loadTest() {
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                test = apiClient.getTest(classroomCode, testname);
                pdfData = apiClient.getTestPDF(classroomCode, testname);
                totalQuestions = test.questionCount();

                questionOrder = IntStream.range(0, totalQuestions).boxed().collect(Collectors.toList());
                Collections.shuffle(questionOrder);

                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (!test.status().equals("ACTIVE")) {
                        stopPolling();
                        JOptionPane.showMessageDialog(mainFrame, "This test is no longer active.", "Test Ended", JOptionPane.INFORMATION_MESSAGE);
                        mainFrame.returnToMainWindow();
                        return;
                    }
                    buildQuestionNav();
                    renderPage(0);
                    startPolling();
                } catch (Exception e) {
                    pdfLabel.setText("Failed to load test: " + e.getMessage());
                }
            }
        }.execute();
    }

    private int getRealQuestionIndex(int viewIndex) {
        if (questionOrder == null || viewIndex < 0 || viewIndex >= questionOrder.size()) {
            return viewIndex;
        }
        return questionOrder.get(viewIndex);
    }

    private void navigatePage(int newPage) {
        if (newPage >= 0 && newPage < totalQuestions) {
            renderPage(newPage);
        }
    }

    private void renderPage(int pageIndex) {
        currentPage = pageIndex;
        final int realQuestionIndex = getRealQuestionIndex(currentPage);

        pageLabel.setText("Question " + (currentPage + 1) + " of " + totalQuestions);
        prevButton.setEnabled(currentPage > 0);
        nextButton.setEnabled(currentPage < totalQuestions - 1);

        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() {
                int targetWidth = pdfScrollPane.getViewport().getWidth() > 0 ? pdfScrollPane.getViewport().getWidth() - 10 : 800;
                if (targetWidth <= 0) targetWidth = 800;
                return PdfUtil.getScaledPdfPage(pdfData, realQuestionIndex, targetWidth);
            }

            @Override
            protected void done() {
                try {
                    ImageIcon image = get();
                    pdfLabel.setText(null);
                    pdfLabel.setIcon(image);
                    SwingUtilities.invokeLater(() -> pdfScrollPane.getVerticalScrollBar().setValue(0));
                } catch (Exception e) {
                    pdfLabel.setIcon(null);
                    pdfLabel.setText("Failed to render page " + (currentPage + 1));
                }
            }
        }.execute();

        updateAnswerButtons();
        updateQuestionNavHighlight();
    }

    private void selectAnswer(String answer) {
        if (isSubmitting) return;

        answers.put(getRealQuestionIndex(currentPage), answer);

        updateAnswerButtons();
        updateQuestionNavHighlight();

        for (int i = currentPage + 1; i < totalQuestions; i++) {
            if (!answers.containsKey(getRealQuestionIndex(i))) {
                navigatePage(i);
                return;
            }
        }
        if (currentPage + 1 < totalQuestions) {
            navigatePage(currentPage + 1);
        }
    }

    private void updateAnswerButtons() {
        String selected = answers.get(getRealQuestionIndex(currentPage));

        for (Component comp : answerButtonPanel.getComponents()) {
            JButton btn = (JButton) comp;
            if (btn.getText().equals(selected)) {
                btn.setBackground(new Color(37, 99, 235));
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(UIManager.getColor("Button.background"));
                btn.setForeground(UIManager.getColor("Button.foreground"));
            }
        }
    }

    private void buildQuestionNav() {
        questionNavPanel.removeAll();
        for (int i = 0; i < totalQuestions; i++) {
            JButton navBtn = new JButton(String.valueOf(i + 1));
            navBtn.setMargin(new Insets(5, 5, 5, 5));
            navBtn.setFont(new Font("SansSerif", Font.BOLD, 12));
            final int page = i;
            navBtn.addActionListener(e -> navigatePage(page));
            questionNavPanel.add(navBtn);
        }
        updateQuestionNavHighlight();
    }

    private void updateQuestionNavHighlight() {
        for (int i = 0; i < questionNavPanel.getComponentCount(); i++) {
            JButton btn = (JButton) questionNavPanel.getComponent(i);
            int realQuestionIndex = getRealQuestionIndex(i);
            if (answers.containsKey(realQuestionIndex)) {
                btn.setBackground(new Color(22, 163, 74));
                btn.setForeground(Color.WHITE);
            } else if (i == currentPage) {
                btn.setBackground(new Color(37, 99, 235));
                btn.setForeground(Color.WHITE);
            } else {
                btn.setBackground(Color.WHITE);
                btn.setForeground(Color.BLACK);
            }
        }
    }

    public void startPolling() {
        pollingTimer = new Timer(5000, e -> {
            if(!isSubmitting) pollTestStatus();
        });
        pollingTimer.start();
    }

    public void stopPolling() {
        if (pollingTimer != null) {
            pollingTimer.stop();
        }
    }

    private List<String> getAnswersAsList() {
        return IntStream.range(0, totalQuestions)
                .mapToObj(i -> answers.getOrDefault(i, ""))
                .collect(Collectors.toList());
    }

    private void saveAnswers() {
        if (isSubmitting || answers.isEmpty()) return;

        final List<String> answersToSave = getAnswersAsList();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                apiClient.updateTestAnswers(classroomCode, testname, answersToSave);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    System.out.println("Answers auto-saved at " + java.time.LocalTime.now());
                } catch (Exception e) {
                    System.err.println("Failed to auto-save answers: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void pollTestStatus() {
        saveAnswers();

        new SwingWorker<Test, Void>() {
            @Override
            protected Test doInBackground() throws Exception {
                return apiClient.getTest(classroomCode, testname);
            }

            @Override
            protected void done() {
                try {
                    Test updatedTest = get();
                    if (updatedTest.status().equals("ENDED")) {
                        submitTest(false);
                    }
                } catch (Exception e) {
                    System.err.println("Polling error: " + e.getMessage());
                    stopPolling();
                }
            }
        }.execute();
    }

    public void submitTest(boolean isLockout) {
        if (isSubmitting) return;
        isSubmitting = true;
        stopPolling();

        List<String> answersArray = getAnswersAsList();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                apiClient.submitTest(classroomCode, testname, answersArray);
                return null;
            }

            @Override
            protected void done() {
                if (!isLockout) {
                    JOptionPane.showMessageDialog(mainFrame,
                            "The test has been ended by the teacher. Your answers are submitted.",
                            "Test Ended",
                            JOptionPane.INFORMATION_MESSAGE);
                    mainFrame.returnToMainWindow();
                }
                else {
                    JOptionPane.showMessageDialog(
                            mainFrame,
                            "You have been locked out. Your results are submitted.",
                            "Test Locked",
                            JOptionPane.ERROR_MESSAGE
                    );
                    mainFrame.returnToMainWindow();
                }
            }
        }.execute();
    }
}