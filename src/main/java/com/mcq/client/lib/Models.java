package com.mcq.client.lib;

import java.util.List;

public class Models {

    public record User(
            String firstname,
            String lastname,
            String email,
            String username,
            String role
    ) {}

    public record LoginRequest(String username, String password) {}

    public record RegisterRequest(
            String firstname,
            String lastname,
            String email,
            String username,
            String password,
            String role
    ) {}

    public record ClassroomDTO(
            String code,
            String classroomname,
            User classroomteacher,
            List<String> classroomstudents
    ) {}

    public record Test(
            int id,
            String testname,
            String questionsPdfPath,
            List<String> correctAnswers,
            String status,
            ClassroomDTO classroom,
            int questionCount
    ) {}

    public record StudentResultDTO(
            User user,
            List<String> userAnswers,
            List<String> correctAnswers,
            int score,
            int totalQuestions,
            String pdfPath
    ) {}

    public record TeacherResultsDTO(
            List<String> correctAnswers,
            int totalQuestions,
            List<StudentResultDTO> submissions
    ) {}

    public record ApiError(String message) {}
}