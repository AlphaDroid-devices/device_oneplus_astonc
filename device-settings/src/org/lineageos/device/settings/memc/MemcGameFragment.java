/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 *
 * Per-game MEMC enable list: installed profiled games with toggles.
 * Master toggle lives on DeviceSettings; this screen is opt-out per game.
 */

package org.lineageos.device.settings.memc;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import org.lineageos.device.settings.Constants;
import org.lineageos.device.settings.R;

import java.util.List;

public class MemcGameFragment extends PreferenceFragmentCompat {

    private static final String TAG = "MemcGameFragment";
    private static final String KEY_LIST = "memc_game_apps_list";
    private static final String KEY_EMPTY = "memc_game_apps_empty";

    private PreferenceCategory mListCategory;
    private Preference mEmptyPref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.memc_game_preferences, rootKey);
        mListCategory = findPreference(KEY_LIST);
        mEmptyPref = findPreference(KEY_EMPTY);
        if (mListCategory != null) {
            mListCategory.setOrderingAsAdded(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        if (mListCategory == null) {
            return;
        }
        mListCategory.removeAll();

        List<MemcGameProfiles.Profile> installed =
                MemcGameProfiles.getInstalled(requireContext());
        PackageManager pm = requireContext().getPackageManager();

        if (installed.isEmpty()) {
            if (mEmptyPref != null) {
                mEmptyPref.setVisible(true);
            }
            return;
        }
        if (mEmptyPref != null) {
            mEmptyPref.setVisible(false);
        }

        for (MemcGameProfiles.Profile profile : installed) {
            SwitchPreferenceCompat pref = new SwitchPreferenceCompat(requireContext());
            pref.setKey("memc_app_" + profile.packageName);
            pref.setPersistent(false);
            try {
                pref.setTitle(pm.getApplicationInfo(profile.packageName, 0).loadLabel(pm));
                pref.setIcon(pm.getApplicationInfo(profile.packageName, 0).loadIcon(pm));
            } catch (PackageManager.NameNotFoundException e) {
                pref.setTitle(profile.packageName);
            }
            pref.setSummary(profile.packageName);
            pref.setChecked(MemcGameProfiles.isAppEnabled(requireContext(), profile.packageName));
            pref.setOnPreferenceChangeListener((p, newValue) -> {
                boolean enabled = (Boolean) newValue;
                MemcGameProfiles.setAppEnabled(requireContext(), profile.packageName, enabled);
                // Reconcile live session if this game is currently foreground.
                MemcGameService.notifyStateChanged(requireContext());
                if (Constants.DEBUG) {
                    Log.i(TAG, profile.packageName + " -> " + enabled);
                }
                return true;
            });
            mListCategory.addPreference(pref);
        }
    }
}
