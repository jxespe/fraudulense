package com.example.fraudulens.models;

import com.google.firebase.Timestamp;

public class ActivityLog {
    private String id;
    private String user;
    private String action;
    private Timestamp timestamp;

    public ActivityLog() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUser() { return user; }
    public void setUser(String user) { this.user = user; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
}
