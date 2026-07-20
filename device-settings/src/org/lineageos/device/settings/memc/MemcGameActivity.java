/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 *
 * Host activity for MemcGameFragment — same pattern as BypassChargingActivity /
 * RefreshRateActivity. DeviceSettings is an EXTRA_SETTINGS inject into AOSP
 * Settings; nested android:fragment alone is unreliable there, so the per-game
 * list is also launchable as its own activity (and via explicit intent).
 */

package org.lineageos.device.settings.memc;

import android.os.Bundle;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

import org.lineageos.device.settings.R;

public class MemcGameActivity extends CollapsingToolbarBaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_memc_game);
        setTitle(getString(R.string.memc_game_apps_title));
    }
}
