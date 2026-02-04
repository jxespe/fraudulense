package com.example.fraudulens.models;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a user-submitted fraud or scam report in Firestore.
 */
public class Report {
    private String id;
    private String userId;
    private String message;   // text or scam message
    private String result;    // detection result (e.g., "Phishing", "Legit")
    private Timestamp timestamp;
    private String status;    // e.g., "Open", "Resolved", "Under Review"
    private String source;    // sender/source for SMS-based reports

    public Report() {
        // Needed for Firestore deserialization
    }

    public Report(String userId, String message, String result, Timestamp timestamp, String status) {
        this.userId = userId;
        this.message = message;
        this.result = result;
        this.timestamp = timestamp;
        this.status = status;
    }

    // --- Getters and Setters ---
    public String getId() { return id; }
    public void setId(String id) {
        this.id = id;
    }


    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    // --- Firestore map conversion ---
    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("userId", userId);
        m.put("message", message);
        m.put("result", result);
        m.put("timestamp", timestamp != null ? timestamp : Timestamp.now());
        m.put("status", status);
        if (source != null) m.put("source", source);
        return m;
    }
}
