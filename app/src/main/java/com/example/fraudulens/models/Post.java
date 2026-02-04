package com.example.fraudulens.models;

public class Post {
    private String id;
    private String userId;
    private String userName;
    private String userPhotoUrl;
    private String caption;
    private String imageUrl;
    private com.google.firebase.Timestamp timestamp;
    private int likeCount;
    private int commentCount;
    private int shareCount;
    private java.util.List<String> likes;
    private java.util.Map<String, Object> sharedPost;
    private String sharedPostId;

    public Post() {
        // Empty constructor required for Firestore serialization
    }

    public Post(String userId, String userName, String userPhotoUrl, String caption, String imageUrl,
                com.google.firebase.Timestamp timestamp, int likeCount, int commentCount, int shareCount,
                java.util.List<String> likes, java.util.Map<String, Object> sharedPost, String sharedPostId) {
        this.userId = userId;
        this.userName = userName;
        this.userPhotoUrl = userPhotoUrl;
        this.caption = caption;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.shareCount = shareCount;
        this.likes = likes;
        this.sharedPost = sharedPost;
        this.sharedPostId = sharedPostId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserPhotoUrl() { return userPhotoUrl; }
    public void setUserPhotoUrl(String userPhotoUrl) { this.userPhotoUrl = userPhotoUrl; }

    public String getCaption() { return caption; }
    public void setCaption(String caption) { this.caption = caption; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public com.google.firebase.Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(com.google.firebase.Timestamp timestamp) { this.timestamp = timestamp; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public int getShareCount() { return shareCount; }
    public void setShareCount(int shareCount) { this.shareCount = shareCount; }

    public java.util.List<String> getLikes() { return likes; }
    public void setLikes(java.util.List<String> likes) { this.likes = likes; }

    public java.util.Map<String, Object> getSharedPost() { return sharedPost; }
    public void setSharedPost(java.util.Map<String, Object> sharedPost) { this.sharedPost = sharedPost; }

    public String getSharedPostId() { return sharedPostId; }
    public void setSharedPostId(String sharedPostId) { this.sharedPostId = sharedPostId; }

    public java.util.Map<String, Object> toMap() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("userPhotoUrl", userPhotoUrl);
        map.put("caption", caption);
        map.put("imageUrl", imageUrl);
        map.put("timestamp", timestamp != null ? timestamp : com.google.firebase.Timestamp.now());
        map.put("likeCount", likeCount);
        map.put("commentCount", commentCount);
        map.put("shareCount", shareCount);
        map.put("likes", likes != null ? likes : new java.util.ArrayList<>());
        if (sharedPost != null) map.put("sharedPost", sharedPost);
        if (sharedPostId != null) map.put("sharedPostId", sharedPostId);
        return map;
    }
}
