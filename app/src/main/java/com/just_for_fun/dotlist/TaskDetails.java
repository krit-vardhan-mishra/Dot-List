package com.just_for_fun.dotlist;

import androidx.annotation.NonNull;

public class TaskDetails {

    private String notes;
    private String filePath;

    public TaskDetails() {}

    public TaskDetails(String notes, String filePath) {
        this.notes = notes;
        this.filePath = filePath;
    }

    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes;
    }
    public String getFilePath() {
        return filePath;
    }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @NonNull
    @Override
    public String toString() {
        return "Notes: " + notes + ", File Path: " + filePath;
    }
}