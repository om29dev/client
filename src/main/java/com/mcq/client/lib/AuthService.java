// src/main/java/com/mcq/client/lib/AuthService.java
package com.mcq.client.lib;

import com.mcq.client.lib.Models.User;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.net.CookieHandler;
import java.net.CookieManager;

// Singleton class to manage authentication state
public class AuthService {
    private static AuthService instance;
    private User user;
    private final PropertyChangeSupport support;
    private final ApiClient apiClient;

    private AuthService() {
        this.support = new PropertyChangeSupport(this);
        this.apiClient = ApiClient.getInstance();

        // In a real app, you might load a saved token/user from disk here
    }

    public static synchronized AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    public User getUser() {
        return user;
    }

    public boolean isTeacher() {
        if (user == null) return false;
        return user.role().equals("ROLE_TEACHER") || user.role().equals("ROLE_ADMIN");
    }

    public void setUser(User newUser) {
        User oldUser = this.user;
        this.user = newUser;
        support.firePropertyChange("user", oldUser, newUser);
    }

    public void addPropertyChangeListener(PropertyChangeListener pcl) {
        support.addPropertyChangeListener(pcl);
    }

    public void removePropertyChangeListener(PropertyChangeListener pcl) {
        support.removePropertyChangeListener(pcl);
    }

    // --- Authentication Methods ---

    public void login(String username, String password) throws Exception {
        apiClient.login(username, password);
        User userData = apiClient.getUserByUsername(username);
        setUser(userData);
        // ApiClient's CookieManager will store the session cookie
    }

    public void register(Models.RegisterRequest request) throws Exception {
        apiClient.register(request);
    }

    public void logout() {
        try {
            apiClient.logout();
        } catch (Exception e) {
            System.err.println("Failed to logout on server: " + e.getMessage());
        } finally {
            setUser(null);
            // Re-create the cookie manager to clear session
            CookieHandler.setDefault(new CookieManager());
        }
    }
}