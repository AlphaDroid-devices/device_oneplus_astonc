/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 *
 * Game MEMC (Iris 7P frame interpolation) session policy.
 *
 * When the master toggle is on and a profiled game comes to the foreground:
 * pin the panel at 120 (SF MIN=PEAK + ADFR min fps - FRC needs a fixed timing
 * and the 120 mode for 60->120 output) and publish the game's
 * irisConfigureSet(258) payload via sys.display.iris.memc_request; the CAF
 * composer request engine applies it at the Present boundary and the Iris
 * service enters FRC once the game's render cadence is stable (typically the
 * in-game 60 fps setting). On leaving the game the request is cleared and
 * RefreshRateMonitorService restores the user's refresh-rate state - no
 * backups are kept here, the monitor is the single source of truth.
 */

package org.lineageos.device.settings.memc;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.device.settings.Constants;
import org.lineageos.device.settings.refreshrate.RefreshRateMonitorService;
import org.lineageos.device.settings.utils.FileUtils;
import org.lineageos.device.settings.utils.ForegroundAppDetector;

public class MemcGameService extends Service {

    private static final String TAG = "MemcGameService";
    private static final String LISTENER_ID = "MemcGame";
    private static final String REQUEST_OFF = "off";

    private static volatile MemcGameService sInstance;

    private Handler mHandler;
    private ForegroundAppDetector mForegroundDetector;
    private boolean mMonitoring = false;
    private String mSessionPackage = null;

    // ===== Lifecycle =====

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mHandler = new Handler();
        mForegroundDetector = ForegroundAppDetector.getInstance(this);
        if (Constants.DEBUG) Log.i(TAG, "Service created");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopMonitoring();
        endSession();
        sInstance = null;
        if (Constants.DEBUG) Log.i(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler.post(this::handleStateChanged);
        return START_STICKY;
    }

    // ===== Public API =====

    /** True while a game MEMC session pins the panel; RefreshRateMonitorService
     *  must not fight the pin (same contract as the HBM pin). */
    public static boolean isPinActive() {
        MemcGameService instance = sInstance;
        return instance != null && instance.mSessionPackage != null;
    }

    public static void notifyStateChanged(Context context) {
        MemcGameService instance = sInstance;
        if (instance == null) {
            Intent serviceIntent = new Intent(context, MemcGameService.class);
            try {
                context.startService(serviceIntent);
                if (Constants.DEBUG) Log.i(TAG, "Service started");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start service", e);
            }
            return;
        }
        instance.mHandler.post(instance::handleStateChanged);
    }

    public static boolean isEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.KEY_MEMC_GAME, false);
    }

    public static void setEnabled(Context context, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(Constants.KEY_MEMC_GAME, enabled)
                .apply();
    }

    // ===== State handling =====

    private void handleStateChanged() {
        if (!isEnabled(this)) {
            if (Constants.DEBUG) Log.i(TAG, "Disabled - stopping");
            stopMonitoring();
            endSession();
            return;
        }

        if (!mMonitoring) {
            startMonitoring();
            // startMonitoring delivers an initial report for the current app
        } else {
            evaluateForegroundApp(mForegroundDetector.getCurrentForegroundApp());
        }
    }

    private void startMonitoring() {
        if (mMonitoring) {
            return;
        }
        mMonitoring = true;
        mForegroundDetector.startMonitoring(LISTENER_ID, this::evaluateForegroundApp);
        if (Constants.DEBUG) Log.i(TAG, "Monitoring started");
    }

    private void stopMonitoring() {
        if (!mMonitoring) {
            return;
        }
        mMonitoring = false;
        mForegroundDetector.stopMonitoring(LISTENER_ID);
        if (Constants.DEBUG) Log.i(TAG, "Monitoring stopped");
    }

    private void evaluateForegroundApp(String packageName) {
        // Master toggle already checked in handleStateChanged. Per-game opt-out:
        // profiled games are allowed unless the user disabled them in the list.
        MemcGameProfiles.Profile profile =
                packageName == null ? null : MemcGameProfiles.get(this, packageName);
        if (profile != null && MemcGameProfiles.isAppEnabled(this, packageName)) {
            startSession(profile);
        } else {
            if (profile != null && Constants.DEBUG) {
                Log.i(TAG, "Skip " + packageName + " (disabled in per-game list)");
            }
            endSession();
        }
    }

    // ===== Session pin / request =====

    private void startSession(MemcGameProfiles.Profile profile) {
        if (profile.packageName.equals(mSessionPackage)) {
            return;
        }
        mSessionPackage = profile.packageName;

        // Pin the panel first (stock ordering: settle the refresh rate, then 258).
        // DDIC self-refresh at the mode rate; the kernel holds min-fps tx while
        // FRC is live anyway, this keeps the floor from re-arming between sessions.
        FileUtils.writeLine(Constants.NODE_ADFR_MIN_FPS,
                String.valueOf(Constants.MEMC_PIN_REFRESH_RATE));
        setRefreshRate(Constants.MEMC_PIN_REFRESH_RATE, Constants.MEMC_PIN_REFRESH_RATE);

        String payload = profile.toRequestPayload();
        SystemProperties.set(Constants.PROP_MEMC_REQUEST, payload);
        Log.i(TAG, "MEMC session ON for " + profile.packageName + " (" + payload + ")");
    }

    private void endSession() {
        if (mSessionPackage == null) {
            return;
        }
        Log.i(TAG, "MEMC session OFF for " + mSessionPackage);
        SystemProperties.set(Constants.PROP_MEMC_REQUEST, REQUEST_OFF);
        mSessionPackage = null;
        // Restore the user's refresh-rate state (tile / per-app override / auto):
        // the monitor re-applies MIN/PEAK and adfr_min_fps from its own state.
        RefreshRateMonitorService.notifyStateChanged(this);
    }

    private void setRefreshRate(float min, float peak) {
        Settings.System.putFloatForUser(getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, min, UserHandle.USER_CURRENT);
        Settings.System.putFloatForUser(getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, peak, UserHandle.USER_CURRENT);
    }
}
