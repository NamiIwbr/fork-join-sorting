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

    private static final String[][] SAMPLES = {
            {"Номын жишээ",
                    "7, 12, 19, 3, 18, 4, 2, 6, 15, 8", "2"},
            {"Threshold = 1",
                    "7, 12, 19, 3, 18, 4, 2, 6, 15, 8", "1"},
            {"Эрэмбэлэгдсэн",
                    "1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16",
                    "2"},
            {"Урвуу эрэмбэтэй",
                    "16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1",
                    "2"},
            {"Бүгд адил", "5, 5, 5, 5, 5, 5, 5, 5", "2"},
            {"Давтагдсан тоотой", "4, 2, 4, 1, 2, 4, 1, 3, 3, 2", "2"},
            {"Сөрөг, 0 тоотой", "-5, 3, 0, -9, 8, -1, 0, 7", "2"},
            {"Нэг тоо", "42", "2"}
    };

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

        Label title = new Label("Fork-Join Sorting");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label(
                "Тоонуудыг хоёр хэсэг болгон хуваагаад, эрэмбэлээд, "
                + "буцааж нэгтгэхийг мод хэлбэрээр харуулна");
        subtitle.getStyleClass().add("app-subtitle");

        VBox header = new VBox(2, title, subtitle);

        numbersField = new TextField(DEFAULT_NUMBERS);
        numbersField.setPromptText("8, 3, 6, 1, 9, 2, 7, 4");

        thresholdField = new TextField(DEFAULT_THRESHOLD);
        thresholdField.setPrefWidth(70);
        thresholdField.setMaxWidth(70);

        TextField sizeField = new TextField("16");
        sizeField.setPrefWidth(70);
        sizeField.setMaxWidth(70);

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
                old.setSelected(true);
            }
        });

        ComboBox<String> samplesBox = new ComboBox<>();
        samplesBox.setPromptText("Жишээ сонгох");
        samplesBox.setMaxWidth(Double.MAX_VALUE);
        for (String[] sample : SAMPLES) {
            samplesBox.getItems().add(sample[0]);
        }

        Button generateButton = new Button("Үүсгэх");

        Button resetButton = new Button("Reset");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(resetButton, Priority.ALWAYS);

        Button sortButton = new Button("Эрэмбэлэх");
        sortButton.getStyleClass().add("btn-primary");
        sortButton.setMaxWidth(Double.MAX_VALUE);
        sortButton.setDefaultButton(true);
        HBox.setHgrow(sortButton, Priority.ALWAYS);

        HBox buttonRow = new HBox(8, resetButton, sortButton);

        VBox inputCard = new VBox(
                8,
                keyLabel("Тоонууд (таслалаар тусгаарлана)"),
                numbersField,
                spacer(2),
                row(rowKey("Threshold"), thresholdField),
                row(rowKey("Алгоритм"), mergeToggle, quickToggle),
                row(rowKey("Санамсаргүй тоо"), sizeField, generateButton),
                row(rowKey("Жишээ"), samplesBox),
                spacer(2),
                buttonRow
        );
        inputCard.getStyleClass().add("card");
        HBox.setHgrow(samplesBox, Priority.ALWAYS);

        originalLabel = resultLine();
        sortedLabel = resultLine();
        statsLabel = new Label("");
        statsLabel.getStyleClass().add("stats");
        statsLabel.setWrapText(true);
        statusLabel = new Label("Бэлэн.");
        statusLabel.getStyleClass().add("muted");
        statusLabel.setWrapText(true);

        VBox resultCard = new VBox(
                4,
                keyLabel("Өгөгдөл"), originalLabel,
                spacer(2),
                keyLabel("Үр дүн"), sortedLabel,
                spacer(2),
                statsLabel,
                statusLabel
        );
        resultCard.getStyleClass().add("card");

        treeCanvas = new TreeCanvas();

        VBox left = new VBox(12, inputCard, resultCard,
                treeCanvas.getSidePanel());
        left.setPrefWidth(350);
        left.setMinWidth(350);
        left.setMaxWidth(350);

        ScrollPane leftScroll = new ScrollPane(left);
        leftScroll.setFitToWidth(true);
        leftScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        leftScroll.setPrefWidth(372);
        leftScroll.setMinWidth(372);

        HBox body = new HBox(14, leftScroll, treeCanvas);
        HBox.setHgrow(treeCanvas, Priority.ALWAYS);

        Label footer = new Label(
                "F.CSM302 Үйлдлийн систем · Бие даалт 1 · Fork-Join Sorting");
        footer.getStyleClass().add("footer");

        VBox root = new VBox(12, header, body, footer);
        VBox.setVgrow(body, Priority.ALWAYS);
        root.setPadding(new Insets(16, 20, 10, 20));

        sortButton.setOnAction(event -> sortNumbers());

        samplesBox.setOnAction(event -> {
            int idx = samplesBox.getSelectionModel().getSelectedIndex();
            if (idx >= 0) {
                numbersField.setText(SAMPLES[idx][1]);
                thresholdField.setText(SAMPLES[idx][2]);
                setStatus("Жишээ ачааллаа: " + SAMPLES[idx][0]
                        + ". «Эрэмбэлэх» дарна уу.", 0);
            }
        });

        resetButton.setOnAction(event -> {
            numbersField.setText(DEFAULT_NUMBERS);
            thresholdField.setText(DEFAULT_THRESHOLD);
            mergeToggle.setSelected(true);
            samplesBox.getSelectionModel().clearSelection();
            originalLabel.setText("—");
            sortedLabel.setText("—");
            statsLabel.setText("");
            setStatus("Бэлэн.", 0);
            treeCanvas.showMessage(
                    "Тоогоо оруулаад «Эрэмбэлэх» дарна уу. "
                    + "Мод энд гарна.");
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

        Scene scene = new Scene(root, 1280, 860);
        Theme.apply(scene);

        stage.setTitle("Fork-Join Sorting");
        stage.setMinWidth(1050);
        stage.setMinHeight(700);
        stage.setScene(scene);
        stage.show();
    }


    private Label keyLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("key");
        return l;
    }

    private Label rowKey(String text) {
        Label l = keyLabel(text);
        l.setMinWidth(112);
        return l;
    }

    private HBox row(javafx.scene.Node... nodes) {
        HBox h = new HBox(8, nodes);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private Label resultLine() {
        Label l = new Label("—");
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private Region spacer(double h) {
        Region r = new Region();
        r.setMinHeight(h);
        r.setPrefHeight(h);
        return r;
    }

    private void setStatus(String text, int kind) {
        statusLabel.setText(text);
        statusLabel.getStyleClass().removeAll(
                "muted", "status-ok", "status-warn");
        statusLabel.getStyleClass().add(
                kind == 1 ? "status-ok" : kind == 2 ? "status-warn" : "muted");
    }

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

    private boolean sameTree(SortNode real, SortTrace.TNode traced) {
        if (real.getLeft() != traced.left
                || real.getRight() != traced.right
                || real.getChildren().size() != traced.children.size()) {
            return false;
        }
        for (int i = 0; i < traced.children.size(); i++) {
            if (!sameTree(real.getChildren().get(i), traced.children.get(i))) {
                return false;
            }
        }
        return true;
    }

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
            Integer[] numbers = new Integer[parts.length];

            for (int i = 0; i < parts.length; i++) {
                if (parts[i].trim().isEmpty()) {
                    throw new IllegalArgumentException(
                            "Тоонуудын хооронд илүү таслал байна."
                    );
                }

                numbers[i] = Integer.parseInt(parts[i].trim());
            }

            int[] original = new int[numbers.length];
            for (int i = 0; i < numbers.length; i++) {
                original[i] = numbers[i];
            }

            boolean mergeSort = isMergeSort();
            String algoName = mergeSort ? "Merge Sort" : "Quick Sort";

            ForkJoinPool pool = new ForkJoinPool();
            int parallelism = pool.getParallelism();
            SortNode rootNode;
            long startTime = System.nanoTime();

            try {
                if (mergeSort) {
                    MergeSortTask<Integer> task = new MergeSortTask<>(
                            numbers, 0, numbers.length - 1, threshold);
                    pool.invoke(task);
                    rootNode = task.getRootNode();

                } else {
                    QuickSortTask<Integer> task = new QuickSortTask<>(
                            numbers, 0, numbers.length - 1, threshold);
                    pool.invoke(task);
                    rootNode = task.getRootNode();
                }
            } finally {
                pool.shutdown();
            }

            double elapsedMs =
                    (System.nanoTime() - startTime) / 1_000_000.0;

            int[] sorted = new int[numbers.length];
            for (int i = 0; i < numbers.length; i++) {
                sorted[i] = numbers[i];
            }

            originalLabel.setText(format(original));
            sortedLabel.setText(format(sorted));

            statsLabel.setText(String.format(
                    "%s · %.3f ms · %d тоо · threshold %d · "
                    + "%d зангилаа · гүн %d · parallelism %d",
                    algoName,
                    elapsedMs,
                    sorted.length,
                    threshold,
                    countNodes(rootNode),
                    calculateDepth(rootNode),
                    parallelism
            ));

            if (original.length <= SortTrace.MAX_SIZE) {
                SortTrace trace = SortTrace.build(
                        original, threshold, mergeSort
                );

                boolean sameResult =
                        Arrays.equals(trace.sortedValues(), sorted);
                boolean sameShape = sameTree(rootNode, trace.root);

                if (sameResult && sameShape) {
                    setStatus("Амжилттай эрэмбэллээ. Мод нь бодит "
                            + "fork-join-той яг таарч байна.", 1);
                } else {
                    setStatus("Анхаар: мод бодит fork-join-той таараагүй "
                            + "байна.", 2);
                }

                treeCanvas.load(trace);

            } else {
                setStatus("Амжилттай эрэмбэллээ.", 1);
                treeCanvas.showMessage(
                        "Дүрслэл зөвхөн " + SortTrace.MAX_SIZE
                        + " хүртэл тоотой үед ажиллана (одоо "
                        + original.length + " тоо байна). "
                        + "Эрэмбэлэлт хийгдсэн, үр дүнг дээрээс харна уу."
                );
            }

        } catch (NumberFormatException e) {
            showError(
                    "Тоонууд болон threshold-д бүхэл тоо оруулна уу "
                    + "(−2147483648 … 2147483647)."
            );

        } catch (IllegalArgumentException e) {
            showError(e.getMessage());

        } catch (StackOverflowError e) {
            showError("Рекурсийн гүн хэт их болсон тул эрэмбэлж чадсангүй. "
                    + "Threshold-ийг ихэсгэх эсвэл өгөгдлийг багасгана уу.");
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