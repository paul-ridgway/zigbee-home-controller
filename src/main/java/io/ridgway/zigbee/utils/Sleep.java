package io.ridgway.zigbee.utils;

public class Sleep {

    private Sleep() {
    }

    public static void milliseconds(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException ignored) {
        }
    }

}

