package com.just_for_fun.dotlist.Bin;

import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.just_for_fun.dotlist.Database.DBHelper;
import com.just_for_fun.dotlist.R;
import com.just_for_fun.dotlist.Task.Task;
import java.util.List;

public class BinActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bin_layout);

        // Handle back button
        ImageButton backButton = findViewById(R.id.trash_strip).findViewById(R.id.back_arrow);
        backButton.setOnClickListener(v -> finish());

        // Setup RecyclerView
        RecyclerView binRecyclerView = findViewById(R.id.trash_view);
        binRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        try {
            DBHelper dbHelper = new DBHelper(this);
            List<Task> validTasks = dbHelper.getDeletedTasks();

            BinAdapter adapter = new BinAdapter(validTasks);
            binRecyclerView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}

/*
// Get tasks deleted within 30 days and with titles
        List<Task> validTasks = TaskAdapter.getDeletedTasks().stream()
                .filter(task ->
                      //  !task.getTitle().isEmpty() &&
                                (System.currentTimeMillis() - task.getDeletionTime()) <= TimeUnit.DAYS.toMillis(30)
                )
                .collect(Collectors.toList());

        BinAdapter adapter = new BinAdapter(validTasks);
        binRecyclerView.setAdapter(adapter);
*/
