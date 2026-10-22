import javafx.scene.Scene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class Theme {

    private static String url;

    private Theme() {
    }

    public static String url() {
        if (url == null) {
            try {
                Path p = Files.createTempFile("forkjoin-theme", ".css");
                Files.writeString(p, CSS);
                p.toFile().deleteOnExit();
                url = p.toUri().toString();
            } catch (IOException e) {
                throw new IllegalStateException("Theme could not be loaded", e);
            }
        }
        return url;
    }

    public static void apply(Scene scene) {
        scene.getStylesheets().add(url());
    }

    private static final String CSS = """
        .root {
            -bg: #f4f5f7;
            -line: #d8dbe0;
            -field: #c9ccd2;
            -text: #2b2f36;
            -muted: #6b7280;
            -accent: #3b6fd4;
            -accent-dark: #2f5bb5;
            -fx-font-family: "Segoe UI", "Helvetica Neue", Arial, sans-serif;
            -fx-font-size: 13px;
            -fx-background-color: -bg;
            -fx-focus-color: transparent;
            -fx-faint-focus-color: transparent;
        }

        .label { -fx-text-fill: -text; }
        .muted { -fx-text-fill: -muted; }

        .app-title { -fx-font-size: 22px; -fx-font-weight: bold; }
        .app-subtitle { -fx-text-fill: -muted; -fx-font-size: 12px; }
        .footer { -fx-text-fill: #9aa0aa; -fx-font-size: 11px; }

        .card {
            -fx-background-color: white;
            -fx-background-radius: 8;
            -fx-border-color: -line;
            -fx-border-radius: 8;
            -fx-padding: 12;
        }
        .hint { -fx-text-fill: -muted; -fx-font-size: 12px; -fx-font-weight: bold; }
        .key { -fx-text-fill: -text; -fx-font-weight: bold; }
        .stats { -fx-text-fill: -muted; -fx-font-size: 12px; }

        /* ---------- inputs ---------- */
        .text-field {
            -fx-background-color: white;
            -fx-text-fill: -text;
            -fx-prompt-text-fill: #aab0ba;
            -fx-background-radius: 4;
            -fx-border-color: -field;
            -fx-border-radius: 4;
            -fx-padding: 6 8;
            -fx-highlight-fill: #bcd0f5;
            -fx-highlight-text-fill: -text;
        }
        .text-field:focused { -fx-border-color: -accent; }

        .seg {
            -fx-background-color: #eceef2;
            -fx-text-fill: -text;
            -fx-background-radius: 4;
            -fx-background-insets: 0;
            -fx-border-color: -field;
            -fx-border-radius: 4;
            -fx-padding: 6 12;
            -fx-cursor: hand;
        }
        .seg:hover { -fx-background-color: #e2e5ea; }
        .seg:selected {
            -fx-background-color: -accent;
            -fx-text-fill: white;
            -fx-border-color: -accent;
        }

        /* ---------- buttons ---------- */
        .button {
            -fx-background-color: #eceef2;
            -fx-text-fill: -text;
            -fx-background-radius: 4;
            -fx-background-insets: 0;
            -fx-border-color: -field;
            -fx-border-radius: 4;
            -fx-cursor: hand;
            -fx-padding: 6 14;
        }
        .button:hover { -fx-background-color: #e2e5ea; }
        .button:disabled { -fx-opacity: 0.45; }

        .btn-primary, .btn-play {
            -fx-background-color: -accent;
            -fx-text-fill: white;
            -fx-border-color: -accent;
        }
        .btn-primary:hover, .btn-play:hover { -fx-background-color: -accent-dark; }

        /* ---------- sliders ---------- */
        .slider .track {
            -fx-background-color: -line;
            -fx-background-radius: 3;
            -fx-background-insets: 0;
            -fx-padding: 2;
        }
        .slider .thumb {
            -fx-background-color: -accent;
            -fx-background-radius: 20;
            -fx-background-insets: 0;
            -fx-padding: 7;
        }

        /* ---------- messages ---------- */
        .status-ok { -fx-text-fill: #2e7d32; }
        .status-warn { -fx-text-fill: #b26a00; }

        .log {
            -fx-background-color: #fff8e1;
            -fx-border-color: #ecd98f;
            -fx-background-radius: 4;
            -fx-border-radius: 4;
            -fx-padding: 8 10;
            -fx-text-fill: #5a4a10;
        }
        .side-range { -fx-font-size: 20px; -fx-font-weight: bold; }

        /* ---------- tree area ---------- */
        .scroll-pane { -fx-background-color: transparent; }
        .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-pane > .corner { -fx-background-color: transparent; }
        .viz-scroll {
            -fx-background-color: white;
            -fx-background-radius: 6;
            -fx-border-color: -line;
            -fx-border-radius: 6;
        }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .track { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: #c9ccd2; -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; -fx-background-color: transparent; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-padding: 0; -fx-shape: " "; }

        /* ---------- combo box ---------- */
        .combo-box {
            -fx-background-color: #eceef2;
            -fx-background-radius: 4;
            -fx-background-insets: 0;
            -fx-border-color: -field;
            -fx-border-radius: 4;
            -fx-cursor: hand;
        }
        .combo-box:hover { -fx-background-color: #e2e5ea; }
        .combo-box .list-cell { -fx-text-fill: #2b2f36; -fx-background-color: transparent; }
        .combo-box .arrow-button { -fx-background-color: transparent; }
        .combo-box .arrow { -fx-background-color: #6b7280; }
        .combo-box-popup > .list-view {
            -fx-background-color: white;
            -fx-border-color: #d8dbe0;
        }
        .combo-box-popup .list-cell { -fx-text-fill: #2b2f36; -fx-background-color: transparent; -fx-padding: 6 12; }
        .combo-box-popup .list-cell:filled:hover { -fx-background-color: #eef2fb; }
        .combo-box-popup .list-cell:filled:selected { -fx-background-color: #3b6fd4; -fx-text-fill: white; }

        /* ---------- dialogs ---------- */
        .dialog-pane { -fx-background-color: white; }
        .dialog-pane .label { -fx-text-fill: #2b2f36; }
        .dialog-pane > .button-bar > .container > .button {
            -fx-background-color: #3b6fd4;
            -fx-text-fill: white;
            -fx-background-radius: 4;
        }
        .tooltip { -fx-background-color: #2b2f36; -fx-text-fill: white; -fx-background-radius: 4; }
        """;
}