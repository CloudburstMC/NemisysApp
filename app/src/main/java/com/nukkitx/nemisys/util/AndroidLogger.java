package com.nukkitx.nemisys.util;

public class AndroidLogger {

    public static void log(String message) {
        System.out.println(message);
    }

    public static void debug(String message) {
        System.out.println(message);
    }

    public static void warn(String message) {
        System.out.println(message);
    }

    public static void error(String message) {
        System.out.println(message);
    }

    public static void error(String message, Throwable throwable) {
        System.out.println(message);
        throwable.printStackTrace();
    }

    public static boolean isDebuggingMode() {
        return true;
    }
}
