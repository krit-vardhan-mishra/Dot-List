package com.just_for_fun.dotlist;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class MainActivity extends AppCompatActivity {

    private LinearLayout taskContainer;
    private int currentCapacity = 5;
    private final List<Task> tasks = new ArrayList<>();
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DBHelper(this);
        taskContainer = findViewById(R.id.taskContainer);

        // Load Tasks for Database
        loadTasksFromDatabase();

        // Add initial input boxes
        for (int i = 0; i < currentCapacity; i++) {
            taskContainer.addView(getLayoutInflater().inflate(R.layout.task_row, taskContainer, false));
        }

        // Add text changed listener to each EditText
        addTextWatchersToTasks();
    }

    private void loadTasksFromDatabase() {
        List<Task> savedTasks = dbHelper.getAllTasks();
        for (Task task : savedTasks) {
            addTaskToUI(task);
            tasks.add(task);
        }
    }

    private void addTaskToUI(Task task) {
        View row = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);
        EditText editText = row.findViewById(R.id.taskEditText);
        CheckBox checkBox = row.findViewById(R.id.taskCheckbox);

        editText.setText(task.getTitle());
        checkBox.setChecked(task.isCompleted());

        checkBox.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            task.setCompleted(isChecked);
            dbHelper.updateTask(task.getId(), isChecked, task.getDetails().getFilePath());
        }));

        taskContainer.addView(row);
    }

    private void addTextWatchersToTasks() {
        for (int i = 0; i < taskContainer.getChildCount(); i++) {
            View row = taskContainer.getChildAt(i);
            EditText editText = row.findViewById(R.id.taskEditText);
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    checkAndAddRows();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void checkAndAddRows() {
        int filledBoxes = 0;
        for (int i = 0; i < taskContainer.getChildCount(); i++) {
            View row = taskContainer.getChildAt(i);
            EditText editText = row.findViewById(R.id.taskEditText);
            if (!editText.getText().toString().isEmpty()) {
                filledBoxes++;
            }
        }

        double fillPercentage = (double) filledBoxes / currentCapacity * 100;
        if (fillPercentage >= 80) {
            // Double the capacity
            currentCapacity *= 2;
            for (int i = 0; i < currentCapacity - taskContainer.getChildCount(); i++) {
                taskContainer.addView(getLayoutInflater().inflate(R.layout.task_row, taskContainer, false));
            }
            addTextWatchersToTasks();
        }
    }

    private void addNewTaskRow() {
        View row = getLayoutInflater().inflate(R.layout.task_row, taskContainer, false);
        EditText editText = row.findViewById(R.id.taskEditText);
        CheckBox checkBox = row.findViewById(R.id.taskCheckbox);

        checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Task newTask = new Task(editText.getText().toString(), isChecked);
            dbHelper.insertTask(newTask.getTitle(), isChecked);
            tasks.add(newTask);
        });

        taskContainer.addView(row);
    }

    // Handle file attachment
    public void onFileAttached(View view) {
        int taskIndex = getTaskIndexFromView(view);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(intent, taskIndex);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri selectedFileUri = data.getData();
            Task task = getTaskAtIndex(requestCode);

            if (task != null) {
                task.getDetails().setFilePath(selectedFileUri.toString());

                // Update the UI to indicate file attachment
                View row = taskContainer.getChildAt(requestCode);
                try {
                    ImageView attachmentIcon = row.findViewById(R.id.attachmentIcon);
                    attachmentIcon.setVisibility(View.VISIBLE); // Show the icon
                } catch (NoSuchElementException e) {
                    e.printStackTrace();
                }
                dbHelper.updateTask(task.getId(), task.isCompleted(), task.getDetails().getFilePath());
            }
            // TODO: Update UI to indicate file attachment.
        }
    }

    private int getTaskIndexFromView(View view) {
        LinearLayout parentRow = (LinearLayout) view.getParent();
        return taskContainer.indexOfChild(parentRow);
    }

    private Task getTaskAtIndex(int index) {
        if (index >= 0 && index < tasks.size()) {
            return tasks.get(index);
        }
        return null;
    }
}
