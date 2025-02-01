package com.just_for_fun.dotlist.Database;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DatabaseExecutor {
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static DatabaseExecutor instance;

    private DatabaseExecutor() {}

    public static DatabaseExecutor getInstance() {
        if (instance == null) {
            instance = new DatabaseExecutor();
        }
        return instance;
    }

    public void execute(Runnable runnable) {
        executor.execute(runnable);
    }
}
