package com.just_for_fun.dotlist;

import android.net.Uri;

import java.io.Serializable;

public class Task implements Serializable {

    private int id;
    private boolean isCompleted;
    private TaskDetails details;
    private String notes;
    private Uri fileUri;
    private long timestamp;
    private int position;
    private String content;
    private String title;

    public Task() {
        this.details = new TaskDetails();
    }

    public Task(int id, String content) {
        this.id = id;
        this.content = content;
    }

    public Task(int id, String content, String title) {
        this.id = id;
        this.content = content;
        this.title = title;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
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
    public void setFilePath(String filePath) { this.details.setFilePath(filePath); }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public Uri getFileUri() { return fileUri; }
    public void setFileUri(Uri fileUri) { this.fileUri = fileUri; }

}