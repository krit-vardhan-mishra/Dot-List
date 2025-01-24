package com.just_for_fun.dotlist;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> tasks;
    private final Context context;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    public TaskAdapter(List<Task> tasks, Context context) {
        this.tasks = tasks;
        this.context = context;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_row, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task, position);

        holder.itemView.setOnLongClickListener(v -> {
            showDeleteDialog(position);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            showDeleteDialog(position);
        });
    }

    private void showDeleteDialog(int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Delete Task");
        builder.setMessage("Are you sure you want to delete this task?");
        builder.setPositiveButton("Delete", (dialog, which) -> {
            tasks.remove(position);
            notifyItemRemoved(position);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    class TaskViewHolder extends RecyclerView.ViewHolder {
        private final EditText taskEditText, notesArea;
        private final ImageButton downArrow, upArrow, uploadButton, previewButton, deleteButton;
        private final ConstraintLayout notesLayout;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskEditText = itemView.findViewById(R.id.taskEditText);
            notesArea = itemView.findViewById(R.id.notesArea);
            itemView.findViewById(R.id.taskCheckbox);
            downArrow = itemView.findViewById(R.id.down_arrow);
            upArrow = itemView.findViewById(R.id.up_arrow);
            uploadButton = itemView.findViewById(R.id.uploadButton);
            previewButton = itemView.findViewById(R.id.previewButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            notesLayout = itemView.findViewById(R.id.notesLayout);

            setupFilePicker();
        }

        void bind(Task task, int position) {
            taskEditText.setText(task.getContent());
            notesArea.setText(task.getNotes());

            if (task.getFileUri() != null) {
                updateFilePreview(task.getFileUri());
            }

            setupTextListeners(task, position);
            setupArrowClickListeners();
            setupFileButtons(task);
        }

        private void setupTextListeners(Task task, int position) {
            taskEditText.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    task.setContent(s.toString());
                }
            });

            notesArea.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void afterTextChanged(Editable s) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    task.setNotes(s.toString());
                }
            });
        }

        private void setupFileButtons(Task task) {
            uploadButton.setOnClickListener(v -> openFilePicker());
            previewButton.setOnClickListener(v -> showFilePreview(task));
            deleteButton.setOnClickListener(v -> removeUploadedFile(task));
        }

        private void setupFilePicker() {
            filePickerLauncher = ((MainActivity) context).registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            tasks.get(getAdapterPosition()).setFileUri(uri);
                            updateFilePreview(uri);
                        }
                    }
            );
        }

        private void updateFilePreview(Uri uri) {
            try {
                uploadButton.setVisibility(View.GONE);
                previewButton.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.VISIBLE);

                String mimeType = context.getContentResolver().getType(uri);
                if (mimeType != null && mimeType.startsWith("image/")) {
                    Glide.with(context)
                            .load(uri)
                            .placeholder(R.drawable.ic_loading_spinner)
                            .error(R.drawable.ic_file_error)
                            .into(previewButton);
                } else {
                    previewButton.setImageResource(R.drawable.preview_icon);
                }
            } catch (Exception e) {
                showError("Failed to load preview");
            }
        }

        private void showFilePreview(Task task) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW)
                        .setData(task.getFileUri())
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                context.startActivity(intent);
            } catch (Exception e) {
                showError("Cannot open file");
            }
        }

        private void removeUploadedFile(Task task) {
            task.setFileUri(null);
            previewButton.setVisibility(View.GONE);
            deleteButton.setVisibility(View.GONE);
            uploadButton.setVisibility(View.VISIBLE);
        }

        private void openFilePicker() {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .setType("*/*")
                    .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/pdf", "text/plain"})
                    .addCategory(Intent.CATEGORY_OPENABLE);
            filePickerLauncher.launch(intent);
        }

        private void showError(String message) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }

        private void setupArrowClickListeners() {
            downArrow.setOnClickListener(v -> toggleNotesVisibility(true));
            upArrow.setOnClickListener(v -> toggleNotesVisibility(false));
        }

        private void toggleNotesVisibility(boolean show) {
            TransitionManager.beginDelayedTransition((ViewGroup) itemView);
            notesLayout.setVisibility(show ? View.VISIBLE : View.GONE);
            downArrow.setVisibility(show ? View.GONE : View.VISIBLE);
            upArrow.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }
}

//
//public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
//
//    private final List<Task> tasks;
//    private final OnTaskClickListener listener;
//    private final Context context;
//
//    public TaskAdapter(List<Task> tasks, OnTaskClickListener listener, Context context) {
//        this.tasks = tasks;
//        this.listener = listener;
//        this.context = context;
//    }
//
//    @NonNull
//    @Override
//    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.task_row, parent, false);
//        return new TaskViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
//        Task task = tasks.get(position);
//        holder.bind(task, listener);
//
//        holder.itemView.setOnLongClickListener(v -> {
//            showDeleteDialog(position);
//            return true; // Consume the long press event
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return tasks.size();
//    }
//
//    private void showDeleteDialog(int position) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(context);
//        builder.setTitle("Delete Task");
//        builder.setMessage("Are you sure you want to delete this task?");
//        builder.setPositiveButton("Delete", (dialog, which) -> {
//            // Remove the task from the list
//            if (listener instanceof MainActivity) {
//                Task task = tasks.get(position);
////                ((MainActivity) listener).deleteTaskFromDatabase(task.getId());
//                tasks.remove(position);
//                notifyItemRemoved(position);
//            }
//        });
//        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
//        builder.create().show();
//    }
//    public static class TaskViewHolder extends RecyclerView.ViewHolder {
//        private final EditText taskEditText;
//        private final CheckBox taskCheckbox;
//        private final ImageButton downArrow;
//        private final ImageButton upArrow;
//        private final ConstraintLayout notesLayout;
//
//        public TaskViewHolder(@NonNull View itemView) {
//            super(itemView);
//            taskEditText = itemView.findViewById(R.id.taskEditText);
//            taskCheckbox = itemView.findViewById(R.id.taskCheckbox);
//            downArrow = itemView.findViewById(R.id.down_arrow);
//            upArrow = itemView.findViewById(R.id.up_arrow);
//            notesLayout = itemView.findViewById(R.id.notesLayout);
//        }
//
//        public void bind(Task task, OnTaskClickListener listener) {
//            taskEditText.setText(task.getContent());
//
//            // Set up click listeners for arrows
//            setupArrowClickListeners();
//
//            // Set up text change listener for EditText
//            taskEditText.addTextChangedListener(new TextWatcher() {
//                @Override
//                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
//
//                @Override
//                public void onTextChanged(CharSequence s, int start, int before, int count) {
//                    listener.onTaskTextChanged(getAdapterPosition(), s.toString());
//                }
//
//                @Override
//                public void afterTextChanged(Editable s) {}
//            });
//        }
//
//        private void setupArrowClickListeners() {
//            downArrow.setOnClickListener(v -> {
//                Transition transition = new AutoTransition();
//                transition.setDuration(500);
//
//                TransitionManager.beginDelayedTransition((ViewGroup) itemView);
//                notesLayout.setVisibility(View.VISIBLE);
//                downArrow.setVisibility(View.GONE);
//                upArrow.setVisibility(View.VISIBLE);
//
//                // Adjust layout constraints dynamically
//                ConstraintLayout.LayoutParams layoutParams =
//                        (ConstraintLayout.LayoutParams) taskEditText.getLayoutParams();
//                layoutParams.endToStart = upArrow.getId();
//                layoutParams.startToEnd = taskCheckbox.getId();
//                taskEditText.setLayoutParams(layoutParams);
//            });
//
//            upArrow.setOnClickListener(v -> {
//                Transition transition = new AutoTransition();
//                transition.setDuration(500);
//
//                TransitionManager.beginDelayedTransition((ViewGroup) itemView);
//                notesLayout.setVisibility(View.GONE);
//                downArrow.setVisibility(View.VISIBLE);
//                upArrow.setVisibility(View.GONE);
//
//                // Adjust layout constraints dynamically
//                ConstraintLayout.LayoutParams layoutParams =
//                        (ConstraintLayout.LayoutParams) taskEditText.getLayoutParams();
//                layoutParams.endToStart = downArrow.getId();
//                layoutParams.startToEnd = taskCheckbox.getId();
//                taskEditText.setLayoutParams(layoutParams);
//            });
//        }
//    }
//
//    public interface OnTaskClickListener {
//        void onTaskTextChanged(int position, String text);
//    }
//}