package com.example.data.download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reconstructed Offline Download Service (introduced in Hamyra 2.1).
 * Supports background downloads of movies and episodes with notification controls.
 */
public final class DownloadService extends Service {
    public static final String CHANNEL_ID = "hamyra_downloads";
    public static final int NOTIFICATION_ID = 4001;

    public static final String ACTION_START_DOWNLOAD = "com.example.download.START_DOWNLOAD";
    public static final String ACTION_PAUSE_ALL = "com.example.download.PAUSE_ALL";
    public static final String ACTION_RESUME_ALL = "com.example.download.RESUME_ALL";
    public static final String ACTION_CANCEL_ALL = "com.example.download.CANCEL_ALL";
    public static final String ACTION_CANCEL_ITEM = "com.example.download.CANCEL_ITEM";

    public static class DownloadItem {
        public final long id;
        public final String title;
        public final String url;
        public final String destinationPath;
        public int status; // 0=pending, 1=downloading, 2=paused, 3=completed, 4=failed
        public long downloadedBytes;
        public long totalBytes;

        public DownloadItem(long id, String title, String url, String destinationPath) {
            this.id = id;
            this.title = title;
            this.url = url;
            this.destinationPath = destinationPath;
            this.status = 0;
            this.downloadedBytes = 0;
            this.totalBytes = 0;
        }
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ConcurrentHashMap<Long, DownloadItem> activeDownloads = new ConcurrentHashMap<>();
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_PAUSE_ALL:
                    pauseAll();
                    break;
                case ACTION_RESUME_ALL:
                    resumeAll();
                    break;
                case ACTION_CANCEL_ALL:
                    cancelAll();
                    break;
                case ACTION_CANCEL_ITEM:
                    long id = intent.getLongExtra("download_id", -1);
                    if (id != -1) cancelDownload(id);
                    break;
            }
        }
        updateNotification();
        return START_NOT_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Hamyra Downloads",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Background download progress and controls");
            channel.setShowBadge(false);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    private PendingIntent createActionPendingIntent(String action) {
        Intent intent = new Intent(this, DownloadService.class).setAction(action);
        return PendingIntent.getService(this, action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private void updateNotification() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(this, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(this);
        }

        int activeCount = 0;
        int pausedCount = 0;
        DownloadItem current = null;

        for (DownloadItem item : activeDownloads.values()) {
            if (item.status == 1 || item.status == 0) {
                activeCount++;
                if (current == null) current = item;
            } else if (item.status == 2) {
                pausedCount++;
            }
        }

        builder.setSmallIcon(android.R.drawable.stat_sys_download);
        builder.setOngoing(activeCount > 0);

        if (current != null) {
            String title = current.title != null ? current.title : "Downloading";
            if (activeCount > 1) {
                title += " (+" + (activeCount - 1) + " more)";
            }
            builder.setContentTitle(title);

            if (current.totalBytes > 0) {
                int percent = (int) ((current.downloadedBytes * 100) / current.totalBytes);
                builder.setProgress(100, percent, false);
                builder.setContentText(percent + "% · " + formatSize(current.downloadedBytes) + " / " + formatSize(current.totalBytes));
            } else {
                builder.setProgress(0, 0, true);
                builder.setContentText("Starting…");
            }

            builder.addAction(new Notification.Action.Builder(null, "Pause", createActionPendingIntent(ACTION_PAUSE_ALL)).build());
            builder.addAction(new Notification.Action.Builder(null, "Cancel", createActionPendingIntent(ACTION_CANCEL_ALL)).build());
            startForeground(NOTIFICATION_ID, builder.build());
        } else if (pausedCount > 0) {
            builder.setContentTitle("Downloads paused");
            builder.setContentText(pausedCount + " paused · tap Resume to continue");
            builder.setSmallIcon(android.R.drawable.stat_sys_download_done);
            builder.addAction(new Notification.Action.Builder(null, "Resume", createActionPendingIntent(ACTION_RESUME_ALL)).build());
            builder.addAction(new Notification.Action.Builder(null, "Cancel", createActionPendingIntent(ACTION_CANCEL_ALL)).build());
            stopForeground(false);
            if (notificationManager != null) {
                notificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        } else {
            stopForeground(true);
        }
    }

    public void pauseAll() {
        for (DownloadItem item : activeDownloads.values()) {
            if (item.status == 1) {
                item.status = 2;
            }
        }
        updateNotification();
    }

    public void resumeAll() {
        for (DownloadItem item : activeDownloads.values()) {
            if (item.status == 2) {
                item.status = 1;
            }
        }
        updateNotification();
    }

    public void cancelAll() {
        for (Map.Entry<Long, DownloadItem> entry : activeDownloads.entrySet()) {
            deleteFile(entry.getValue().destinationPath);
        }
        activeDownloads.clear();
        stopForeground(true);
    }

    public void cancelDownload(long id) {
        DownloadItem item = activeDownloads.remove(id);
        if (item != null) {
            deleteFile(item.destinationPath);
        }
        updateNotification();
    }

    private static void deleteFile(String path) {
        if (path != null && !path.isEmpty()) {
            try {
                File f = new File(path);
                if (f.exists()) f.delete();
            } catch (Throwable ignored) {}
        }
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
