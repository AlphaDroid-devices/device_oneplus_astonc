/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.device.settings.ui.screens

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.axion.compose.preferences.ClickablePreference
import com.android.axion.compose.preferences.PreferenceGroup
import com.android.axion.compose.preferences.SwitchPreference
import org.lineageos.device.settings.Constants
import org.lineageos.device.settings.R
import org.lineageos.device.settings.memc.MemcGameProfiles
import org.lineageos.device.settings.memc.MemcGameService
import org.lineageos.device.settings.ui.AppIcon
import org.lineageos.device.settings.ui.OnResume
import org.lineageos.device.settings.ui.SettingsScroll
import org.lineageos.device.settings.ui.appLabel

private const val TAG = "MemcGameScreen"

@Composable
fun MemcGameScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    var installed by remember { mutableStateOf(MemcGameProfiles.getInstalled(context)) }
    var enabledMap by remember {
        mutableStateOf(
            installed.associate { it.packageName to MemcGameProfiles.isAppEnabled(context, it.packageName) },
        )
    }

    OnResume {
        installed = MemcGameProfiles.getInstalled(context)
        enabledMap = installed.associate {
            it.packageName to MemcGameProfiles.isAppEnabled(context, it.packageName)
        }
    }

    val listed = installed
    val enabled = enabledMap

    SettingsScroll(contentPadding) {
        if (listed.isEmpty()) {
            PreferenceGroup {
                item {
                    ClickablePreference(
                        title = stringResource(R.string.memc_game_apps_empty),
                        summary = stringResource(R.string.memc_game_apps_empty_summary),
                        enabled = false,
                        onClick = {},
                    )
                }
            }
        } else {
            PreferenceGroup(title = stringResource(R.string.memc_game_apps_category)) {
                listed.forEach { profile ->
                    item {
                        SwitchPreference(
                            title = appLabel(context, profile.packageName)
                                .ifEmpty { profile.packageName },
                            summary = profile.packageName,
                            checked = enabled[profile.packageName] == true,
                            customIcon = { AppIcon(profile.packageName) },
                            onCheckedChange = { enable ->
                                MemcGameProfiles.setAppEnabled(
                                    context,
                                    profile.packageName,
                                    enable,
                                )
                                MemcGameService.notifyStateChanged(context)
                                enabledMap = enabledMap + (profile.packageName to enable)
                                if (Constants.DEBUG) {
                                    Log.i(TAG, "${profile.packageName} -> $enable")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
