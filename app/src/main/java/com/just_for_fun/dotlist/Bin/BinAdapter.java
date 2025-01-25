package com.just_for_fun.dotlist.Bin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.just_for_fun.dotlist.R;
import com.just_for_fun.dotlist.Task.Task;

import java.util.List;

public class BinAdapter extends RecyclerView.Adapter<BinAdapter.BinViewHolder> {
    private List<Task> tasks;

    public BinAdapter(List<Task> tasks) {
        this.tasks = tasks;
    }

    @NonNull
    @Override
    public BinViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_row_simple, parent, false); // Create a simplified layout
        return new BinViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BinViewHolder holder, int position) {
        holder.titleTextView.setText(tasks.get(position).getTitle());
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    static class BinViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView;
        public BinViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.bin_task_title);
        }
    }
}
