package com.ck.music_app;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MusicApplication extends Application {
    private static final String TAG = "MusicApplication";
    private ExecutorService executorService;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize executor service for background tasks
        executorService = Executors.newSingleThreadExecutor();

        // Initialize Google Play Services Provider in background
        initializeProviderInstallerAsync();
    }

    private void initializeProviderInstallerAsync() {
        // Run provider installation in background thread to avoid blocking main thread
        executorService.execute(() -> {
            try {
                ProviderInstaller.installIfNeeded(this);
                Log.d(TAG, "Google Play Services Provider installed successfully");
            } catch (GooglePlayServicesRepairableException e) {
                Log.w(TAG, "Google Play Services Provider installation failed (repairable)", e);
                // The provider is helpful, but it is possible to succeed without it.
            } catch (GooglePlayServicesNotAvailableException e) {
                Log.w(TAG, "Google Play Services Provider installation failed (not available)", e);
                // App should be able to work without it, but might be vulnerable to security
                // issues.
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error installing Google Play Services Provider", e);
            }
        });
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}