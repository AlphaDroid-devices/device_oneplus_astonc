/*
 * SPDX-FileCopyrightText: 2026 AlphaDroid
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.device.settings.memc

import android.os.Bundle
import androidx.activity.ComponentActivity
import org.lineageos.device.settings.ui.DeviceSettingsDest
import org.lineageos.device.settings.ui.setDeviceSettingsContent

class MemcGameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setDeviceSettingsContent(DeviceSettingsDest.Memc)
    }
}
