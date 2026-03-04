package de.tyro.mcnetwork.util;

public class MathUtil {
    public static float max(float... values) {
        float max = Float.MIN_VALUE;
        for (float value : values) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    public static float  min(float... values) {
        float min = Float.MAX_VALUE;
        for (float value : values) {
            if (value < min) {
                min = value;
            }
        }
        return min;
    }
}
