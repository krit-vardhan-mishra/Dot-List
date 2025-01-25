package com.just_for_fun.dotlist.Task;

import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

public class Task {
    private long id;
    private String title;
    private String notes;
    private boolean isChecked;
    private boolean isExpanded;
    private Uri fileUri;
    private boolean showDeleteFileBtn;
    private List<NoteFormatting> notesFormatting = new ArrayList<>();
    private long deletionTime = -1;

    public Task() {
        this.title = "";
        this.notes = "";
        this.isExpanded = false;
        this.fileUri = null;
        this.showDeleteFileBtn = false;
        this.isChecked = false;
    }

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public long getDeletionTime() { return deletionTime; }
    public void setDeletionTime(long deletionTime) { this.deletionTime = deletionTime; }
    public boolean isChecked() { return isChecked; }
    public void setChecked(boolean checked) { isChecked = checked; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { isExpanded = expanded; }
    public Uri getFileUri() { return fileUri; }
    public void setFileUri(Uri fileUri) { this.fileUri = fileUri; }
    public boolean isShowDeleteFileBtn() { return showDeleteFileBtn; }
    public void setShowDeleteFileBtn(boolean showDeleteFileBtn) { this.showDeleteFileBtn = showDeleteFileBtn; }
    public List<NoteFormatting> getNotesFormatting() { return notesFormatting; }
    public void setNotesFormatting(List<NoteFormatting> notesFormatting) { this.notesFormatting = notesFormatting; }
}