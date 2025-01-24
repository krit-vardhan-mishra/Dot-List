package com.just_for_fun.dotlist;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TaskAdapter taskAdapter;
    private final List<Task> tasks = new ArrayList<>();
    private FloatingActionButton addTaskButton;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerView();
        loadAppData();
        setupFloatingActionButton();
    }

    private void initializeViews() {
        recyclerView = findViewById(R.id.recycler_view);
        addTaskButton = findViewById(R.id.addTask);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter(tasks, this);
        recyclerView.setAdapter(taskAdapter);
    }

    private void setupFloatingActionButton() {
        addTaskButton.setOnClickListener(v -> {
            tasks.add(new Task(tasks.size(), ""));
            taskAdapter.notifyItemInserted(tasks.size() - 1);
            recyclerView.smoothScrollToPosition(tasks.size() - 1);
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        saveAppData();
    }

    private void saveAppData() {
        SharedPreferences preferences = getSharedPreferences("AppData", MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();

        Gson gson = new Gson();
        editor.putString("tasks", gson.toJson(tasks));
        editor.apply();
    }

    private void loadAppData() {
        SharedPreferences preferences = getSharedPreferences("AppData", MODE_PRIVATE);
        String tasksJson = preferences.getString("tasks", "");

        Gson gson = new Gson();
        Type taskListType = new TypeToken<ArrayList<Task>>(){}.getType();
        List<Task> savedTasks = gson.fromJson(tasksJson, taskListType);

        if (savedTasks != null) {
            tasks.clear();
            tasks.addAll(savedTasks);
            taskAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        saveAppData();
        super.onDestroy();
    }
}

//
//public class MainActivity extends AppCompatActivity implements TaskAdapter.OnTaskClickListener {
//
//    private static final String KEY_NOTES = "notes";
//    private static final String KEY_FILE_URI = "file_uri";
//    private static final String STATE_SCROLL_POSITION = "scroll_position";
//
//    private EditText notesArea;
//    private ImageButton upArrow;
//    private EditText taskEditText;
//    private ImageButton deleteButton;
//    private ImageButton uploadButton;
//    private ImageButton previewButton;
//    private ConstraintLayout notesLayout;
//
//    private RecyclerView recyclerView;
//    private TaskAdapter taskAdapter;
//    private final List<Task> tasks = new ArrayList<>();
//    private FloatingActionButton addTaskButton;
//
//    private ActivityResultLauncher<Intent> filePickerLauncher;
//    private Uri selectedFileUri;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        initializeViews();
//        setupRecyclerView();
//        loadAppData();
//        setupInitialState(savedInstanceState);
//        setupClickListeners();
//        setupFilePicker();
//        setupFloatingActionButton();
//
//        previewButton.setOnLongClickListener(v -> {
//            deleteButton.setVisibility(View.VISIBLE);
//            return true;
//        });
//
//        deleteButton.setOnClickListener(v -> {
//            removeUploadedFile();
//        });
//    }
//
//    private void setupFloatingActionButton() {
//        addTaskButton.setOnClickListener(v -> {
//            Task newTask = new Task();
//            tasks.add(newTask);
//
//            taskAdapter.notifyItemInserted(tasks.size() - 1);
//            recyclerView.smoothScrollToPosition(tasks.size() - 1);
//        });
//    }
//
//    private void initializeViews() {
//        upArrow = findViewById(R.id.up_arrow);
//        notesArea = findViewById(R.id.notesArea);
//        notesLayout = findViewById(R.id.notesLayout);
//        taskEditText = findViewById(R.id.taskEditText);
//        uploadButton = findViewById(R.id.uploadButton);
//        previewButton = findViewById(R.id.previewButton);
//        recyclerView = findViewById(R.id.recycler_view);
//        deleteButton = findViewById(R.id.deleteButton);
//        addTaskButton = findViewById(R.id.addTask);
//    }
//
//    private void setupRecyclerView() {
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        taskAdapter = new TaskAdapter(tasks, this, this);
//        recyclerView.setAdapter(taskAdapter);
//    }
//
//    @Override
//    public void onTaskTextChanged(int position, String text) {
//        Task task = tasks.get(position);
//        task.setContent(text);
//    }
//
//    private void setupInitialState(Bundle savedInstanceState) {
//        if (savedInstanceState != null) {
//            restoreState(savedInstanceState);
//        } else {
//            notesLayout.setVisibility(View.GONE);
//            upArrow.setVisibility(View.GONE);
//            previewButton.setVisibility(View.GONE);
//
//            if (tasks.isEmpty()) {
//                for (int i = 0; i < 5; i++) {
//                    tasks.add(new Task(i, ""));
//                }
//
//                taskAdapter.notifyDataSetChanged();
//            }
//        }
//    }
//
//    private void setupClickListeners() {
//        uploadButton.setOnClickListener(v -> openFilePicker());
//        previewButton.setOnClickListener(v -> showFilePreview());
//    }
//
//
//    private void setupFilePicker() {
//        filePickerLauncher = registerForActivityResult(
//                new ActivityResultContracts.StartActivityForResult(),
//                result -> {
//                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
//                        handleFileSelection(result.getData().getData());
//                    }
//                }
//        );
//    }
//
//    private void handleFileSelection(Uri uri) {
//        if (uri == null) {
//            showError("Invalid file selection.");
//            return;
//        }
//
//        try {
//            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
//            selectedFileUri = uri;
//            updateFilePreview();
//        } catch (Exception e) {
//            showError("Failed to select file: " + e.getMessage());
//        }
//    }
//
//    private void updateFilePreview() {
//        uploadButton.setVisibility(View.GONE);
//        previewButton.setVisibility(View.VISIBLE);
//
//        try {
//            // Check if the selected file is an image
//            String mimeType = getContentResolver().getType(selectedFileUri);
//            if (mimeType != null && mimeType.startsWith("image/")) {
//                // Show loading spinner during image loading
//                previewButton.setImageResource(R.drawable.ic_loading_spinner);
//
//                // Load the image using Glide
//                Glide.with(this)
//                        .load(selectedFileUri)
//                        .placeholder(R.drawable.ic_loading_spinner) // Shows a spinner while loading
//                        .error(R.drawable.ic_file_error)           // Shows an error icon if loading fails
//                        .into(previewButton);
//            } else {
//                // If not an image, show the preview icon directly
//                previewButton.setImageResource(R.drawable.preview_icon);
//            }
//        } catch (Exception e) {
//            // Show a text message if an error occurs
//            previewButton.setImageResource(R.drawable.preview_icon); // Fallback error icon
//            Toast.makeText(this, "Failed to load file preview.", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    private void showFilePreview() {
//        if (selectedFileUri != null) {
//            try {
//                Intent intent = new Intent(Intent.ACTION_VIEW);
//                intent.setData(selectedFileUri);
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                startActivity(intent);
//            } catch (Exception e) {
//                showError("Cannot open file: " + e.getMessage());
//            }
//        }
//    }
//
//    private void removeUploadedFile() {
//        selectedFileUri = null; // Clear the reference
//        // Update UI if needed
//        previewButton.setVisibility(View.GONE);
//        deleteButton.setVisibility(View.GONE);
//        uploadButton.setVisibility(View.VISIBLE);
//        Toast.makeText(this, "File removed successfully", Toast.LENGTH_SHORT).show();
//    }
//
//    private void showError(String message) {
//        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
//    }
//
//    @Override
//    protected void onSaveInstanceState(@NonNull Bundle outState) {
//        super.onSaveInstanceState(outState);
//        outState.putString(KEY_NOTES, notesArea.getText().toString());
//        outState.putInt(STATE_SCROLL_POSITION, recyclerView.getScrollY());
//        if (selectedFileUri != null) {
//            outState.putString(KEY_FILE_URI, selectedFileUri.toString());
//        }
//    }
//
//    private void restoreState(@NonNull Bundle savedInstanceState) {
//        notesArea.setText(savedInstanceState.getString(KEY_NOTES, ""));
//
//        String uriString = savedInstanceState.getString(KEY_FILE_URI);
//        if (uriString != null) {
//            selectedFileUri = Uri.parse(uriString);
//            updateFilePreview();
//        }
//    }
//
//    @Override
//    public void onConfigurationChanged(@NonNull Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//
//        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//            handleLandscapeMode();
//        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//            handlePortraitMode();
//        }
//
//        if (taskAdapter != null) {
//            taskAdapter.notifyDataSetChanged();
//        }
//    }
//
//    private void handleLandscapeMode() {
//        // Adjust RecyclerView margins
//        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();
//        params.setMargins(dpToPx(16), dpToPx(8), dpToPx(16), dpToPx(8));
//        recyclerView.setLayoutParams(params);
//
//        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
//        if (layoutManager instanceof LinearLayoutManager) {
//            for (int i = 0; i < taskAdapter.getItemCount(); i++) {
//                View itemView = layoutManager.findViewByPosition(i);
//                if (itemView != null) {
//                    ViewGroup.LayoutParams itemParams = itemView.getLayoutParams();
//                    itemParams.width = getResources().getDimensionPixelSize(R.dimen.task_width_landscape);
//                    itemView.setLayoutParams(itemParams);
//                }
//            }
//        }
//    }
//
//    private void handlePortraitMode() {
//        // Reset RecyclerView margins
//        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();
//        params.setMargins(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
//        recyclerView.setLayoutParams(params);
//
//        // Optionally, reset item width for portrait mode
//        RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
//        if (layoutManager instanceof LinearLayoutManager) {
//            // Reset item width to match parent
//            for (int i = 0; i < taskAdapter.getItemCount(); i++) {
//                View itemView = layoutManager.findViewByPosition(i);
//                if (itemView != null) {
//                    ViewGroup.LayoutParams itemParams = itemView.getLayoutParams();
//                    itemParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
//                    itemView.setLayoutParams(itemParams);
//                }
//            }
//        }
//    }
//
//    private int dpToPx(int dp) {
//        return (int) (dp * getResources().getDisplayMetrics().density);
//    }
//
//    private void openFilePicker() {
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//        intent.setType("*/*");
//        String[] mimeTypes = {"application/pdf", "text/plain"};
//        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        filePickerLauncher.launch(intent);
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        saveAppData(); // Save data when the application is closing
//    }
//
//    private void saveAppData() {
//        SharedPreferences preferences = getSharedPreferences("AppData", MODE_PRIVATE);
//        SharedPreferences.Editor editor = preferences.edit();
//        editor.putString("title", taskEditText.getText().toString());
//        editor.putString("notes", notesArea.getText().toString()); // Save notes
//        editor.apply();
//    }
//
//    private void loadAppData() {
//        SharedPreferences preferences = getSharedPreferences("AppData", MODE_PRIVATE);
//        String savedNotes = preferences.getString("notes", "");
//        String savedTitle = preferences.getString("title", "");
//        notesArea.setText(savedNotes);
//        taskEditText.setText(savedTitle);
//    }
//}

//    private void saveTaskToDatabase(int index, String content) {
//        try {
//            Task task = new Task(index, content);
//            task.setPosition(index);
//            boolean taskExists = false;
//            for (Task existingTask : tasks) {
//                if (existingTask.getPosition() == index) {
//                    taskExists = true;
//                    break;
//                }
//            }
//
//            if (taskExists) {
//                dbHelper.updateTaskContent(index, content);
//            } else {
//                dbHelper.insertTask(task);
//            }
//        } catch (Exception e) {
//            Log.e("MainActivity", "Error saving task: " + e.getMessage());
//            showError("Failed to save task.");
//        }
//    }

//    public void deleteTaskFromDatabase(int index) {
//        dbHelper.deleteTask(index);
//    }

//private int getEditTextIndex(EditText editText) {
//    // Get the parent view of the EditText (the task row)
//    View taskRow = (View) editText.getParent();
//
//    // Get the RecyclerView's adapter position for this task row
//    RecyclerView.ViewHolder viewHolder = recyclerView.getChildViewHolder(taskRow);
//    if (viewHolder != null) {
//        return viewHolder.getAdapterPosition();
//    }
//
//    return -1;
//}

//    private void populateTaskContainers() {
//        tasks.clear();
//        tasks.addAll(dbHelper.getAllTasks());
//        taskAdapter.notifyDataSetChanged(); // Refresh RecyclerView
//    }


//    private long insertTask(Task task) {
//        return dbHelper.insertTask(task); // Insert into database
//    }
//
//    private void updateTaskContent(int taskId, String content) {
//        dbHelper.updateTaskContent(taskId, content); // Update in database
//    }

//    private void initializeDatabase() {
//        dbHelper = new DBHelper(this);
//        tasks.addAll(dbHelper.getAllTasks()); // Load tasks from the database
//        taskAdapter.notifyDataSetChanged(); // Refresh the adapter
//    }