package com.just_for_fun.dotlist.Task;

import static android.graphics.Typeface.ITALIC;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.just_for_fun.dotlist.Database.DBHelper;
import com.just_for_fun.dotlist.MainActivity;
import com.just_for_fun.dotlist.R;
import com.just_for_fun.dotlist.Task.TextStyle;

import java.util.ArrayList;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private final List<Task> tasks;
    private static List<Task> deletedTasks = new ArrayList<>();
    private final OnFileUploadListener fileUploadListener;
    private final OnTaskDeleteListener taskDeleteListener;
    private final DBHelper dbHelper;

    public interface OnFileUploadListener {
        void onFileUpload(int position);
    }

    public interface OnTaskDeleteListener {
        void onTaskDelete(int position);
    }

    public TaskAdapter(List<Task> tasks, OnFileUploadListener fileUploadListener,
                       OnTaskDeleteListener taskDeleteListener, DBHelper dbHelper) {
        this.tasks = tasks;
        this.fileUploadListener = fileUploadListener;
        this.taskDeleteListener = taskDeleteListener;
        this.dbHelper = dbHelper;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_row, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads);
        } else {
            // Update only visibility for expansion/collapse
            Task task = tasks.get(position);
            updateExpansionState(holder, task);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);

        // Setup CheckBox
        holder.checkBox.setChecked(task.isChecked());
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setChecked(isChecked);
            dbHelper.updateTask(task);
        });

        // Remove existing TextWatcher to avoid duplicates
        if (holder.titleTextWatcher != null) {
            holder.taskEditText.removeTextChangedListener(holder.titleTextWatcher);
        }

        // Set the title text from the Task object
        holder.taskEditText.setText(task.getTitle());

        // Add a new TextWatcher
        holder.titleTextWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                task.setTitle(s.toString());
                dbHelper.updateTask(task);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        };

        holder.taskEditText.addTextChangedListener(holder.titleTextWatcher);

        // Remove existing TextWatcher for notes to avoid duplicates
        if (holder.notesTextWatcher != null) {
            holder.notesArea.removeTextChangedListener(holder.notesTextWatcher);
        }

        // Set notes text from the Task object
        holder.notesArea.setText(task.getNotes());

        // Add new TextWatcher for notes
        holder.notesTextWatcher = new NoteTextWatcher(task);
        holder.notesArea.addTextChangedListener(holder.notesTextWatcher);

        setupTaskViews(holder, task, position);
        setupTextFormatting(holder, task);
    }

    // region View Setup Methods
    private void setupTaskViews(TaskViewHolder holder, Task task, int position) {
        updateViewState(holder, task);
        setupArrowButtons(holder, task, position);
        setupFileOperations(holder, task, position);
        setupTaskDeletion(holder, position);
        setupNotesField(holder, task);

        holder.taskEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                task.setTitle(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateViewState(TaskViewHolder holder, Task task) {
        updateExpansionState(holder, task);
        updateFileState(holder, task);
    }

    private void updateExpansionState(TaskViewHolder holder, Task task) {
        holder.notesLayout.setVisibility(task.isExpanded() ? View.VISIBLE : View.GONE);
        holder.downArrow.setVisibility(task.isExpanded() ? View.GONE : View.VISIBLE);
        holder.upArrow.setVisibility(task.isExpanded() ? View.VISIBLE : View.GONE);
    }

    private void updateFileState(TaskViewHolder holder, Task task) {
        holder.uploadButton.setVisibility(task.getFileUri() == null ? View.VISIBLE : View.GONE);
        holder.previewButton.setVisibility(task.getFileUri() != null ? View.VISIBLE : View.GONE);
        holder.deleteButton.setVisibility(task.isShowDeleteFileBtn() ? View.VISIBLE : View.GONE);
    }

    private void setupArrowButtons(TaskViewHolder holder, Task task, int position) {
        View.OnClickListener arrowListener = v -> toggleExpansion(holder, task, position);
        holder.downArrow.setOnClickListener(arrowListener);
        holder.upArrow.setOnClickListener(arrowListener);
    }

    private void setupFileOperations(TaskViewHolder holder, Task task, int position) {
        holder.uploadButton.setOnClickListener(v -> handleFileUpload(task, position));
        holder.previewButton.setOnClickListener(v -> showFilePreview(holder, task));
        holder.previewButton.setOnLongClickListener(v -> handlePreviewLongPress(task, position));
        holder.deleteButton.setOnClickListener(v -> handleFileDelete(task, position));
    }

    private void setupTaskDeletion(TaskViewHolder holder, int position) {
        holder.checkBox.setOnLongClickListener(v -> showDeleteConfirmationDialog(holder, position));
    }

    private void setupNotesField(TaskViewHolder holder, Task task) {
        holder.notesArea.setHorizontallyScrolling(false);
        holder.notesArea.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        holder.notesArea.setText(task.getNotes(), TextView.BufferType.SPANNABLE);
    }
    // endregion

    // region Text Formatting
    private void setupTextFormatting(TaskViewHolder holder, Task task) {
        holder.notesArea.setCustomSelectionActionModeCallback(new TextActionModeCallback(holder));
        holder.notesArea.addTextChangedListener(new NoteTextWatcher(task));
        applyExistingFormatting(holder, task);
    }

    private void applyExistingFormatting(TaskViewHolder holder, Task task) {
        SpannableStringBuilder notesSpannable = (SpannableStringBuilder) holder.notesArea.getText();
        updateNotesFormatting(notesSpannable, task.getNotesFormatting());
    }

    private void updateNotesFormatting(SpannableStringBuilder notesSpannable, List<NoteFormatting> formatting) {
        if (notesSpannable == null || formatting == null) return;

        notesSpannable.clearSpans();
        for (NoteFormatting format : formatting) {
            switch (format.getStyle()) {
                case BOLD:
                    notesSpannable.setSpan(new StyleSpan(Typeface.BOLD), format.getStart(), format.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case ITALIC:
                    notesSpannable.setSpan(new StyleSpan(Typeface.ITALIC), format.getStart(), format.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case UNDERLINE:
                    notesSpannable.setSpan(new UnderlineSpan(), format.getStart(), format.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case STRIKETHROUGH:
                    notesSpannable.setSpan(new StrikethroughSpan(), format.getStart(), format.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case COLOR:
                    notesSpannable.setSpan(new ForegroundColorSpan(format.getColor()), format.getStart(), format.getEnd(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
            }
        }
    }

    private void applyFormatSpan(SpannableStringBuilder builder, NoteFormatting format) {
        switch (format.getStyle()) {
            case BOLD:
                builder.setSpan(new StyleSpan(Typeface.BOLD), format.getStart(), format.getEnd(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case ITALIC:
                builder.setSpan(new StyleSpan(ITALIC), format.getStart(), format.getEnd(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case UNDERLINE:
                builder.setSpan(new UnderlineSpan(), format.getStart(), format.getEnd(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
        }
    }
    // endregion

    // region File Operations
    private void handleFileUpload(Task task, int position) {
        task.setExpanded(true);
        fileUploadListener.onFileUpload(position);
    }

    private void showFilePreview(TaskViewHolder holder, Task task) {
        if (task.getFileUri() != null) {
            Context context = holder.itemView.getContext();
            if (context instanceof MainActivity) {
                ((MainActivity) context).showFilePreview(task.getFileUri());
            }
        }
    }

    private boolean handlePreviewLongPress(Task task, int position) {
        task.setShowDeleteFileBtn(true);
        notifyItemChanged(position);
        return true;
    }

    private void handleFileDelete(Task task, int position) {
        task.setFileUri(null);
        task.setShowDeleteFileBtn(false);
        task.setExpanded(true);
        notifyItemChanged(position);
    }
    // endregion

    // region Task Operations
    private void toggleExpansion(@NonNull TaskViewHolder holder, Task task, int position) {
        task.setExpanded(!task.isExpanded());
        if (task.isExpanded()) {
            holder.notesArea.requestFocus();
        }
        notifyItemChanged(position, "toggle_expansion");
    }

    private boolean showDeleteConfirmationDialog(TaskViewHolder holder, int position) {
        new AlertDialog.Builder(holder.itemView.getContext())
                .setTitle(R.string.delete_task_title)
                .setMessage(R.string.delete_task_confirmation)
                .setPositiveButton(R.string.delete, (dialog, which) -> deleteTask(position))
                .setNegativeButton(R.string.cancel, null)
                .show();
        return true;
    }

    private void deleteTask(int position) {
        if (position == RecyclerView.NO_POSITION) return;
        Task task = tasks.get(position);
        task.setDeletionTime(System.currentTimeMillis());
        dbHelper.updateTask(task);
        deletedTasks.add(task);
        tasks.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, tasks.size() - position);
    }

    public static List<Task> getDeletedTasks() {
        return deletedTasks;
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void addNewTask() {
        Task newTask = new Task();
        long id = dbHelper.addTask(newTask);
        newTask.setId(id);
        tasks.add(newTask);
        notifyItemInserted(tasks.size() - 1);
    }

    // region ViewHolder
    static class TaskViewHolder extends RecyclerView.ViewHolder {

        private TextWatcher titleTextWatcher;
        private TextWatcher notesTextWatcher;

        final ConstraintLayout notesLayout;
        final ImageButton downArrow, upArrow, uploadButton, deleteButton, previewButton;
        final EditText taskEditText, notesArea;
        final CheckBox checkBox;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            notesLayout = itemView.findViewById(R.id.notesLayout);
            downArrow = itemView.findViewById(R.id.down_arrow);
            upArrow = itemView.findViewById(R.id.up_arrow);
            uploadButton = itemView.findViewById(R.id.uploadButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            previewButton = itemView.findViewById(R.id.previewButton);
            taskEditText = itemView.findViewById(R.id.taskEditText);
            checkBox = itemView.findViewById(R.id.taskCheckbox);
            notesArea = itemView.findViewById(R.id.notesArea);
        }
    }
    // endregion

    // region Helper Classes
    private static class TextActionModeCallback implements ActionMode.Callback {
        private final TaskViewHolder holder;

        TextActionModeCallback(TaskViewHolder holder) {
            this.holder = holder;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.text_actions, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int start = holder.notesArea.getSelectionStart();
            int end = holder.notesArea.getSelectionEnd();
            if (start == end) return false;

            SpannableStringBuilder builder = new SpannableStringBuilder(holder.notesArea.getText());
            Context context = holder.itemView.getContext();

            if (item.getItemId() == R.id.action_bold) {
                applyStyle(builder, start, end, Typeface.BOLD);
            } else if (item.getItemId() == R.id.action_italic) {
                applyStyle(builder, start, end, ITALIC);
            } else if (item.getItemId() == R.id.action_underline) {
                applyUnderline(builder, start, end);
            } else if (item.getItemId() == android.R.id.copy) {
                handleCopyAction(start, end, context);
            } else if (item.getItemId() == android.R.id.cut) {
                handleCutAction(builder, start, end, context);
            } else if (item.getItemId() == android.R.id.paste) {
                handlePasteAction(builder, start, context);
            } else if (item.getItemId() == R.id.action_strikethrough) {
                applyStrikethrough(builder, start, end);
            } else if (item.getItemId() == R.id.action_color) {
                showColorPickerDialog(builder, start, end);
            }

            holder.notesArea.setText(builder);
            mode.finish();
            return true;
        }

        private void applyStyle(SpannableStringBuilder builder, int start, int end, int typefaceStyle) {
            StyleSpan styleSpan = new StyleSpan(typefaceStyle);
            builder.setSpan(styleSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private void applyUnderline(SpannableStringBuilder builder, int start, int end) {
            UnderlineSpan underlineSpan = new UnderlineSpan();
            builder.setSpan(underlineSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        private void handleCopyAction(int start, int end, Context context) {
            String selectedText = holder.notesArea.getText().subSequence(start, end).toString();
            copyToClipboard(selectedText, context);
        }

        private void handleCutAction(SpannableStringBuilder builder, int start, int end, Context context) {
            String selectedText = holder.notesArea.getText().subSequence(start, end).toString();
            copyToClipboard(selectedText, context);
            builder.delete(start, end);
        }

        private void handlePasteAction(SpannableStringBuilder builder, int start, Context context) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData.Item clipItem = clipboard.getPrimaryClip().getItemAt(0);
                if (clipItem.getText() != null) {
                    builder.insert(start, clipItem.getText());
                }
            }
        }

        private void copyToClipboard(String text, Context context) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("Copied Text", text));
            Toast.makeText(context, R.string.text_copied, Toast.LENGTH_SHORT).show();
        }

        private void applyStrikethrough(SpannableStringBuilder builder, int start, int end) {
            builder.setSpan(new StrikethroughSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private void applyColor(SpannableStringBuilder builder, int start, int end, int color) {
            builder.setSpan(new ForegroundColorSpan(color), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        private void showColorPickerDialog(SpannableStringBuilder builder, int start, int end) {
            Context context = holder.itemView.getContext();
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setTitle(R.string.choose_color);

            // List of colors to choose from
            int[] colors = {
                    Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.BLACK
            };

            dialog.setItems(R.array.color_names, (d, which) -> {
                int selectedColor = colors[which];
                applyColor(builder, start, end, selectedColor);
                holder.notesArea.setText(builder);
            });

            dialog.show();
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // Cleanup if needed
        }
    }

    private static class NoteTextWatcher implements TextWatcher {
        private final Task task;

        NoteTextWatcher(Task task) {
            this.task = task;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            task.setNotes(s.toString());
        }

        @Override
        public void afterTextChanged(Editable s) {
            List<NoteFormatting> formatting = new ArrayList<>();
            for (Object span : s.getSpans(0, s.length(), Object.class)) {
                if (span instanceof StyleSpan) {
                    handleStyleSpan((StyleSpan) span, s, formatting);
                } else if (span instanceof UnderlineSpan) {
                    handleUnderlineSpan((UnderlineSpan) span, s, formatting);
                } else if (span instanceof StrikethroughSpan) {
                    handleStrikethroughSpan((StrikethroughSpan) span, s, formatting);
                } else if (span instanceof ForegroundColorSpan) {
                    handleColorSpan((ForegroundColorSpan) span, s, formatting);
                }
            }
            task.setNotesFormatting(formatting);
        }

        private void handleStrikethroughSpan(StrikethroughSpan span, Editable s, List<NoteFormatting> formatting) {
            formatting.add(new NoteFormatting(
                    s.getSpanStart(span),
                    s.getSpanEnd(span),
                    TextStyle.STRIKETHROUGH
            ));
        }

        private void handleColorSpan(ForegroundColorSpan span, Editable s, List<NoteFormatting> formatting) {
            formatting.add(new NoteFormatting(
                    s.getSpanStart(span),
                    s.getSpanEnd(span),
                    TextStyle.COLOR,
                    span.getForegroundColor()
            ));
        }

        private List<NoteFormatting> extractFormatting(Editable s) {
            List<NoteFormatting> formatting = new ArrayList<>();
            for (Object span : s.getSpans(0, s.length(), Object.class)) {
                if (span instanceof StyleSpan) {
                    handleStyleSpan((StyleSpan) span, s, formatting);
                } else if (span instanceof UnderlineSpan) {
                    handleUnderlineSpan((UnderlineSpan) span, s, formatting);
                }
            }
            return formatting;
        }

        private void handleStyleSpan(StyleSpan span, Editable s, List<NoteFormatting> formatting) {
            int style = span.getStyle();
            if (style == Typeface.BOLD) {
                addFormatting(s, span, TextStyle.BOLD, formatting);
            } else if (style == ITALIC) {
                addFormatting(s, span, TextStyle.ITALIC, formatting);
            }
        }

        private void handleUnderlineSpan(UnderlineSpan span, Editable s, List<NoteFormatting> formatting) {
            addFormatting(s, span, TextStyle.UNDERLINE, formatting);
        }

        private void addFormatting(Editable s, Object span, TextStyle style, List<NoteFormatting> formatting) {
            formatting.add(new NoteFormatting(
                    s.getSpanStart(span),
                    s.getSpanEnd(span),
                    style
            ));
        }
    }
    // endregion

    public List<Task> getTasks() {
        return new ArrayList<>(tasks); // Return copy to prevent external modification
    }

    public void removeTask(int position) {
        if (position >= 0 && position < tasks.size()) {
            tasks.remove(position); // Remove the task from the list
            notifyItemRemoved(position); // Notify the adapter of the removal
            notifyItemRangeChanged(position, tasks.size()); // Update positions of remaining items
        }
    }
}