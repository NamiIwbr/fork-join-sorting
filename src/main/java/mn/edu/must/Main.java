package mn.edu.must;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;

public class Main extends Application {

    private static final String DEFAULT_NUMBERS = "8, 3, 6, 1, 9, 2, 7, 4";
    private static final String DEFAULT_THRESHOLD = "2";
    private static final int MAX_SHOWN = 40;

    private TextField numbersField;
    private TextField thresholdField;
    private ToggleButton mergeToggle;
    private ToggleButton quickToggle;

    private Label originalLabel;
    private Label sortedLabel;
    private Label statsLabel;
    private Label statusLabel;
    private TreeCanvas treeCanvas;

    @Override
    public void start(Stage stage) {

        // ------------------------------------------------------ header
        Label title = new Label("Fork-Join Sorting");
        title.getStyleClass().add("app-title");


        Region rainbow = new Region();
        rainbow.getStyleClass().add("rainbow");

        VBox header = new VBox(4, title, spacer(6), rainbow);

        // ------------------------------------------------------ inputs
        numbersField = new TextField(DEFAULT_NUMBERS);
        numbersField.setPromptText("8, 3, 6, 1, 9, 2, 7, 4");
        HBox.setHgrow(numbersField, Priority.ALWAYS);

        thresholdField = new TextField(DEFAULT_THRESHOLD);
        thresholdField.setPrefWidth(64);
        thresholdField.setMaxWidth(64);

        TextField sizeField = new TextField("16");
        sizeField.setPrefWidth(64);
        sizeField.setMaxWidth(64);

        ToggleGroup group = new ToggleGroup();
        mergeToggle = new ToggleButton("Merge Sort");
        quickToggle = new ToggleButton("Quick Sort");
        for (ToggleButton b : new ToggleButton[]{mergeToggle, quickToggle}) {
            b.setToggleGroup(group);
            b.getStyleClass().add("seg");
        }
        mergeToggle.setSelected(true);
        group.selectedToggleProperty().addListener((o, old, now) -> {
            if (now == null && old != null) {
                old.setSelected(true);   // one algorithm is always selected
            }
        });

        Button generateButton = new Button("Санамсаргүй");
        generateButton.getStyleClass().add("btn-ghost");

        Button resetButton = new Button("Reset");
        resetButton.getStyleClass().add("btn-ghost");

        Button sortButton = new Button("Эрэмбэлэх");
        sortButton.getStyleClass().add("btn-primary");
        sortButton.setDefaultButton(true);

        HBox row1 = new HBox(
                10, keyLabel("Тоонууд"), numbersField
        );
        row1.setAlignment(Pos.CENTER_LEFT);

        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);

        HBox row2 = new HBox(
                10,
                keyLabel("Threshold"), thresholdField,
                spacer(8, true),
                keyLabel("Алгоритм"), mergeToggle, quickToggle,
                grow,
                keyLabel("Хэмжээ"), sizeField, generateButton,
                spacer(8, true),
                resetButton, sortButton
        );
        row2.setAlignment(Pos.CENTER_LEFT);

        VBox inputCard = new VBox(12, row1, row2);
        inputCard.getStyleClass().add("card");

        // ------------------------------------------------------ result
        originalLabel = resultLine();
        sortedLabel = resultLine();
        statsLabel = new Label("");
        statsLabel.getStyleClass().add("stats");
        statsLabel.setWrapText(true);
        statusLabel = new Label("Бэлэн.");
        statusLabel.getStyleClass().add("muted");

        VBox resultCard = new VBox(
                6,
                lineRow("Эхний", originalLabel),
                lineRow("Дараа", sortedLabel),
                statsLabel,
                statusLabel
        );
        resultCard.getStyleClass().add("card");

        // ------------------------------------------------------- tree
        treeCanvas = new TreeCanvas();
        VBox.setVgrow(treeCanvas, Priority.ALWAYS);

        Label footer = new Label(
                "F.CSM302 Үйлдлийн систем · Бие даалт 1 · Fork-Join Sorting");
        footer.getStyleClass().add("footer");

        VBox root = new VBox(
                14, header, inputCard, resultCard, treeCanvas, footer
        );
        root.setPadding(new Insets(22, 28, 14, 28));
        root.setAlignment(Pos.TOP_CENTER);
        footer.setAlignment(Pos.CENTER);

        // ---------------------------------------------------- actions
        sortButton.setOnAction(event -> sortNumbers());

        resetButton.setOnAction(event -> {
            numbersField.setText(DEFAULT_NUMBERS);
            thresholdField.setText(DEFAULT_THRESHOLD);
            mergeToggle.setSelected(true);
            originalLabel.setText("—");
            sortedLabel.setText("—");
            statsLabel.setText("");
            setStatus("Бэлэн.", 0);
            treeCanvas.showMessage(
                    "Тоогоо оруулаад «Эрэмбэлэх» товчийг дарна уу — "
                    + "recursion tree энд гарна.");
        });

        generateButton.setOnAction(event -> {
            try {
                int size = Integer.parseInt(sizeField.getText().trim());

                if (size < 1 || size > 10000) {
                    throw new IllegalArgumentException(
                            "Өгөгдлийн хэмжээ 1–10000 байх ёстой."
                    );
                }

                java.util.Random random = new java.util.Random();
                StringBuilder values = new StringBuilder();

                for (int i = 0; i < size; i++) {
                    if (i > 0) {
                        values.append(", ");
                    }

                    values.append(random.nextInt(2001) - 1000);
                }

                numbersField.setText(values.toString());
                setStatus(size + " санамсаргүй тоо үүсгэлээ.", 0);

            } catch (NumberFormatException e) {
                showError("Өгөгдлийн хэмжээг бүхэл тоогоор оруулна уу.");
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            }
        });

        originalLabel.setText("—");
        sortedLabel.setText("—");

        Scene scene = new Scene(root, 1120, 880);
        Theme.apply(scene);

        stage.setTitle("Fork-Join Sorting");
        stage.setMinWidth(1020);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();
    }

    // ------------------------------------------------------ UI helpers

    private Label keyLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("key");
        return l;
    }

    private Label resultLine() {
        Label l = new Label("—");
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(l, Priority.ALWAYS);
        return l;
    }

    private HBox lineRow(String key, Label value) {
        Label k = keyLabel(key);
        k.setMinWidth(54);
        HBox row = new HBox(10, k, value);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    private Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        r.setPrefHeight(h);
        return r;
    }

    private Region spacer(double w, boolean horizontal) {
        Region r = new Region();
        r.setMinWidth(w);
        r.setPrefWidth(w);
        return r;
    }

    /** kind: 0 = normal, 1 = ok, 2 = warning */
    private void setStatus(String text, int kind) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll(
                "muted", "status-ok", "status-warn");
        statusLabel.getStyleClass().add(
                kind == 1 ? "status-ok" : kind == 2 ? "status-warn" : "muted");
    }

    /** Short text for an array: shows at most MAX_SHOWN values. */
    private String format(int[] values) {
        if (values.length <= MAX_SHOWN) {
            return Arrays.toString(values);
        }
        String head = Arrays.toString(Arrays.copyOf(values, MAX_SHOWN));
        return head.substring(0, head.length() - 1)
                + ", … +" + (values.length - MAX_SHOWN) + " тоо]";
    }

    private boolean isMergeSort() {
        return mergeToggle.isSelected();
    }

    // --------------------------------------------------------- sorting

    private void sortNumbers() {
        try {
            String input = numbersField.getText().trim();

            if (input.isEmpty()) {
                throw new IllegalArgumentException(
                        "Тоонуудын жагсаалт хоосон байна."
                );
            }

            int threshold = Integer.parseInt(
                    thresholdField.getText().trim()
            );

            if (threshold < 1) {
                throw new IllegalArgumentException(
                        "Threshold нь 1 буюу түүнээс их байх ёстой."
                );
            }

            String[] parts = input.split(",", -1);
            int[] numbers = new int[parts.length];

            for (int i = 0; i < parts.length; i++) {
                if (parts[i].trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Тоонуудын хооронд илүү таслал байна."
                    );
                }

                numbers[i] = Integer.parseInt(parts[i].trim());
            }

            int[] original = numbers.clone();
            boolean mergeSort = isMergeSort();
            String algoName = mergeSort ? "Merge Sort" : "Quick Sort";

            // ---- real fork-join run (used for the result and timing) ----
            long startTime = System.nanoTime();

            ForkJoinPool pool = new ForkJoinPool();
            SortNode rootNode;

            try {
                if (mergeSort) {
                    MergeSortTask task = new MergeSortTask(
                            numbers,
                            0,
                            numbers.length - 1,
                            threshold
                    );

                    pool.invoke(task);
                    rootNode = task.getRootNode();

                } else {
                    QuickSortTask task = new QuickSortTask(
                            numbers,
                            0,
                            numbers.length - 1,
                            threshold
                    );

                    pool.invoke(task);
                    rootNode = task.getRootNode();
                }
            } finally {
                pool.shutdown();
            }

            double elapsedMs =
                    (System.nanoTime() - startTime) / 1_000_000.0;

            originalLabel.setText(format(original));
            sortedLabel.setText(format(numbers));

            statsLabel.setText(String.format(
                    "%s · %.3f ms · %d тоо · threshold %d · "
                    + "%d зангилаа · гүн %d",
                    algoName,
                    elapsedMs,
                    numbers.length,
                    threshold,
                    countNodes(rootNode),
                    calculateDepth(rootNode)
            ));

            // ---- step-by-step animated replay of the same algorithm ----
            if (original.length <= SortTrace.MAX_SIZE) {
                SortTrace trace = SortTrace.build(
                        original, threshold, mergeSort
                );

                if (Arrays.equals(trace.sortedValues(), numbers)) {
                    setStatus("✔ Эрэмбэлэлт амжилттай.", 1);
                } else {
                    setStatus("⚠ Дүрслэлийн үр дүн fork-join-той зөрүүтэй "
                            + "байна.", 2);
                }

                treeCanvas.load(trace);

            } else {
                setStatus("✔ Эрэмбэлэлт амжилттай.", 1);
                treeCanvas.showMessage(
                        "Дүрслэл зөвхөн " + SortTrace.MAX_SIZE
                        + " хүртэл тоотой үед ажиллана (одоо "
                        + original.length + " тоо байна). "
                        + "Эрэмбэлэлт хийгдсэн, үр дүнг дээрээс харна уу."
                );
            }

        } catch (NumberFormatException e) {
            showError(
                    "Тоонууд болон threshold-д бүхэл тоо оруулна уу."
            );

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.getDialogPane().getStylesheets().add(Theme.url());
        alert.setTitle("Оролтын алдаа");
        alert.setHeaderText("Оролтыг шалгана уу");
        alert.setContentText(message);
        alert.showAndWait();
    }

    private int countNodes(SortNode node) {
        int count = 1;

        for (SortNode child : node.getChildren()) {
            count += countNodes(child);
        }

        return count;
    }

    private int calculateDepth(SortNode node) {
        int maxChildDepth = 0;

        for (SortNode child : node.getChildren()) {
            maxChildDepth = Math.max(
                    maxChildDepth,
                    calculateDepth(child)
            );
        }

        return 1 + maxChildDepth;
    }

    public static void main(String[] args) {
        launch(args);
    }
}