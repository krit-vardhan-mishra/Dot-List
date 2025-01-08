package com.just_for_fun.dotlist;

import android.net.Uri;

public class Task {

    private int id;
    private String title;
    private boolean isCompleted;
    private TaskDetails details;

    private String notes;
    private Uri fileUri;
    private long timestamp;
    private int position;
    private String content;

    public Task() {
        this.details = new TaskDetails();
    }

    // Constructor for database integration (with ID)
    public Task(int id, String title, boolean isCompleted, TaskDetails details) {
        this.id = id;
        this.title = title;
        this.isCompleted = isCompleted;
        this.details = details != null ? details : new TaskDetails();
    }

    public Task(int id, String title, boolean isCompleted, TaskDetails details, long timestamp) {
        this.id = id;
        this.title = title;
        this.isCompleted = isCompleted;
        this.details = details != null ? details : new TaskDetails();
        this.timestamp = timestamp;
    }

    public Task(int id, String content) {
        this.id = id;
        this.content = content;
    }

    // Constructor for unsaved tasks (no ID)
    public Task(String title, boolean isCompleted) {
        this.id = -1; // Temporary ID until saved in DB
        this.title = title;
        this.isCompleted = isCompleted;
        this.details = new TaskDetails(); // Default initialization
    }

    // Getters and Setters
    public int getId() { return id; }

    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public boolean isCompleted() { return isCompleted; }

    public void setCompleted(boolean completed) { this.isCompleted = completed; }

    public TaskDetails getDetails() {
        if (details == null) {
            details = new TaskDetails();
        }
        return details;
    }

    public void setDetails(TaskDetails details) { this.details = details; }

    public String getFilePath() { return details.getFilePath(); }

    public  void setFilePath(String filePath) { this.details.setFilePath(filePath); }

    public long getTimestamp() { return timestamp; }

    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getPosition() { return position; }

    public void setPosition(int position) { this.position = position; }

    public String getContent() { return content; }

    public void setContent(String content) { this.content = content; }

}
