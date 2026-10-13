package mn.edu.must;

import javafx.scene.Scene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Light pastel theme (JavaFX CSS).
 * The CSS is written to a temp file at start-up, so no resources folder
 * or build configuration is needed. (Text blocks need Java 15+.)
 */
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
            -bg: #fffaf4;
            -line: #efe3d6;
            -text: #4a4458;
            -muted: #9a90a8;
            -pink: #ff9eb5;
            -pink-dark: #f27f9f;
            -lav: #d8ccff;
            -fx-font-family: "Segoe UI", "SF Pro Text", "Helvetica Neue", Arial, sans-serif;
            -fx-font-size: 13px;
            -fx-background-color: -bg;
            -fx-focus-color: transparent;
            -fx-faint-focus-color: transparent;
        }

        .label { -fx-text-fill: -text; }
        .muted { -fx-text-fill: -muted; }

        /* ---------- header ---------- */
        .app-title { -fx-text-fill: -text; -fx-font-size: 26px; -fx-font-weight: bold; }
        .app-subtitle { -fx-text-fill: -muted; -fx-font-size: 12px; }
        .rainbow {
            -fx-background-color: linear-gradient(to right, #ffb3c6, #d8ccff, #b9ead0, #ffe7a0);
            -fx-background-radius: 4;
            -fx-min-height: 4; -fx-pref-height: 4; -fx-max-height: 4;
        }
        .footer { -fx-text-fill: #c3b8cf; -fx-font-size: 11px; }

        /* ---------- cards ---------- */
        .card {
            -fx-background-color: white;
            -fx-background-radius: 18;
            -fx-border-color: -line;
            -fx-border-radius: 18;
            -fx-padding: 16;
            -fx-effect: dropshadow(gaussian, rgba(120,90,60,0.08), 12, 0.1, 0, 3);
        }
        .hint { -fx-text-fill: -muted; -fx-font-size: 11px; -fx-font-weight: bold; }
        .key { -fx-text-fill: -muted; -fx-font-weight: bold; }
        .stats { -fx-text-fill: -muted; -fx-font-size: 12px; }

        /* ---------- inputs ---------- */
        .text-field {
            -fx-background-color: #fffdfb;
            -fx-text-fill: -text;
            -fx-prompt-text-fill: #cbbfd6;
            -fx-background-radius: 12;
            -fx-border-color: #eadfd2;
            -fx-border-radius: 12;
            -fx-padding: 8 12;
            -fx-highlight-fill: -lav;
            -fx-highlight-text-fill: -text;
        }
        .text-field:focused { -fx-border-color: -pink-dark; }

        .seg {
            -fx-background-color: #f6efe7;
            -fx-text-fill: -muted;
            -fx-background-radius: 16;
            -fx-background-insets: 0;
            -fx-padding: 7 16;
            -fx-font-weight: bold;
            -fx-cursor: hand;
        }
        .seg:hover { -fx-background-color: #efe4d8; }
        .seg:selected { -fx-background-color: -lav; -fx-text-fill: #43357a; }

        /* ---------- buttons ---------- */
        .button {
            -fx-background-radius: 20;
            -fx-background-insets: 0;
            -fx-cursor: hand;
            -fx-padding: 8 18;
            -fx-font-weight: bold;
        }
        .btn-primary {
            -fx-background-color: -pink;
            -fx-text-fill: white;
            -fx-effect: dropshadow(gaussian, rgba(255,158,181,0.55), 10, 0.2, 0, 3);
        }
        .btn-primary:hover { -fx-background-color: -pink-dark; }
        .btn-primary:pressed { -fx-translate-y: 1; }

        .btn-ghost {
            -fx-background-color: transparent;
            -fx-border-color: #e6d9cb;
            -fx-border-radius: 20;
            -fx-text-fill: -muted;
        }
        .btn-ghost:hover { -fx-background-color: #f6efe7; }

        .btn-ctrl {
            -fx-background-color: #f6efe7;
            -fx-text-fill: -text;
        }
        .btn-ctrl:hover { -fx-background-color: #efe4d8; }
        .btn-ctrl:disabled { -fx-opacity: 0.4; }

        .btn-play {
            -fx-background-color: #b9ead0;
            -fx-text-fill: #1d5c40;
            -fx-padding: 8 22;
        }
        .btn-play:hover { -fx-background-color: #a5e0c2; }
        .btn-play:disabled { -fx-opacity: 0.4; }

        /* ---------- sliders ---------- */
        .slider .track {
            -fx-background-color: #efe3d6;
            -fx-background-radius: 6;
            -fx-background-insets: 0;
            -fx-padding: 3;
        }
        .slider .thumb {
            -fx-background-color: -pink;
            -fx-background-radius: 20;
            -fx-background-insets: 0;
            -fx-padding: 8;
            -fx-border-color: white;
            -fx-border-width: 2;
            -fx-border-radius: 20;
            -fx-effect: dropshadow(gaussian, rgba(242,127,159,0.45), 6, 0.2, 0, 1);
        }

        /* ---------- messages ---------- */
        .status-ok { -fx-text-fill: #3fa774; -fx-font-weight: bold; }
        .status-warn { -fx-text-fill: #d98a00; -fx-font-weight: bold; }

        .log {
            -fx-background-color: #fff6dc;
            -fx-border-color: #f4e2a8;
            -fx-background-radius: 12;
            -fx-border-radius: 12;
            -fx-padding: 10 14;
            -fx-text-fill: #6b5a1e;
            -fx-font-size: 13px;
        }

        /* ---------- visualization ---------- */
        .scroll-pane { -fx-background-color: transparent; }
        .scroll-pane > .viewport { -fx-background-color: transparent; }
        .scroll-pane > .corner { -fx-background-color: transparent; }
        .viz-scroll {
            -fx-background-color: #fffdf9;
            -fx-background-radius: 14;
            -fx-border-color: #e6d9cb;
            -fx-border-style: dashed;
            -fx-border-radius: 14;
        }
        .scroll-bar { -fx-background-color: transparent; }
        .scroll-bar .track { -fx-background-color: transparent; }
        .scroll-bar .thumb { -fx-background-color: #e5d8ca; -fx-background-radius: 6; }
        .scroll-bar .increment-button, .scroll-bar .decrement-button { -fx-padding: 0; -fx-background-color: transparent; }
        .scroll-bar .increment-arrow, .scroll-bar .decrement-arrow { -fx-padding: 0; -fx-shape: " "; }

        /* ---------- dialogs ---------- */
        .dialog-pane { -fx-background-color: #fffaf4; }
        .dialog-pane > .header-panel { -fx-background-color: #fdeff0; }
        .dialog-pane .label { -fx-text-fill: #4a4458; }
        .dialog-pane > .button-bar > .container > .button {
            -fx-background-color: #ff9eb5;
            -fx-text-fill: white;
            -fx-background-radius: 20;
        }
        .tooltip { -fx-background-color: #4a4458; -fx-text-fill: white; -fx-background-radius: 8; }
        """;
}