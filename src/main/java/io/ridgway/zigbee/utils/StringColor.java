package io.ridgway.zigbee.utils;

public class StringColor {

    private enum Color {
        ANSI_RESET("\u001B[0m"),
        ANSI_BLACK("\u001B[30m"),
        ANSI_RED("\u001B[31m"),
        ANSI_GREEN("\u001B[32m"),
        ANSI_YELLOW("\u001B[33m"),
        ANSI_BLUE("\u001B[34m"),
        ANSI_PURPLE("\u001B[35m"),
        ANSI_CYAN("\u001B[36m"),
        ANSI_WHITE("\u001B[37m");

        private final String code;

        Color(final String code) {
            this.code = code;
        }

        private String getCode() {
            return code;
        }
    }

    private StringColor() {
    }

    private static String color(final Color color, final String message) {
        return color.getCode() + message + Color.ANSI_RESET.getCode();
    }

    public static String yellow(final String message) {
        return color(Color.ANSI_YELLOW, message);
    }

    public static String blue(final String message) {
        return color(Color.ANSI_BLUE, message);
    }

    public static String green(final String message) {
        return color(Color.ANSI_GREEN, message);
    }

    public static String red(final String message) {
        return color(Color.ANSI_RED, message);
    }

    public static String black(final String message) {
        return color(Color.ANSI_BLACK, message);
    }

    public static String purple(final String message) {
        return color(Color.ANSI_PURPLE, message);
    }

    public static String cyan(final String message) {
        return color(Color.ANSI_CYAN, message);
    }

    public static String white(final String message) {
        return color(Color.ANSI_WHITE, message);
    }

}
