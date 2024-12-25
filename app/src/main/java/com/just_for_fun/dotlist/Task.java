package com.just_for_fun.dotlist;

public class Task {

    private int id;
    private String title;
    private boolean isCompleted;
    private TaskDetails details;
    private String filePath;

    // Constructor for database integration (with ID)
    public Task(int id, String title, boolean isCompleted, String filePath) {
        this.id = id;
        this.title = title;
        this.isCompleted = isCompleted;
        this.details = new TaskDetails(); // Initialize TaskDetails object
        this.filePath = filePath;
    }

    // Constructor for existing tasks with details (loaded from DB)
    public Task(int id, String title, boolean isCompleted, TaskDetails details) {
        this.id = id;
        this.title = title;
        this.isCompleted = isCompleted;
        this.details = details;
    }

    // **New Constructor for unsaved tasks (no ID)**
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
        return details;
    }

    public void setDetails(TaskDetails details) {
        this.details = details;
    }

    public String getFilePath() {
        return filePath;
    }

    public  void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
