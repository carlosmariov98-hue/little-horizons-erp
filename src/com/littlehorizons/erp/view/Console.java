package com.littlehorizons.erp.view;

/**
 * Helpers to print titles and messages with a consistent look.
 */
public final class Console {

    private static final int BANNER_WIDTH = 46;

    private Console() {
    }

    public static void banner(String... lines) {
        String border = "═".repeat(BANNER_WIDTH);
        System.out.println("╔" + border + "╗");
        for (String line : lines) {
            System.out.println("║" + String.format("%-" + BANNER_WIDTH + "s", "   " + line) + "║");
        }
        System.out.println("╚" + border + "╝");
    }

    public static void title(String text) {
        System.out.println();
        System.out.println(text);
        System.out.println("-".repeat(text.length()));
    }

    public static void info(String message) {
        System.out.println(message);
    }

    public static void success(String message) {
        System.out.println("[ÉXITO] " + message);
    }

    public static void error(String message) {
        System.out.println("[ERROR] " + message);
    }
}
