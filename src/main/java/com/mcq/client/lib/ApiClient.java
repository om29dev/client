// src/main/java/com/mcq/client/lib/ApiClient.java
package com.mcq.client.lib;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mcq.client.lib.Models.*;

import java.io.IOException;
import java.io.InputStream; // <-- ADDED
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Properties; // <-- ADDED

// Singleton for all API calls
public class ApiClient {

    private static ApiClient instance;
    private final String API_BASE_URL; // <-- MODIFIED (Removed hardcoding)
    private final HttpClient httpClient;
    private final Gson gson;

    private ApiClient() {
        // --- MODIFIED: Load API_BASE_URL from properties ---
        Properties props = new Properties();
        String baseUrl;
        try (InputStream in = ApiClient.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in != null) {
                props.load(in);
                // The property key in application.properties should be 'api.base.url'
                baseUrl = props.getProperty("api.base.url", "http://localhost:8080");
            } else {
                System.err.println("WARNING: application.properties not found. Defaulting to localhost:8080");
                baseUrl = "http://localhost:8080";
            }
        } catch (IOException e) {
            System.err.println("ERROR: Failed to load application.properties. Defaulting to localhost:8080");
            e.printStackTrace();
            baseUrl = "http://localhost:8080";
        }
        this.API_BASE_URL = baseUrl;
        // --- END MODIFICATION ---

        // Enable cookie management to maintain session
        CookieHandler.setDefault(new CookieManager());

        this.httpClient = HttpClient.newBuilder()
                .cookieHandler(CookieHandler.getDefault())
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.gson = new Gson();
    }

    public static synchronized ApiClient getInstance() {
        if (instance == null) {
            instance = new ApiClient();
        }
        return instance;
    }

    private <T> T request(String endpoint, String method, Object body, TypeToken<T> responseType) throws Exception {
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE_URL + endpoint))
                    .header("Content-Type", "application/json");

            if (body != null) {
                String jsonBody = gson.toJson(body);
                requestBuilder.method(method, HttpRequest.BodyPublishers.ofString(jsonBody));
            } else {
                requestBuilder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                // Try to parse error message
                try {
                    ApiError error = gson.fromJson(response.body(), ApiError.class);
                    if (error != null && error.message() != null) {
                        throw new IOException(error.message());
                    }
                    throw new IOException("HTTP Error: " + response.statusCode() + " - " + response.body());
                } catch (Exception e) {
                    if(e instanceof IOException) throw e;
                    throw new IOException("HTTP Error: " + response.statusCode() + " - " + response.body());
                }
            }

            if (response.statusCode() == 204 || response.body().isEmpty()) {
                return null;
            }

            // Fix for the java.lang.Void error
            if (responseType.getType() == Void.class) {
                return null;
            }

            return gson.fromJson(response.body(), responseType);

        } catch (Exception e) {
            // --- THIS IS THE FIX ---
            // Add a null check before calling .contains()
            if (e.getMessage() != null && e.getMessage().contains("java.lang.Void")) {
                throw new Exception("Gson deserialization error: " + e.getMessage(), e);
            }
            // --- END FIX ---
            throw new Exception("API request failed: " + e.getMessage(), e);
        }
    }

    // --- Auth ---
    public void login(String username, String password) throws Exception {
        request("/api/auth/login", "POST", new LoginRequest(username, password), new TypeToken<Void>() {});
    }

    public void register(RegisterRequest req) throws Exception {
        request("/api/auth/register", "POST", req, new TypeToken<Void>() {});
    }

    public void logout() throws Exception {
        request("/api/auth/logout", "POST", null, new TypeToken<Void>() {});
    }

    public User getUserByUsername(String username) throws Exception {
        return request("/api/users/" + username, "GET", null, new TypeToken<User>() {});
    }

    // --- Classrooms ---
    public List<ClassroomDTO> getClassrooms(String filter) throws Exception {
        String query = (filter != null) ? "?filter=" + filter : "";
        return request("/api/classrooms" + query, "GET", null, new TypeToken<List<ClassroomDTO>>() {});
    }

    public ClassroomDTO getClassroom(String code) throws Exception {
        return request("/api/classrooms/" + code, "GET", null, new TypeToken<ClassroomDTO>() {});
    }

    public ClassroomDTO createClassroom(String classroomName) throws Exception {
        return request("/api/classrooms", "POST", Map.of("classroomname", classroomName), new TypeToken<ClassroomDTO>() {});
    }

    public void joinClassroom(String code) throws Exception {
        request("/api/classrooms/" + code + "/join", "POST", null, new TypeToken<Void>() {});
    }

    public void removeStudent(String code, String studentUsername) throws Exception {
        request("/api/classrooms/" + code + "/remove/" + studentUsername, "DELETE", null, new TypeToken<Void>() {});
    }

    // --- Tests ---
    public List<Test> getTests(String classroomCode) throws Exception {
        return request("/api/classrooms/" + classroomCode + "/tests", "GET", null, new TypeToken<List<Test>>() {});
    }

    public Test getTest(String classroomCode, String testname) throws Exception {
        return request("/api/classrooms/" + classroomCode + "/tests/" + testname, "GET", null, new TypeToken<Test>() {});
    }

    public void startTest(String classroomCode, String testname) throws Exception {
        request("/api/classrooms/" + classroomCode + "/tests/" + testname + "/start", "POST", null, new TypeToken<Void>() {});
    }

    public void endTest(String classroomCode, String testname) throws Exception {
        request("/api/classrooms/" + classroomCode + "/tests/" + testname + "/end", "POST", null, new TypeToken<Void>() {});
    }

    public void deleteTest(String classroomCode, String testname) throws Exception {
        request("/api/classrooms/" + classroomCode + "/tests/" + testname, "DELETE", null, new TypeToken<Void>() {});
    }

    public byte[] getTestPDF(String classroomCode, String testname) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/api/classrooms/" + classroomCode + "/tests/" + testname + "/pdf"))
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, BodyHandlers.ofByteArray());

        if (response.statusCode() >= 400) {
            throw new IOException("Failed to download PDF: " + response.statusCode());
        }
        return response.body();
    }

    public void submitTest(String classroomCode, String testname, List<String> answers) throws Exception {
        request("/api/classrooms/" + classroomCode + "/tests/" + testname + "/submissions/submit", "POST", answers, new TypeToken<Void>() {});
    }

    public Test getActiveTestForStudent() throws Exception {
        return request("/api/classrooms/student/active-test", "GET", null, new TypeToken<Test>() {});
    }

    public StudentResultDTO getMySubmission(String classroomCode, String testname) throws Exception {
        return request("/api/classrooms/" + classroomCode + "/tests/" + testname + "/submissions/my", "GET", null, new TypeToken<StudentResultDTO>() {});
    }

    public TeacherResultsDTO getAllSubmissions(String classroomCode, String testname) throws Exception {
        return request("/api/classrooms/" + classroomCode + "/tests/" + testname + "/submissions", "GET", null, new TypeToken<TeacherResultsDTO>() {});
    }

    // This method is special as it handles FormData, not JSON
    public Test createTest(String classroomCode, String testname, java.io.File pdfFile, List<String> correctAnswers) throws Exception {
        String boundary = "Boundary-" + System.currentTimeMillis();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_BASE_URL + "/api/classrooms/" + classroomCode + "/tests"))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(ofMimeMultipartData(testname, pdfFile, correctAnswers, boundary))
                .build();

        HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());

        if (response.statusCode() >= 400) {
            try {
                ApiError error = gson.fromJson(response.body(), ApiError.class);
                throw new IOException(error.message());
            } catch (Exception e) {
                throw new IOException("HTTP Error: " + response.statusCode() + " - " + response.body());
            }
        }

        return gson.fromJson(response.body(), Test.class);
    }

    // Helper for createTest to build the multipart body
    private HttpRequest.BodyPublisher ofMimeMultipartData(String testname, java.io.File pdfFile, List<String> correctAnswers, String boundary) throws IOException {
        var byteArrays = new java.util.ArrayList<byte[]>();
        String crlf = "\r\n";

        // Test name part
        byteArrays.add(("--" + boundary + crlf).getBytes());
        byteArrays.add(("Content-Disposition: form-data; name=\"testname\"" + crlf + crlf).getBytes());
        byteArrays.add((testname + crlf).getBytes());

        // PDF file part
        byteArrays.add(("--" + boundary + crlf).getBytes());
        byteArrays.add(("Content-Disposition: form-data; name=\"pdfFile\"; filename=\"" + pdfFile.getName() + "\"" + crlf).getBytes());
        byteArrays.add(("Content-Type: application/pdf" + crlf + crlf).getBytes());
        byteArrays.add(java.nio.file.Files.readAllBytes(pdfFile.toPath()));
        byteArrays.add((crlf).getBytes());

        // Correct answers parts
        for (String answer : correctAnswers) {
            byteArrays.add(("--" + boundary + crlf).getBytes());
            byteArrays.add(("Content-Disposition: form-data; name=\"correctAnswers\"" + crlf + crlf).getBytes());
            byteArrays.add((answer + crlf).getBytes());
        }

        // End boundary
        byteArrays.add(("--" + boundary + "--" + crlf).getBytes());

        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }
}