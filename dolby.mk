#
# Copyright (C) 2022 FlamingoOS Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Soong Namespace
PRODUCT_SOONG_NAMESPACES += \
   hardware/dolby

# Enable codec support
AUDIO_FEATURE_ENABLED_DS2_DOLBY_DAP := true

# SEPolicy
BOARD_VENDOR_SEPOLICY_DIRS += hardware/dolby/sepolicy/vendor

# HIDL
DEVICE_FRAMEWORK_COMPATIBILITY_MATRIX_FILE += hardware/dolby/dolby_framework_matrix.xml
DEVICE_MANIFEST_FILE += hardware/dolby/vendor.dolby.hardware.dms@2.0-service.xml
    
# Moto Dolby
PRODUCT_COPY_FILES += \
    hardware/dolby/configs/dax-default.xml:$(TARGET_COPY_OUT_VENDOR)/etc/dolby/dax-default.xml \
    hardware/dolby/configs/media/media_codecs.xml:$(TARGET_COPY_OUT_VENDOR)/etc/media_codecs.xml \
    hardware/dolby/configs/media/media_codecs_dolby_audio.xml:$(TARGET_COPY_OUT_VENDOR)/etc/media_codecs_dolby_audio.xml \
    hardware/dolby/configs/audio/audio_effects.xml:$(TARGET_COPY_OUT_VENDOR)/etc/audio_effects.xml
    
PRODUCT_COPY_FILES += \
    prebuilts/vndk/v33/arm/arch-arm-armv7-a-neon/shared/vndk-core/libstagefright_foundation.so:$(TARGET_COPY_OUT_VENDOR)/lib/libstagefright_foundation-v33.so \
    prebuilts/vndk/v33/arm64/arch-arm64-armv8-a/shared/vndk-core/libstagefright_foundation.so:$(TARGET_COPY_OUT_VENDOR)/lib64/libstagefright_foundation-v33.so
    
PRODUCT_PACKAGES += \
    DolbyFrameworksResCommon

TARGET_EXCLUDES_AUDIOFX := true

PRODUCT_VENDOR_PROPERTIES += \
    persist.vendor.audio_fx.current=dolby \
    vendor.audio.dolby.ds2.enabled=true \
    vendor.audio.dolby.ds2.hardbypass=true \
    ro.vendor.audio.dolby.dax.support=true \
    ro.vendor.dolby.dax.version=DAX3_3.6.0.12_r1 \
   
PRODUCT_PACKAGES += \
    MotoSignatureApp \
    MotoDolbyDax3 \
    MotorolaSettingsProvider \
    daxService \
    com.motorola.frameworks.core.addon \
    com.motorola.motosignature \
    moto-checkin \
    moto-settings \
    moto
   
# proprietary blobs
PRODUCT_COPY_FILES += \
    hardware/dolby/proprietary/odm/bin/hw/vendor.dolby_v3_6.hardware.dms360@2.0-service:$(TARGET_COPY_OUT_ODM)/bin/hw/vendor.dolby_v3_6.hardware.dms360@2.0-service \
    hardware/dolby/proprietary/system_ext/etc/permissions/com.motorola.frameworks.core.addon.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/com.motorola.frameworks.core.addon.xml \
    hardware/dolby/proprietary/system_ext/etc/permissions/com.motorola.motosignature.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/com.motorola.motosignature.xml \
    hardware/dolby/proprietary/system_ext/etc/permissions/com.motorola.software.dolbyui.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/com.motorola.software.dolbyui.xml \
    hardware/dolby/proprietary/system_ext/etc/permissions/moto-checkin.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/moto-checkin.xml \
    hardware/dolby/proprietary/system_ext/etc/permissions/moto-settings.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/moto-settings.xml \
    hardware/dolby/proprietary/system_ext/etc/permissions/moto.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/moto.xml \
    hardware/dolby/proprietary/system_ext/etc/permissions/privapp-com.dolby.daxservice.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-com.dolby.daxservice.xml \
    hardware/dolby/proprietary/system_ext/etc/permissions/privapp-com.motorola.android.providers.settings.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-com.motorola.android.providers.settings.xml \
    hardware/dolby/proprietary/system_ext/etc/permissions/privapp-com.motorola.dolby.dolbyui.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/permissions/privapp-com.motorola.dolby.dolbyui.xml \
    hardware/dolby/proprietary/system_ext/etc/sysconfig/config-com.dolby.daxservice.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/sysconfig/config-com.dolby.daxservice.xml \
    hardware/dolby/proprietary/system_ext/etc/sysconfig/config-com.motorola.android.providers.settings.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/sysconfig/config-com.motorola.android.providers.settings.xml \
    hardware/dolby/proprietary/system_ext/etc/sysconfig/config-com.motorola.dolby.dolbyui.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/sysconfig/config-com.motorola.dolby.dolbyui.xml \
    hardware/dolby/proprietary/system_ext/etc/sysconfig/config-com.motorola.motosignature.app.xml:$(TARGET_COPY_OUT_SYSTEM_EXT)/etc/sysconfig/config-com.motorola.motosignature.app.xml \
    hardware/dolby/proprietary/vendor/bin/hw/vendor.dolby.hardware.dms@2.0-service:$(TARGET_COPY_OUT_VENDOR)/bin/hw/vendor.dolby.hardware.dms@2.0-service \
    hardware/dolby/proprietary/vendor/etc/init/vendor.dolby.hardware.dms@2.0-service.rc:$(TARGET_COPY_OUT_VENDOR)/etc/init/vendor.dolby.hardware.dms@2.0-service.rc \
    hardware/dolby/proprietary/vendor/lib/libdapparamstorage.so:$(TARGET_COPY_OUT_VENDOR)/lib/libdapparamstorage.so \
    hardware/dolby/proprietary/vendor/lib/libdeccfg.so:$(TARGET_COPY_OUT_VENDOR)/lib/libdeccfg.so \
    hardware/dolby/proprietary/vendor/lib/libstagefright_soft_ac4dec.so:$(TARGET_COPY_OUT_VENDOR)/lib/libstagefright_soft_ac4dec.so \
    hardware/dolby/proprietary/vendor/lib/libstagefright_soft_ddpdec.so:$(TARGET_COPY_OUT_VENDOR)/lib/libstagefright_soft_ddpdec.so \
    hardware/dolby/proprietary/vendor/lib/libstagefrightdolby.so:$(TARGET_COPY_OUT_VENDOR)/lib/libstagefrightdolby.so \
    hardware/dolby/proprietary/vendor/lib/soundfx/libswdap.so:$(TARGET_COPY_OUT_VENDOR)/lib/soundfx/libswdap.so \
    hardware/dolby/proprietary/vendor/lib/soundfx/libswgamedap.so:$(TARGET_COPY_OUT_VENDOR)/lib/soundfx/libswgamedap.so \
    hardware/dolby/proprietary/vendor/lib/soundfx/libswvqe.so:$(TARGET_COPY_OUT_VENDOR)/lib/soundfx/libswvqe.so \
    hardware/dolby/proprietary/vendor/lib/vendor.dolby.hardware.dms@2.0.so:$(TARGET_COPY_OUT_VENDOR)/lib/vendor.dolby.hardware.dms@2.0.so \
    hardware/dolby/proprietary/vendor/lib64/libdapparamstorage.so:$(TARGET_COPY_OUT_VENDOR)/lib64/libdapparamstorage.so \
    hardware/dolby/proprietary/vendor/lib64/libdeccfg.so:$(TARGET_COPY_OUT_VENDOR)/lib64/libdeccfg.so \
    hardware/dolby/proprietary/vendor/lib64/libdlbdsservice.so:$(TARGET_COPY_OUT_VENDOR)/lib64/libdlbdsservice.so \
    hardware/dolby/proprietary/vendor/lib64/libstagefright_soft_ac4dec.so:$(TARGET_COPY_OUT_VENDOR)/lib64/libstagefright_soft_ac4dec.so \
    hardware/dolby/proprietary/vendor/lib64/libstagefright_soft_ddpdec.so:$(TARGET_COPY_OUT_VENDOR)/lib64/libstagefright_soft_ddpdec.so \
    hardware/dolby/proprietary/vendor/lib64/libstagefrightdolby.so:$(TARGET_COPY_OUT_VENDOR)/lib64/libstagefrightdolby.so \
    hardware/dolby/proprietary/vendor/lib64/soundfx/libswdap.so:$(TARGET_COPY_OUT_VENDOR)/lib64/soundfx/libswdap.so \
    hardware/dolby/proprietary/vendor/lib64/soundfx/libswgamedap.so:$(TARGET_COPY_OUT_VENDOR)/lib64/soundfx/libswgamedap.so \
    hardware/dolby/proprietary/vendor/lib64/soundfx/libswvqe.so:$(TARGET_COPY_OUT_VENDOR)/lib64/soundfx/libswvqe.so \
    hardware/dolby/proprietary/vendor/lib64/vendor.dolby.hardware.dms@2.0-impl.so:$(TARGET_COPY_OUT_VENDOR)/lib64/vendor.dolby.hardware.dms@2.0-impl.so \
    hardware/dolby/proprietary/vendor/lib64/vendor.dolby.hardware.dms@2.0.so:$(TARGET_COPY_OUT_VENDOR)/lib64/vendor.dolby.hardware.dms@2.0.so
