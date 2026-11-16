import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

public class Main extends Application {

    private static final String DEFAULT_NUMBERS = "8, 3, 6, 1, 9, 2, 7, 4";
    private static final String DEFAULT_THRESHOLD = "2";
    private static final String DEFAULT_DELAY = "20";
    private static final int MAX_SHOWN = 40;
    private static final int CORES = Runtime.getRuntime().availableProcessors();

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
            {"Int-ийн хязгаар", "2147483647, 0, -2147483648, -1, 1", "1"},
            {"Нэг тоо", "42", "2"}
    };

    private TextField numbersField;
    private TextField thresholdField;
    private TextField delayField;
    private ComboBox<String> workersBox;
    private ToggleButton mergeToggle;
    private ToggleButton quickToggle;
    private Button sortButton;
    private ProgressIndicator progress;
    private VBox inputCard;

    private Label originalLabel;
    private Label sortedLabel;
    private Label statsLabel;
    private Label statusLabel;
    private TreeCanvas treeCanvas;
    private ThreadTimeline timeline;

    private boolean running = false;

    /** Background thread-ээс UI руу буцаах үр дүн. */
    private record RunOutput(int[] original, int[] sorted, boolean mergeSort,
                             int threshold, long delayMs,
                             SortRunner.Result result, SortTrace trace) {
    }

    @Override
    public void start(Stage stage) {

        Label title = new Label("Fork-Join Sorting");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label(
                "Массивыг fork-оор хувааж, worker thread-үүд зэрэг "
                + "эрэмбэлээд, join-оор буцааж нэгтгэхийг харуулна");
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

        workersBox = new ComboBox<>();
        workersBox.getItems().addAll(
                "Auto (" + CORES + ")", "1", "2", "4", "8");
        workersBox.getSelectionModel().select(0);
        workersBox.setTooltip(new Tooltip(
                "ForkJoinPool-ийн parallelism буюу worker thread-ийн тоо. "
                + "1 сонговол бүх task нэг thread дээр ажиллана."));

        delayField = new TextField(DEFAULT_DELAY);
        delayField.setPrefWidth(70);
        delayField.setMaxWidth(70);
        delayField.setTooltip(new Tooltip(
                "Leaf task бүрт зориуд нэмэх саатал (ms). Жижиг өгөгдөл дээр "
                + "worker-ууд зэрэг ажиллахыг timeline дээр тод харуулна. "
                + "0 = бодит хугацаа. " + SortTrace.MAX_SIZE
                + "-аас олон тоонд автоматаар 0 болно."));
        Label delayUnit = new Label("ms / leaf");
        delayUnit.getStyleClass().add("muted");

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

        sortButton = new Button("Эрэмбэлэх");
        sortButton.getStyleClass().add("btn-primary");
        sortButton.setMaxWidth(Double.MAX_VALUE);
        sortButton.setDefaultButton(true);
        HBox.setHgrow(sortButton, Priority.ALWAYS);

        progress = new ProgressIndicator();
        progress.setPrefSize(22, 22);
        progress.setVisible(false);
        progress.setManaged(false);

        HBox buttonRow = new HBox(8, resetButton, sortButton, progress);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        inputCard = new VBox(
                8,
                keyLabel("Тоонууд (таслалаар тусгаарлана)"),
                numbersField,
                spacer(2),
                row(rowKey("Threshold"), thresholdField),
                row(rowKey("Алгоритм"), mergeToggle, quickToggle),
                row(rowKey("Worker thread"), workersBox),
                row(rowKey("Leaf саатал"), delayField, delayUnit),
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
        timeline = new ThreadTimeline();
        treeCanvas.setOnTaskChanged(timeline::highlight);

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

        SplitPane right = new SplitPane(treeCanvas, timeline);
        right.setOrientation(Orientation.VERTICAL);
        right.setDividerPositions(0.56);
        right.getStyleClass().add("right-split");

        HBox body = new HBox(14, leftScroll, right);
        HBox.setHgrow(right, Priority.ALWAYS);

        Label footer = new Label(
                "F.CSM302 Үйлдлийн систем · Бие даалт 2 · II-4-7 · "
                + "Fork-Join Sorting");
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
            delayField.setText(DEFAULT_DELAY);
            workersBox.getSelectionModel().select(0);
            mergeToggle.setSelected(true);
            samplesBox.getSelectionModel().clearSelection();
            originalLabel.setText("—");
            sortedLabel.setText("—");
            statsLabel.setText("");
            setStatus("Бэлэн.", 0);
            treeCanvas.showMessage(
                    "Тоогоо оруулаад «Эрэмбэлэх» дарна уу. "
                    + "Мод энд гарна.");
            timeline.clear("«Эрэмбэлэх» дарахад worker thread бүр аль "
                    + "task-ийг хэзээ гүйцэтгэснийг энд харуулна.");
        });

        generateButton.setOnAction(event -> {
            try {
                int size = InputParser.parseRandomSize(sizeField.getText());
                Random random = new Random();
                StringBuilder values = new StringBuilder();
                for (int i = 0; i < size; i++) {
                    if (i > 0) {
                        values.append(", ");
                    }
                    values.append(random.nextInt(2001) - 1000);
                }
                numbersField.setText(values.toString());
                setStatus(size + " санамсаргүй тоо үүсгэлээ.", 0);
            } catch (IllegalArgumentException e) {
                showError(e.getMessage());
            }
        });

        originalLabel.setText("—");
        sortedLabel.setText("—");

        Scene scene = new Scene(root, 1360, 900);
        Theme.apply(scene);

        stage.setTitle("Fork-Join Sorting — II-4-7");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
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

    private int selectedWorkers() {
        int idx = workersBox.getSelectionModel().getSelectedIndex();
        return idx <= 0 ? 0 : Integer.parseInt(workersBox.getValue());
    }

    static boolean sameTree(SortNode real, SortTrace.TNode traced) {
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

    /** Ижил бүтэцтэй хоёр модны зангилааг хооронд нь холбоно. */
    private static void link(SortTrace.TNode t, SortNode r,
                             Map<SortTrace.TNode, SortNode> out) {
        out.put(t, r);
        for (int i = 0; i < t.children.size(); i++) {
            link(t.children.get(i), r.getChildren().get(i), out);
        }
    }

    /**
     * Оролтыг UI thread дээр шалгаад, эрэмбэлэлтийг background thread дээр
     * ажиллуулна (BD2, шүүмж A1). Ажиллаж байх хооронд товчлуурууд
     * идэвхгүй болж давхар эхлүүлэхээс хамгаална.
     */
    private void sortNumbers() {
        if (running) {
            return;
        }

        final Integer[] numbers;
        final int threshold;
        final int delayInput;
        try {
            numbers = InputParser.parseNumbers(numbersField.getText());
            threshold = InputParser.parseThreshold(thresholdField.getText());
            delayInput = InputParser.parseLeafDelay(delayField.getText());
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
            return;
        }

        final boolean mergeSort = mergeToggle.isSelected();
        final int workers = selectedWorkers();
        final boolean visualize = numbers.length <= SortTrace.MAX_SIZE;
        // Том өгөгдөл дээр зориудын саатал нэмбэл хэт удаан болох тул 0.
        final long delayMs = visualize ? delayInput : 0;

        final int[] original = new int[numbers.length];
        for (int i = 0; i < numbers.length; i++) {
            original[i] = numbers[i];
        }

        Task<RunOutput> job = new Task<>() {
            @Override
            protected RunOutput call() {
                SortRunner.Result result = SortRunner.run(
                        numbers, threshold, mergeSort, workers, delayMs);

                int[] sorted = new int[numbers.length];
                for (int i = 0; i < numbers.length; i++) {
                    sorted[i] = numbers[i];
                }

                SortTrace trace = visualize
                        ? SortTrace.build(original, threshold, mergeSort)
                        : null;
                return new RunOutput(original, sorted, mergeSort, threshold,
                        delayMs, result, trace);
            }
        };

        job.setOnSucceeded(e -> {
            setRunning(false);
            show(job.getValue());
        });
        job.setOnFailed(e -> {
            setRunning(false);
            Throwable ex = job.getException();
            if (ex instanceof StackOverflowError) {
                showError("Рекурсийн гүн хэт их болсон тул эрэмбэлж "
                        + "чадсангүй. Threshold-ийг ихэсгэнэ үү.");
            } else if (ex instanceof OutOfMemoryError) {
                showError("Санах ой хүрэлцсэнгүй. Өгөгдлийг багасгана уу.");
            } else {
                showError("Эрэмбэлэх үед алдаа гарлаа: " + ex);
            }
            setStatus("Алдаа гарлаа.", 2);
        });

        setRunning(true);
        setStatus("Эрэмбэлж байна… (background thread дээр, UI хариу "
                + "өгсөөр байна)", 0);

        Thread runner = new Thread(job, "sort-runner");
        runner.setDaemon(true);
        runner.start();
    }

    private void setRunning(boolean on) {
        running = on;
        inputCard.setDisable(on);
        sortButton.setText(on ? "Эрэмбэлж байна…" : "Эрэмбэлэх");
        progress.setVisible(on);
        progress.setManaged(on);
        // inputCard идэвхгүй үед progress харагдсаар байлгах
        progress.setDisable(false);
    }

    private void show(RunOutput out) {
        SortRunner.Result r = out.result();
        String algoName = out.mergeSort() ? "Merge Sort" : "Quick Sort";

        originalLabel.setText(format(out.original()));
        sortedLabel.setText(format(out.sorted()));

        statsLabel.setText(String.format(
                "%s · %.3f ms%s · %d тоо · threshold %d · "
                + "%d task · гүн %d · parallelism %d · ашигласан worker %d · "
                + "steal %d",
                algoName,
                r.elapsedMs(),
                out.delayMs() > 0
                        ? " (leaf тус бүр +" + out.delayMs() + " ms саатал)"
                        : "",
                out.sorted().length,
                out.threshold(),
                r.nodes(),
                r.depth(),
                r.parallelism(),
                r.workersUsed(),
                r.steals()
        ));

        timeline.load(r);

        if (out.trace() == null) {
            setStatus("Амжилттай эрэмбэллээ. Бодит thread-үүдийн "
                    + "гүйцэтгэлийг доорх timeline-аас харна уу.", 1);
            treeCanvas.showMessage(
                    "Модны алхамчилсан дүрслэл зөвхөн " + SortTrace.MAX_SIZE
                    + " хүртэл тоотой үед ажиллана (одоо "
                    + out.original().length + " тоо байна). Бодит "
                    + "worker thread-үүдийн гүйцэтгэл доорх timeline дээр "
                    + "харагдана.");
            return;
        }

        SortTrace trace = out.trace();
        boolean sameResult = Arrays.equals(trace.sortedValues(), out.sorted());
        boolean sameShape = sameTree(r.root(), trace.root);

        Map<SortTrace.TNode, SortNode> map = new IdentityHashMap<>();
        if (sameShape) {
            link(trace.root, r.root(), map);
        }

        if (sameResult && sameShape) {
            setStatus("Амжилттай эрэмбэллээ. Эцсийн үр дүн ба модны бүтэц "
                    + "бодит fork-join task-уудтай таарч байна. Модны "
                    + "алхамчилсан анимаци нь дарааллыг тайлбарлах "
                    + "зориулалттай; бодит thread, хугацааг доорх timeline "
                    + "болон зангилааны T·W тэмдэг харуулна.", 1);
        } else {
            setStatus("Анхаар: тайлбарын мод бодит fork-join task-уудтай "
                    + "таараагүй байна.", 2);
        }

        treeCanvas.load(trace, map);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.getDialogPane().getStylesheets().add(Theme.url());
        alert.setTitle("Оролтын алдаа");
        alert.setHeaderText("Оролтыг шалгана уу");
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
