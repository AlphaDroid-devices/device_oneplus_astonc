/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 *
 * Per-game Iris 7P MEMC profiles, loaded from res/xml/memc_game_profiles.xml
 * (imported from stock ACE3_400 multimedia_pixelworks_game_apps.xml IMV list).
 */

package org.lineageos.device.settings.memc;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.PreferenceManager;

import org.lineageos.device.settings.Constants;
import org.lineageos.device.settings.R;

import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MemcGameProfiles {

    private static final String TAG = "MemcGameProfiles";

    public static final class Profile {
        public final String packageName;
        public final int scene;
        public final int id;
        public final int fps;

        Profile(String packageName, int scene, int id, int fps) {
            this.packageName = packageName;
            this.scene = scene;
            this.id = id;
            this.fps = fps;
        }

        /** irisConfigureSet(258) payload for the composer request engine. */
        public String toRequestPayload() {
            // The panel is pinned at MEMC_PIN_REFRESH_RATE and the chip interpolates
            // 2x, so the render cadence the FRC gate locks onto is pin/2 (60 for a
            // 120 pin). The XML fps is the stock placeholder (45 for most titles) and
            // must not be published verbatim, or FRC never locks when the game renders
            // at 60 - only the handful of fps=60 entries would ever engage.
            int inputFps = Constants.MEMC_PIN_REFRESH_RATE / 2;
            return "10,-1," + scene + ",-1," + id + "," + inputFps;
        }
    }

    private static Map<String, Profile> sProfiles;

    private MemcGameProfiles() {
    }

    public static synchronized Profile get(Context context, String packageName) {
        ensureLoaded(context);
        return sProfiles.get(packageName);
    }

    /** All profiles from XML (not filtered by install state). */
    public static synchronized Map<String, Profile> getAll(Context context) {
        ensureLoaded(context);
        return Collections.unmodifiableMap(sProfiles);
    }

    /**
     * Profiled games that are installed on this device, sorted by label.
     * Used by the per-game enable list UI.
     */
    public static List<Profile> getInstalled(Context context) {
        ensureLoaded(context);
        PackageManager pm = context.getPackageManager();
        List<Profile> installed = new ArrayList<>();
        for (Profile profile : sProfiles.values()) {
            try {
                pm.getApplicationInfo(profile.packageName, 0);
                installed.add(profile);
            } catch (PackageManager.NameNotFoundException ignored) {
                // not installed
            }
        }
        installed.sort((a, b) -> {
            String la = labelOf(pm, a.packageName);
            String lb = labelOf(pm, b.packageName);
            return la.compareToIgnoreCase(lb);
        });
        return installed;
    }

    /**
     * Per-game enable (opt-out model): master toggle is separate; a profiled
     * game is allowed unless the user turned it off in the games list.
     */
    public static boolean isAppEnabled(Context context, String packageName) {
        if (packageName == null) {
            return false;
        }
        return !loadDisabledSet(context).contains(packageName);
    }

    public static void setAppEnabled(Context context, String packageName, boolean enabled) {
        if (packageName == null) {
            return;
        }
        Set<String> disabled = loadDisabledSet(context);
        if (enabled) {
            disabled.remove(packageName);
        } else {
            disabled.add(packageName);
        }
        saveDisabledSet(context, disabled);
        if (Constants.DEBUG) {
            Log.i(TAG, packageName + " MEMC " + (enabled ? "enabled" : "disabled"));
        }
    }

    private static Set<String> loadDisabledSet(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String raw = prefs.getString(Constants.KEY_MEMC_GAME_DISABLED, "");
        Set<String> set = new HashSet<>();
        if (TextUtils.isEmpty(raw)) {
            return set;
        }
        for (String pkg : TextUtils.split(raw, "\\|")) {
            if (!TextUtils.isEmpty(pkg)) {
                set.add(pkg);
            }
        }
        return set;
    }

    private static void saveDisabledSet(Context context, Set<String> disabled) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(Constants.KEY_MEMC_GAME_DISABLED, TextUtils.join("|", disabled))
                .apply();
    }

    private static String labelOf(PackageManager pm, String packageName) {
        try {
            return pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return packageName;
        }
    }

    private static void ensureLoaded(Context context) {
        if (sProfiles == null) {
            sProfiles = load(context);
        }
    }

    private static Map<String, Profile> load(Context context) {
        Map<String, Profile> profiles = new HashMap<>();
        try (XmlResourceParser parser =
                context.getResources().getXml(R.xml.memc_game_profiles)) {
            int event;
            while ((event = parser.next()) != XmlPullParser.END_DOCUMENT) {
                if (event != XmlPullParser.START_TAG || !"game".equals(parser.getName())) {
                    continue;
                }
                String pkg = parser.getAttributeValue(null, "package");
                if (pkg == null || pkg.isEmpty()) {
                    continue;
                }
                int scene = parser.getAttributeIntValue(null, "scene", 50);
                int id = parser.getAttributeIntValue(null, "id", -1);
                int fps = parser.getAttributeIntValue(null, "fps", 0);
                if (id < 0) {
                    continue;
                }
                profiles.put(pkg, new Profile(pkg, scene, id, fps));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse memc_game_profiles", e);
        }
        if (Constants.DEBUG) Log.i(TAG, "Loaded " + profiles.size() + " game profiles");
        return profiles;
    }
}
