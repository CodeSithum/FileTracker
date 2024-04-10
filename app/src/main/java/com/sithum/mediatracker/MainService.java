package com.sithum.mediatracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.FileObserver;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class MainService extends Service {
    private static final String TAG = "MainService";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MediaTrackerChannel";

    private FileObserver fileObserver;
    String sourceDir, destDir;

    @Override
    public void onCreate() {
        super.onCreate();
        startForegroundService();
    }

    private void startForegroundService() {
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
    }

    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Media Tracker Service")
                .setContentText("Running in the background")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Media Tracker Service", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }

        return builder.build();
    }

    private void startFileObserver(String sourceDir, String destDir) {
        fileObserver = new FileObserver(sourceDir, FileObserver.CREATE | FileObserver.MODIFY | FileObserver.DELETE | FileObserver.MOVED_FROM | FileObserver.MOVED_TO) {
            @Override
            public void onEvent(int event, String path) {
                if (event == FileObserver.CREATE || event == FileObserver.MODIFY || event == FileObserver.MOVED_TO) {
                    String sourceFilePath = getSourceFilePath(sourceDir, path);
                    String targetFilePath = getTargetFilePath(destDir, path);
                    copyFile(sourceFilePath, targetFilePath);
                } else if (event == FileObserver.DELETE || event == FileObserver.MOVED_FROM) {
                    // Handle delete or move events if needed
                }
            }
        };
        fileObserver.startWatching();
    }

    private String getSourceFilePath(String sourceDir, String fileName) {
        return sourceDir + File.separator + fileName;
    }

    private String getTargetFilePath(String destDir, String fileName) {
        return destDir + File.separator + fileName;
    }

    private void copyFile(String sourcePath, String targetPath) {
        Log.d(TAG, "Copying file from: " + sourcePath);
        Log.d(TAG, "Copying file to: " + targetPath);

        File sourceFile = new File(sourcePath);
        File targetFile = new File(targetPath);

        // Check if the source file exists
        if (!sourceFile.exists()) {
            Log.e(TAG, "Source file does not exist: " + sourcePath);
            return;
        }

        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(targetFile);
             FileChannel inChannel = fis.getChannel();
             FileChannel outChannel = fos.getChannel()) {
            inChannel.transferTo(0, inChannel.size(), outChannel);
            Log.d(TAG, "File copied to: " + targetFile.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error copying file: " + sourceFile.getName(), e);
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            sourceDir = intent.getStringExtra("sourceDir");
            destDir = intent.getStringExtra("destDir");

            if (sourceDir != null && destDir != null) {
                Log.d(TAG, "Source directory: " + sourceDir);
                Log.d(TAG, "Destination directory: " + destDir);

                // Start observing and copying files based on the provided directories
                startFileObserver(sourceDir, destDir);
            } else {
                Log.e(TAG, "Source or destination directory path is null");
            }
        } else {
            Log.e(TAG, "Intent is null");
        }
        Log.d(TAG, "Service started");
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fileObserver != null) {
            fileObserver.stopWatching();
        }
    }
}
