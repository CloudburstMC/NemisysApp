package com.nukkitx.network;

import lombok.experimental.UtilityClass;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
@UtilityClass
public final class NativeUtils {
    private static final boolean REUSEPORT_AVAILABLE = false;

    public static boolean isReusePortAvailable() {
        return REUSEPORT_AVAILABLE;
    }
}
