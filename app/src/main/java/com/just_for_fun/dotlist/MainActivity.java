package com.just_for_fun.dotlist;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private LinearLayout taskContainer;
    private int currentCapacity = 5; // Initial capacity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        taskContainer = findViewById(R.id.taskContainer);

        // Add initial input boxes
        for (int i = 0; i < currentCapacity; i++) {
            taskContainer.addView(getLayoutInflater().inflate(R.layout.task_row, taskContainer, false));
        }

        // Add text changed listener to any EditText within the container
        taskContainer.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
            @Override
            public void onViewAttachedToWindow(View v) {
                for (int i = 0; i < taskContainer.getChildCount(); i++) {
                    EditText editText = (EditText) taskContainer.getChildAt(i).findViewById(R.id.taskEditText);
                    editText.addTextChangedListener(textWatcher);
                }
            }

            @Override
            public void onViewDetachedFromWindow(View v) {}
        });
    }

    private final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            checkAndAddRows();
        }

        @Override
        public void afterTextChanged(Editable s) {}
    };

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
        }
    }
}