#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Inherit from those products. Most specific first.
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit_only.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/full_base_telephony.mk)

# Inherit from astonc device
$(call inherit-product, device/oneplus/astonc/device.mk)

# Inherit some common Lineage stuff.
$(call inherit-product, vendor/alpha/config/common_full_phone.mk)

PRODUCT_NAME := alpha_astonc
PRODUCT_DEVICE := astonc
PRODUCT_MANUFACTURER := OnePlus
PRODUCT_BRAND := OnePlus
PRODUCT_MODEL := PJE110

PRODUCT_GMS_CLIENTID_BASE := android-oneplus

PRODUCT_BUILD_PROP_OVERRIDES += \
    BuildDesc="qssi-user 16 BP2A.250605.015 1765893023217 release-keys" \
    BuildFingerprint=OnePlus/PJE110/OP5CF9L1:16/TP1A.220905.001/U.1531cfa_c5893d_c5e3d3:user/release-keys \
    DeviceName=OP5CF9L1 \
    DeviceProduct=OP5CF9L1 \
    SystemDevice=OP5CF9L1 \
    SystemName=OP5CF9L1

# Prebuilt DTB
TARGET_USES_PREBUILT_DTB := false

# Device config
TARGET_HAS_UDFPS := true
TARGET_ENABLE_BLUR := true
TARGET_EXCLUDES_AUDIOFX := false
TARGET_FACE_UNLOCK_SUPPORTED := true

# Build config

# TARGET_BUILD_PACKAGE options:
# 1 - vanilla (default)
# 2 - microg
# 3 - gapps
TARGET_BUILD_PACKAGE := 3

ifeq ($(TARGET_BUILD_PACKAGE),3)
  # (valid only for GAPPS builds)
  TARGET_INCLUDE_GOOGLE_COMMS := true
  TARGET_INCLUDE_PIXEL_LAUNCHER := true
  TARGET_SUPPORTS_QUICK_TAP := true
  TARGET_SUPPORTS_CALL_RECORDING := true
  TARGET_INCLUDE_STOCK_ARCORE := true
  TARGET_INCLUDE_LIVE_WALLPAPERS := true
  TARGET_SUPPORTS_GOOGLE_RECORDER := false
endif

# Debugging
TARGET_INCLUDE_MATLOG := false
WITH_ADB_INSECURE := false
TARGET_BUILD_PERMISSIVE := false

# Extras
TARGET_INCLUDE_SIMPLE_TUNE := true
TARGET_PREBUILT_BCR := true

# Maintainer
ALPHA_BUILD_TYPE := Official
ALPHA_MAINTAINER := elpaablo