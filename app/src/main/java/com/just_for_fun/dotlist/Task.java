package com.just_for_fun.dotlist;

public class Task {

    private int id;
    private String title;
    private boolean isCompleted;
    private TaskDetails details;

    // Constructor for database integration (with ID)
    public Task(int id, String title, boolean isCompleted, TaskDetails details) {
        this.id = id;
        this.title = title;
        this.isCompleted = isCompleted;
        this.details = details != null ? details : new TaskDetails();
    }

    // Constructor for unsaved tasks (no ID)
    public Task(String title, boolean isCompleted) {
        this.id = -1; // Temporary ID until saved in DB
        this.title = title;
        this.isCompleted = isCompleted;
        this.details = new TaskDetails(); // Default initialization
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    public TaskDetails getDetails() {
        if (details == null) {
            details = new TaskDetails();
        }
        return details;
    }

    public void setDetails(TaskDetails details) {
        this.details = details;
    }

    public String getFilePath() {
        return details.getFilePath();
    }

    public  void setFilePath(String filePath) {
        this.details.setFilePath(filePath);
    }

}
