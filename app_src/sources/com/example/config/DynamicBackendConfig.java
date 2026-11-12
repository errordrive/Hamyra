package com.example.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reconstructed Dynamic Backend Failover & Healthcheck System (reconstructing defpackage.ok).
 * Automatically healthchecks endpoints via /healthz and manages failovers.
 */
public final class DynamicBackendConfig {
    private static final String TAG = "DynamicBackendConfig";
    public static final DynamicBackendConfig INSTANCE = new DynamicBackendConfig();

    private volatile String primaryBackend = AppConfig.BACKEND_PRIMARY;
    private volatile String backupBackend = AppConfig.BACKEND_BACKUP;
    private volatile List<String> backendList = new ArrayList<>(Arrays.asList(AppConfig.BACKEND_PRIMARY, AppConfig.BACKEND_BACKUP));
    private volatile String activeBackend = AppConfig.BACKEND_PRIMARY;
    private volatile String backendOverride = "";

    private volatile List<String> workerList = new ArrayList<>(Arrays.asList(AppConfig.WORKER_PRIMARY, AppConfig.WORKER_SECONDARY));
    private volatile String activeWorker = AppConfig.WORKER_PRIMARY;
    private volatile String workerOverride = "";

    private volatile boolean initialized = false;
    private Context context;

    private DynamicBackendConfig() {}

    public void init(Context ctx) {
        if (initialized) return;
        synchronized (this) {
            if (initialized) return;
            this.context = ctx != null ? ctx.getApplicationContext() : null;
            if (this.context != null) {
                SharedPreferences prefs = this.context.getSharedPreferences("hamyra_app_config", Context.MODE_PRIVATE);
                this.primaryBackend = prefs.getString("backend", AppConfig.BACKEND_PRIMARY);
                this.backupBackend = prefs.getString("backup", AppConfig.BACKEND_BACKUP);
                this.backendOverride = prefs.getString("backend_override", "");
                this.activeBackend = prefs.getString("active_backend", AppConfig.BACKEND_PRIMARY);

                this.workerOverride = prefs.getString("worker_override", "");
                this.activeWorker = prefs.getString("active_worker", AppConfig.WORKER_PRIMARY);
            }
            this.initialized = true;
        }
    }

    /**
     * Probes an endpoint with a GET /healthz request to determine availability.
     */
    public static boolean checkHealth(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) return false;
        try {
            String cleanUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
            URL url = new URL(cleanUrl + "/healthz");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Hamyra-App");
            int code = conn.getResponseCode();
            conn.disconnect();
            return code >= 200 && code < 400;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Returns the currently active backend URL (e.g. api.hamyra.xyz).
     */
    public String getBackendUrl() {
        if (backendOverride != null && backendOverride.startsWith("http")) {
            return trimSlash(backendOverride);
        }
        return trimSlash(activeBackend != null ? activeBackend : AppConfig.BACKEND_PRIMARY);
    }

    /**
     * Returns the currently active worker URL (e.g. hamyra-api.*.workers.dev).
     */
    public String getWorkerUrl() {
        if (workerOverride != null && workerOverride.startsWith("http")) {
            return trimSlash(workerOverride);
        }
        return trimSlash(activeWorker != null ? activeWorker : AppConfig.WORKER_PRIMARY);
    }

    /**
     * Failover routine for worker endpoints.
     */
    public synchronized void failoverWorker() {
        if (workerOverride != null && workerOverride.startsWith("http")) return;
        if (workerList.size() < 2) return;

        int currentIndex = workerList.indexOf(activeWorker);
        int nextIndex = (currentIndex + 1) % workerList.size();
        String candidate = workerList.get(nextIndex);

        Log.w(TAG, "Worker failover triggered: " + activeWorker + " -> " + candidate);
        activeWorker = candidate;
        persistConfig("active_worker", activeWorker);
    }

    /**
     * Failover routine for backend endpoints.
     */
    public synchronized void failoverBackend() {
        if (backendOverride != null && backendOverride.startsWith("http")) return;
        if (backendList.size() < 2) return;

        int currentIndex = backendList.indexOf(activeBackend);
        int nextIndex = (currentIndex + 1) % backendList.size();
        String candidate = backendList.get(nextIndex);

        Log.w(TAG, "Backend failover triggered: " + activeBackend + " -> " + candidate);
        activeBackend = candidate;
        persistConfig("active_backend", activeBackend);
    }

    private void persistConfig(String key, String value) {
        if (context != null) {
            try {
                SharedPreferences prefs = context.getSharedPreferences("hamyra_app_config", Context.MODE_PRIVATE);
                prefs.edit().putString(key, value).apply();
            } catch (Throwable ignored) {}
        }
    }

    private String trimSlash(String str) {
        if (str == null) return "";
        return str.endsWith("/") ? str.substring(0, str.length() - 1) : str;
    }
}
