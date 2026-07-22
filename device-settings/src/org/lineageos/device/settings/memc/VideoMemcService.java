/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 *
 * Video MEMC (Video Enhancement) master toggle management.
 *
 * Persists the user's choice in SharedPreferences and mirrors it onto
 * persist.sys.display.iris.auto_memc so the HAL composer knows whether
 * auto video MEMC is enabled.
 */

package org.lineageos.device.settings.memc;

import android.content.Context;
import android.os.SystemProperties;

import androidx.preference.PreferenceManager;

import org.lineageos.device.settings.Constants;

public class VideoMemcService {

    private static final String TAG = "VideoMemcService";

    // ===== Master enable (Video Enhancement toggle) =====

    /** SharedPreferences source of truth; default on to match the composer's
     *  unset-prop default ("1"). */
    public static boolean isMasterEnabled(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(Constants.KEY_MEMC_VIDEO, true);
    }

    /** Persist the choice and mirror it to the composer gate. */
    public static void setMasterEnabled(Context context, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(Constants.KEY_MEMC_VIDEO, enabled)
                .apply();
        applyMasterEnable(context);
    }

    /** Push the persisted state onto the composer gate (called at boot too). */
    public static void applyMasterEnable(Context context) {
        SystemProperties.set(Constants.PROP_AUTO_MEMC, isMasterEnabled(context) ? "1" : "0");
    }
}

