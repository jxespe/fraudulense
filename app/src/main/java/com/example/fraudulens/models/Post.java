package com.example.fraudulens.models;

public class Post {
    private String username;
    private String caption;
    private String imageUrl;
    private String date;

    public Post() {
        // Empty constructor required for Firestore serialization
    }

    public Post(String username, String caption, String imageUrl, String date) {
        this.username = username;
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.date = date;
    }

    public String getUsername() { return username; }
    public String getCaption() { return caption; }
    public String getImageUrl() { return imageUrl; }
    public String getDate() { return date; }

    public void setUsername(String username) { this.username = username; }
    public void setCaption(String caption) { this.caption = caption; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setDate(String date) { this.date = date; }
}
