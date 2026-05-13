package com.mindforge;

public class OpenCVLoader {
    private static boolean loaded = false;

    public static synchronized void load() {
        if (!loaded) {
            System.load("C:\\opencv\\build\\java\\x64\\opencv_java4120.dll");
            loaded = true;
        }
    }
}