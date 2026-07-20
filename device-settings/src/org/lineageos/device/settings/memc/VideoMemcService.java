/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 *
 * Video MEMC refresh-rate pin.
 *
 * The composer auto-detects a fullscreen, MEMC-eligible video and publishes
 * vendor.display.iris.video_memc=1. Unlike the game path, nothing pins the panel
 * for video, so SurfaceFlinger keeps voting the content rate and every pending
 * refresh-rate switch tears the FRC enter down (the flicker-storm interlock in
 * the composer treats a pending rr as a teardown). This service is the missing
 * "policy": while the composer signals video MEMC, pin the panel at the FRC rate
 * (MIN=PEAK + ADFR min fps) so SF stops voting down; on clear, hand back to
 * RefreshRateMonitorService to restore the user's refresh-rate state. Symmetric
 * with MemcGameService, which pins on a foreground game instead.
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

import org.lineageos.device.settings.Constants;
import org.lineageos.device.settings.refreshrate.RefreshRateMonitorService;
import org.lineageos.device.settings.utils.FileUtils;

public class VideoMemcService extends Service {

    private static final String TAG = "VideoMemcService";

    private static volatile VideoMemcService sInstance;

    private Handler mHandler;
    private boolean mPinned = false;
    private final Runnable mEvaluate = this::evaluate;

    // ===== Lifecycle =====

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mHandler = new Handler();
        // The composer flips the prop; re-read it on any system property change.
        SystemProperties.addChangeCallback(() -> mHandler.post(mEvaluate));
        if (Constants.DEBUG) Log.i(TAG, "Service created");
        evaluate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        release();
        sInstance = null;
        if (Constants.DEBUG) Log.i(TAG, "Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mHandler.post(mEvaluate);
        return START_STICKY;
    }

    // ===== Public API =====

    /** True while the video pin holds the panel; RefreshRateMonitorService must
     *  not fight it (same contract as the game / HBM pins). */
    public static boolean isPinActive() {
        VideoMemcService instance = sInstance;
        return instance != null && instance.mPinned;
    }

    public static void notifyStateChanged(Context context) {
        VideoMemcService instance = sInstance;
        if (instance == null) {
            try {
                context.startService(new Intent(context, VideoMemcService.class));
                if (Constants.DEBUG) Log.i(TAG, "Service started");
            } catch (Exception e) {
                Log.e(TAG, "Failed to start service", e);
            }
            return;
        }
        instance.mHandler.post(instance.mEvaluate);
    }

    // ===== State handling =====

    private void evaluate() {
        final boolean want =
                "1".equals(SystemProperties.get(Constants.PROP_VIDEO_MEMC, "0"));
        if (want) {
            pin();
        } else {
            release();
        }
    }

    private void pin() {
        if (mPinned) {
            return;
        }
        mPinned = true;
        // Same pin as MemcGameService: kernel self-refresh floor + SF MIN=PEAK so
        // SF stops voting the content rate and the FRC enter can hold.
        FileUtils.writeLine(Constants.NODE_ADFR_MIN_FPS,
                String.valueOf(Constants.MEMC_PIN_REFRESH_RATE));
        setRefreshRate(Constants.MEMC_PIN_REFRESH_RATE, Constants.MEMC_PIN_REFRESH_RATE);
        Log.i(TAG, "Video MEMC pin ON");
    }

    private void release() {
        if (!mPinned) {
            return;
        }
        mPinned = false;
        Log.i(TAG, "Video MEMC pin OFF");
        // A game session may still own the pin; the monitor is the single source of
        // truth and re-applies the correct state (it checks the pins via isPinActive).
        RefreshRateMonitorService.notifyStateChanged(this);
    }

    private void setRefreshRate(float min, float peak) {
        Settings.System.putFloatForUser(getContentResolver(),
                Settings.System.MIN_REFRESH_RATE, min, UserHandle.USER_CURRENT);
        Settings.System.putFloatForUser(getContentResolver(),
                Settings.System.PEAK_REFRESH_RATE, peak, UserHandle.USER_CURRENT);
    }
}
