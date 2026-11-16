import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TreeCanvas extends VBox {

    private static final double CELL_W = 42;
    private static final double CELL_H = 34;
    private static final double PAD = 10;
    private static final double HEADER_H = 44;
    private static final double BOX_H = HEADER_H + CELL_H + PAD;
    private static final double MIN_BOX_W = 110;
    private static final double GAP_X = 20;
    private static final double GAP_Y = 60;
    private static final double MARGIN = 24;

    private static final String BOX_IDLE =
            "-fx-background-color: white; -fx-border-color: #c9ccd2;"
            + "-fx-border-width: 1; -fx-border-radius: 8;"
            + "-fx-background-radius: 8;";
    private static final String BOX_ACTIVE =
            "-fx-background-color: #eef4ff; -fx-border-color: #3b6fd4;"
            + "-fx-border-width: 2; -fx-border-radius: 8;"
            + "-fx-background-radius: 8;";
    private static final String BOX_DONE =
            "-fx-background-color: #f1f8f2; -fx-border-color: #7fbf8a;"
            + "-fx-border-width: 1.5; -fx-border-radius: 8;"
            + "-fx-background-radius: 8;";

    // Ирмэгийн (edge) төлөв: эцэг → хүүхэд
    private static final int EDGE_COMPUTE = 1;  // эцэг thread өөрөө compute()
    private static final int EDGE_FORK = 2;     // fork() хийгдсэн
    private static final int EDGE_JOINED = 3;   // join()-оор буцаж ирсэн
    private static final int EDGE_RETURNED = 4; // inline compute дуусч буцсан

    private static class View {
        final SortTrace.TNode node;
        final List<View> kids = new ArrayList<>();
        final Pane box = new Pane();
        final Pane cellsPane = new Pane();
        final Label title = new Label();
        final Label status = new Label();
        final Label badge = new Label();
        final Map<Integer, Label> cells = new HashMap<>();
        Line lineToParent;
        Label edgeLabel;
        int edgeState;

        double w;
        double x;
        double y;
        double subW;
        double kidsW;

        boolean revealed;
        int[] order;
        int[] hl = new int[0];
        int pivot = -1;
        boolean done;
        boolean active;
        String statusText = "";

        View(SortTrace.TNode node) {
            this.node = node;
        }
    }

    private SortTrace trace;
    private Map<SortTrace.TNode, SortNode> real = new HashMap<>();
    private Consumer<SortNode> onTaskChanged;
    private View rootView;
    private View current;     
    private final Map<SortTrace.TNode, View> views = new HashMap<>();
    private final List<TranslateTransition> anims = new ArrayList<>();
    private final List<ScaleTransition> pulses = new ArrayList<>();

    private int pos = 0;
    private boolean playing = false;
    private boolean updatingSlider = false;
    private double delayMs = 800;
    private Timeline timeline;

    private final Button restartBtn = new Button("Эхнээс");
    private final Button backBtn = new Button("Өмнөх");
    private final Button playBtn = new Button("Тоглуулах");
    private final Button fwdBtn = new Button("Дараах");
    private final Slider speedSlider = new Slider(1, 10, 3);
    private final Slider scrubber = new Slider(0, 1, 0);
    private final Label stepLabel = new Label("Алхам 0 / 0");
    private final Label phaseChip = new Label("");
    private final Label logLabel = new Label("");
    private final Label sideTask = new Label("");

    private final Label sideRange = new Label("—");
    private final Label sideStatus = new Label("");
    private final FlowPane sideCells = new FlowPane(6, 6);

    private final Pane pane = new Pane();
    private final Group lines = new Group();
    private final Group boxes = new Group();
    private final ScrollPane scroll;
    private final VBox side;

    public TreeCanvas() {
        super(10);
        getStyleClass().add("card");
        setAlignment(Pos.TOP_CENTER);

        restartBtn.getStyleClass().add("btn-ctrl");
        backBtn.getStyleClass().add("btn-ctrl");
        fwdBtn.getStyleClass().add("btn-ctrl");
        playBtn.getStyleClass().add("btn-play");
        playBtn.setMinWidth(100);
        restartBtn.setTooltip(new Tooltip("Эхнээс нь эхлүүлнэ"));
        backBtn.setTooltip(new Tooltip("Нэг алхам хойш"));
        fwdBtn.setTooltip(new Tooltip("Нэг алхам урагш"));

        speedSlider.setPrefWidth(130);
        Label speedLabel = new Label("Хурд");
        speedLabel.getStyleClass().add("muted");
        HBox speedBox = new HBox(8, speedLabel, speedSlider);
        speedBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setMargin(speedBox, new Insets(0, 0, 0, 14));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        stepLabel.getStyleClass().add("muted");

        phaseChip.getStyleClass().add("phase-chip");
        phaseChip.setVisible(false);
        phaseChip.setTooltip(new Tooltip(
                "Одоогийн алхмын төрөл: FORK / COMPUTE / PARTITION / LEAF /"
                + " JOIN / MERGE / DONE"));

        HBox controls = new HBox(
                8, restartBtn, backBtn, playBtn, fwdBtn,
                speedBox, spacer, phaseChip, stepLabel
        );
        controls.setAlignment(Pos.CENTER_LEFT);

        scrubber.setMajorTickUnit(1);
        scrubber.setMinorTickCount(0);
        scrubber.setBlockIncrement(1);
        scrubber.setSnapToTicks(true);
        scrubber.setMaxWidth(Double.MAX_VALUE);

        Label sideTitle = new Label("Одоо эрэмбэлж байгаа хэсэг");
        sideTitle.getStyleClass().add("hint");
        sideRange.getStyleClass().add("side-range");
        sideStatus.getStyleClass().add("muted");
        sideCells.setMinHeight(48);

        logLabel.getStyleClass().add("log");
        logLabel.setWrapText(true);
        logLabel.setMaxWidth(Double.MAX_VALUE);
        logLabel.setMinHeight(54);

        sideTask.getStyleClass().add("side-task");
        sideTask.setWrapText(true);
        sideTask.setMaxWidth(Double.MAX_VALUE);
        sideTask.setVisible(false);
        sideTask.setManaged(false);

        side = new VBox(8, sideTitle, sideRange, sideStatus,
                sideCells, logLabel, sideTask);
        side.getStyleClass().add("card");
        sideCells.prefWrapLengthProperty()
                .bind(side.widthProperty().subtract(34));

        // ---- мод ----
        pane.getChildren().addAll(lines, boxes);
        StackPane holder = new StackPane(pane);
        holder.setAlignment(Pos.TOP_CENTER);

        scroll = new ScrollPane(holder);
        scroll.getStyleClass().add("viz-scroll");
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setMinHeight(320);
        scroll.setPrefHeight(520);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        FlowPane legend = new FlowPane(
                16, 4,
                legendItem("#e0b42f", "харьцуулж байгаа"),
                legendItem("#8f74d6", "pivot"),
                legendItem("#3b6fd4", "одоо ажиллаж байгаа"),
                legendItem("#7fbf8a", "дууссан"),
                edgeLegend("#8f74d6", true, "fork() — queue-д хийсэн"),
                edgeLegend("#3b6fd4", false, "compute() — энэ thread"),
                edgeLegend("#2e9e6b", false, "join() — буцаж ирсэн"),
                badgeLegend()
        );
        legend.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(controls, scrubber, scroll, legend);

        restartBtn.setOnAction(e -> {
            pause();
            goTo(0);
        });
        backBtn.setOnAction(e -> {
            pause();
            if (pos > 0) {
                goTo(pos - 1);
            }
        });
        fwdBtn.setOnAction(e -> {
            pause();
            stepForward();
        });
        playBtn.setOnAction(e -> {
            if (playing) {
                pause();
            } else {
                play(true);
            }
        });

        speedSlider.valueProperty().addListener((o, a, b) -> {
            delayMs = 1500.0 / b.doubleValue();
            if (playing) {
                startTimeline();
            }
        });

        scrubber.valueProperty().addListener((o, a, b) -> {
            if (updatingSlider || trace == null) {
                return;
            }
            int p = (int) Math.round(b.doubleValue());
            if (p != pos) {
                pause();
                goTo(p);
            }
        });

        showMessage("Тоогоо оруулаад «Эрэмбэлэх» дарна уу. Мод энд гарна.");
    }

    public VBox getSidePanel() {
        return side;
    }

    /** Одоогийн алхмын бодит task өөрчлөгдөхөд дуудагдана (timeline-тай холбох). */
    public void setOnTaskChanged(Consumer<SortNode> listener) {
        this.onTaskChanged = listener;
    }

    private HBox edgeLegend(String color, boolean dashed, String text) {
        Line l = new Line(0, 0, 22, 0);
        l.setStroke(Color.web(color));
        l.setStrokeWidth(2);
        if (dashed) {
            l.getStrokeDashArray().addAll(5.0, 4.0);
        }
        Label t = new Label(text);
        t.getStyleClass().add("muted");
        t.setStyle("-fx-font-size: 11px;");
        HBox h = new HBox(6, l, t);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private HBox badgeLegend() {
        Label b = new Label("T3·W2");
        b.setStyle(badgeStyle("W2"));
        Label t = new Label("task дугаар · гүйцэтгэсэн worker thread");
        t.getStyleClass().add("muted");
        t.setStyle("-fx-font-size: 11px;");
        HBox h = new HBox(6, b, t);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static String badgeStyle(String thread) {
        return "-fx-background-color: " + ThreadTimeline.workerColor(thread)
                + "; -fx-text-fill: white; -fx-font-size: 10px;"
                + " -fx-font-weight: bold; -fx-padding: 1 5;"
                + " -fx-background-radius: 8;";
    }

    private HBox legendItem(String color, String text) {
        Circle dot = new Circle(5, Color.web(color));
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        l.setStyle("-fx-font-size: 11px;");
        HBox h = new HBox(6, dot, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    public void clear() {
        pause();
        stopAnims();
        trace = null;
        real = new HashMap<>();
        rootView = null;
        current = null;
        views.clear();
        lines.getChildren().clear();
        boxes.getChildren().clear();
        pane.setPrefSize(0, 0);
        pane.setMinSize(0, 0);
        pane.setMaxSize(0, 0);
        pos = 0;
        stepLabel.setText("Алхам 0 / 0");
        logLabel.setText("");
        sideRange.setText("—");
        sideStatus.setText("");
        sideCells.getChildren().clear();
        sideTask.setText("");
        sideTask.setVisible(false);
        sideTask.setManaged(false);
        phaseChip.setVisible(false);
        setControlsDisabled(true);
    }

    public void showMessage(String message) {
        clear();
        Label l = new Label(message);
        l.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 14px;");
        l.setWrapText(true);
        l.setMaxWidth(560 - 2 * MARGIN);
        l.setLayoutX(MARGIN);
        l.setLayoutY(MARGIN);
        boxes.getChildren().add(l);
        pane.setPrefSize(560, 140);
        pane.setMinSize(560, 140);
        pane.setMaxSize(560, 140);
    }

    public void load(SortTrace t) {
        load(t, null);
    }

    /**
     * @param realTasks trace-ийн зангилаа бүрт харгалзах бодит SortNode
     *                  (thread, хугацаа). null бол badge харуулахгүй.
     */
    public void load(SortTrace t, Map<SortTrace.TNode, SortNode> realTasks) {
        clear();
        trace = t;
        real = realTasks == null ? new HashMap<>() : realTasks;

        rootView = create(t.root, null);
        double total = measure(rootView);
        place(rootView, MARGIN, 0);

        int depth = maxDepth(t.root);
        double width = total + 2 * MARGIN;
        double height = (depth + 1) * (BOX_H + GAP_Y) + MARGIN;
        pane.setPrefSize(width, height);
        pane.setMinSize(width, height);
        pane.setMaxSize(width, height);

        updatingSlider = true;
        scrubber.setMax(Math.max(1, t.steps.size()));
        updatingSlider = false;

        setControlsDisabled(false);
        goTo(0);
        play(false);  
    }


    private View create(SortTrace.TNode n, View parent) {
        View v = new View(n);
        views.put(n, v);

        v.w = Math.max(n.size() * CELL_W + 2 * PAD, MIN_BOX_W);
        v.box.setPrefSize(v.w, BOX_H);
        v.box.setMinSize(v.w, BOX_H);
        v.box.setMaxSize(v.w, BOX_H);

        v.title.setText(n.range());
        v.title.setStyle("-fx-text-fill: #2b2f36; -fx-font-weight: bold;"
                + " -fx-font-size: 13px;");
        v.title.setMinWidth(v.w);
        v.title.setAlignment(Pos.CENTER);
        v.title.setLayoutY(5);

        v.status.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 10px;");
        v.status.setMinWidth(v.w);
        v.status.setAlignment(Pos.CENTER);
        v.status.setLayoutY(23);

        v.cellsPane.setLayoutX((v.w - n.size() * CELL_W) / 2);
        v.cellsPane.setLayoutY(HEADER_H);

        for (int id : n.input) {
            Label c = new Label(String.valueOf(trace.values[id]));
            c.setMinSize(CELL_W - 4, CELL_H);
            c.setPrefSize(CELL_W - 4, CELL_H);
            c.setMaxSize(CELL_W - 4, CELL_H);
            c.setAlignment(Pos.CENTER);
            v.cells.put(id, c);
            v.cellsPane.getChildren().add(c);
        }

        v.box.getChildren().addAll(v.title, v.status, v.cellsPane);
        boxes.getChildren().add(v.box);

        SortNode task = real.get(n);
        if (task != null && task.getThread() != null) {
            v.badge.setText(task.getLabel() + "·" + task.getThread());
            v.badge.setStyle(badgeStyle(task.getThread()));
            v.badge.setLayoutX(0);
            v.badge.setLayoutY(BOX_H + 3);
            v.badge.setTooltip(new Tooltip(taskInfo(task)));
            v.box.getChildren().add(v.badge);
        }

        for (SortTrace.TNode c : n.children) {
            View kid = create(c, v);
            v.kids.add(kid);
        }

        if (parent != null) {
            v.lineToParent = new Line();
            v.lineToParent.setStroke(Color.web("#9aa0aa"));
            v.lineToParent.setStrokeWidth(1.5);
            v.lineToParent.setStrokeLineCap(StrokeLineCap.ROUND);
            lines.getChildren().add(v.lineToParent);

            v.edgeLabel = new Label();
            v.edgeLabel.setMouseTransparent(true);
            lines.getChildren().add(v.edgeLabel);
        }
        return v;
    }

    private double measure(View v) {
        double kidsW = 0;
        for (View c : v.kids) {
            kidsW += measure(c);
        }
        if (!v.kids.isEmpty()) {
            kidsW += GAP_X * (v.kids.size() - 1);
        }
        v.kidsW = kidsW;
        v.subW = Math.max(v.w, kidsW);
        return v.subW;
    }

    private void place(View v, double left, int depth) {
        double cx = left + v.subW / 2;
        v.x = cx - v.w / 2;
        v.y = MARGIN + depth * (BOX_H + GAP_Y);
        v.box.setLayoutX(v.x);
        v.box.setLayoutY(v.y);

        double cl = left + (v.subW - v.kidsW) / 2;
        for (View c : v.kids) {
            place(c, cl, depth + 1);
            cl += c.subW + GAP_X;

            c.lineToParent.setStartX(cx);
            c.lineToParent.setStartY(v.y + BOX_H);
            c.lineToParent.setEndX(c.x + c.w / 2);
            c.lineToParent.setEndY(c.y);

            double mx = (cx + c.x + c.w / 2) / 2;
            double my = (v.y + BOX_H + c.y) / 2;
            c.edgeLabel.setLayoutX(mx + (c.x + c.w / 2 < cx ? -62 : 6));
            c.edgeLabel.setLayoutY(my - 8);
        }
    }

    private int maxDepth(SortTrace.TNode n) {
        int d = 0;
        for (SortTrace.TNode c : n.children) {
            d = Math.max(d, 1 + maxDepth(c));
        }
        return d;
    }

    private void resetView(View v) {
        v.revealed = (v == rootView);
        v.order = v.node.input.clone();
        v.hl = new int[0];
        v.pivot = -1;
        v.done = false;
        v.active = false;
        v.statusText = v.revealed ? "Эхний өгөгдөл" : "";
        v.edgeState = 0;
        v.box.setOpacity(1);
        render(v, false);
    }

    private void goTo(int p) {
        stopAnims();
        current = null;
        for (View v : views.values()) {
            resetView(v);
        }
        for (int i = 0; i < p; i++) {
            apply(trace.steps.get(i), false);
        }
        pos = p;
        refreshInfo();
        revealActive();
    }

    private boolean stepForward() {
        if (trace == null || pos >= trace.steps.size()) {
            return false;
        }
        stopAnims();
        apply(trace.steps.get(pos), true);
        pos++;
        refreshInfo();
        revealActive();
        return true;
    }

    private void apply(SortTrace.Step s, boolean animate) {
        for (SortTrace.TNode r : s.reveal) {
            View c = views.get(r);
            c.revealed = true;
            c.edgeState = r.forked ? EDGE_FORK : EDGE_COMPUTE;
            c.statusText = r.forked ? "fork хийгдсэн · queue-д хүлээж байна"
                    : "compute · энэ thread дээр";
            render(c, false);
            if (animate) {
                c.box.setOpacity(0);
                FadeTransition ft = new FadeTransition(
                        Duration.millis(moveMs()), c.box);
                ft.setToValue(1);
                ft.play();
            }
        }

        if (s.node != null) {
            View v = views.get(s.node);
            if (s.order != null) {
                v.order = s.order.clone();
            }
            v.hl = s.hl;
            v.pivot = s.pivot;
            if (s.status != null) {
                v.statusText = s.status;
            }
            v.done = s.done;
            v.active = !s.done;
            current = v;

            if (SortTrace.JOIN.equals(s.phase)) {
                for (View k : v.kids) {
                    k.edgeState = EDGE_JOINED;
                    renderEdge(k);
                }
            } else if (SortTrace.DONE.equals(s.phase)) {
                for (View k : v.kids) {
                    if (k.edgeState != EDGE_JOINED) {
                        k.edgeState = EDGE_RETURNED;
                        renderEdge(k);
                    }
                }
            }
            render(v, animate);
        }
    }

    private void renderEdge(View v) {
        if (v.lineToParent == null) {
            return;
        }
        Line l = v.lineToParent;
        l.getStrokeDashArray().clear();
        String text;
        String color;
        switch (v.edgeState) {
            case EDGE_FORK -> {
                color = "#8f74d6";
                l.getStrokeDashArray().addAll(6.0, 5.0);
                text = "fork()";
            }
            case EDGE_COMPUTE -> {
                color = "#3b6fd4";
                text = "compute()";
            }
            case EDGE_JOINED -> {
                color = "#2e9e6b";
                text = v.node.forked ? "↑ join()" : "↑ буцсан";
            }
            case EDGE_RETURNED -> {
                color = "#2e9e6b";
                text = "↑ буцсан";
            }
            default -> {
                color = "#9aa0aa";
                text = "";
            }
        }
        l.setStroke(Color.web(color));
        l.setStrokeWidth(v.edgeState == 0 ? 1.5 : 2.2);
        v.edgeLabel.setText(text);
        v.edgeLabel.setStyle("-fx-text-fill: " + color
                + "; -fx-font-size: 11px; -fx-font-weight: bold;"
                + " -fx-background-color: rgba(255,255,255,0.85);"
                + " -fx-padding: 0 3; -fx-background-radius: 3;");
        v.edgeLabel.setVisible(v.revealed && !text.isEmpty());
    }

    private void render(View v, boolean animate) {
        v.box.setVisible(v.revealed);
        if (v.lineToParent != null) {
            v.lineToParent.setVisible(v.revealed);
            renderEdge(v);
        }

        v.status.setText(v.statusText);
        v.box.setStyle(v.done ? BOX_DONE : v.active ? BOX_ACTIVE : BOX_IDLE);

        for (int k = 0; k < v.order.length; k++) {
            int id = v.order[k];
            Label c = v.cells.get(id);
            double target = k * CELL_W;

            if (animate && Math.abs(c.getLayoutX() - target) > 0.5) {
                double from = c.getLayoutX() - target;
                c.setLayoutX(target);
                c.setTranslateX(from);

                TranslateTransition tt = new TranslateTransition(
                        Duration.millis(moveMs()), c);
                tt.setToX(0);
                anims.add(tt);
                tt.play();
                c.toFront();
            } else {
                c.setLayoutX(target);
                c.setTranslateX(0);
            }

            if (animate && contains(v.hl, id)) {
                ScaleTransition st = new ScaleTransition(
                        Duration.millis(moveMs() / 2), c);
                st.setFromX(1);
                st.setFromY(1);
                st.setToX(1.2);
                st.setToY(1.2);
                st.setAutoReverse(true);
                st.setCycleCount(2);
                pulses.add(st);
                st.play();
                c.toFront();
            }

            c.setStyle(cellStyle(v, id, 15));
        }
    }

    private String cellStyle(View v, int id, int fontSize) {
        String bg;
        String border;
        String text;

        if (v.done) {
            bg = "#cfe9d3";
            border = "#7fbf8a";
            text = "#1f4d2b";
        } else if (contains(v.hl, id)) {
            bg = "#ffe08a";
            border = "#e0b42f";
            text = "#4a3a00";
        } else if (id == v.pivot) {
            bg = "#b9a4f0";
            border = "#8f74d6";
            text = "white";
        } else {
            bg = "#f1f2f5";
            border = "#c9ccd2";
            text = "#2b2f36";
        }

        return "-fx-background-color: " + bg + "; -fx-border-color: " + border
                + "; -fx-border-radius: 5; -fx-background-radius: 5;"
                + " -fx-text-fill: " + text + ";"
                + " -fx-font-size: " + fontSize + "px; -fx-font-weight: bold;";
    }

    private static boolean contains(int[] arr, int id) {
        for (int x : arr) {
            if (x == id) {
                return true;
            }
        }
        return false;
    }

    private double moveMs() {
        return Math.min(500, delayMs * 0.7);
    }

    private void stopAnims() {
        for (TranslateTransition tt : anims) {
            tt.stop();
            tt.getNode().setTranslateX(0);
        }
        anims.clear();

        for (ScaleTransition st : pulses) {
            st.stop();
            st.getNode().setScaleX(1);
            st.getNode().setScaleY(1);
        }
        pulses.clear();
    }

    private void refreshSide() {
        sideCells.getChildren().clear();

        View v = current != null ? current : rootView;
        if (v == null) {
            sideRange.setText("—");
            sideStatus.setText("");
            return;
        }

        sideRange.setText(v.node.range());
        sideStatus.setText(v.statusText);

        SortNode task = real.get(v.node);
        if (task != null && task.getThread() != null) {
            sideTask.setText(taskInfo(task));
            sideTask.setVisible(true);
            sideTask.setManaged(true);
        }
        if (onTaskChanged != null) {
            onTaskChanged.accept(task);
        }

        for (int id : v.order) {
            Label c = new Label(String.valueOf(trace.values[id]));
            c.setMinSize(46, 40);
            c.setPrefSize(46, 40);
            c.setMaxSize(46, 40);
            c.setAlignment(Pos.CENTER);
            c.setStyle(cellStyle(v, id, 18));
            sideCells.getChildren().add(c);
        }
    }

    
    private void revealActive() {
        Platform.runLater(() -> {
            View v = current != null ? current : rootView;
            if (v == null) {
                return;
            }

            double vw = scroll.getViewportBounds().getWidth();
            double vh = scroll.getViewportBounds().getHeight();
            if (vw <= 0 || vh <= 0) {
                return;
            }

            double cw = Math.max(pane.getPrefWidth(), vw);
            double ch = Math.max(pane.getPrefHeight(), vh);

            if (cw > vw) {
                double from = scroll.getHvalue() * (cw - vw);
                if (v.x < from || v.x + v.w > from + vw) {
                    double target = v.x + v.w / 2 - vw / 2;
                    scroll.setHvalue(clamp(target / (cw - vw)));
                }
            }

            if (ch > vh) {
                double from = scroll.getVvalue() * (ch - vh);
                if (v.y < from || v.y + BOX_H > from + vh) {
                    double target = v.y + BOX_H / 2 - vh / 2;
                    scroll.setVvalue(clamp(target / (ch - vh)));
                }
            }
        });
    }

    private static double clamp(double x) {
        return Math.max(0, Math.min(1, x));
    }


    private void play(boolean immediate) {
        if (trace == null) {
            return;
        }
        if (pos >= trace.steps.size()) {
            goTo(0);
        }
        playing = true;
        playBtn.setText("Зогсоох");
        if (immediate) {
            tick();
        }
        if (playing) {
            startTimeline();
        }
    }

    private void startTimeline() {
        if (timeline != null) {
            timeline.stop();
        }
        timeline = new Timeline(
                new KeyFrame(Duration.millis(delayMs), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void pause() {
        playing = false;
        if (timeline != null) {
            timeline.stop();
        }
        playBtn.setText("Тоглуулах");
    }

    private void tick() {
        stepForward();
        if (trace == null || pos >= trace.steps.size()) {
            pause();
        }
    }

    private void refreshInfo() {
        int n = trace.steps.size();
        stepLabel.setText("Алхам " + pos + " / " + n);

        if (pos == 0) {
            logLabel.setText("Эхний массив: " + java.util.Arrays.toString(
                    trace.values) + ". «Тоглуулах» дарж эхлүүлнэ үү.");
            phaseChip.setVisible(false);
        } else {
            SortTrace.Step s = trace.steps.get(pos - 1);
            logLabel.setText(s.text);
            phaseChip.setText(s.phase);
            phaseChip.setStyle(phaseStyle(s.phase));
            phaseChip.setVisible(!s.phase.isEmpty());
        }

        updatingSlider = true;
        scrubber.setValue(pos);
        updatingSlider = false;

        backBtn.setDisable(pos == 0);
        fwdBtn.setDisable(pos >= n);

        refreshSide();
    }

    private static String phaseStyle(String phase) {
        String bg = switch (phase) {
            case SortTrace.FORK -> "#8f74d6";
            case SortTrace.COMPUTE -> "#3b6fd4";
            case SortTrace.JOIN -> "#2e9e6b";
            case SortTrace.MERGE -> "#e07b39";
            case SortTrace.LEAF -> "#2a9fb0";
            case SortTrace.PARTITION -> "#b26a00";
            default -> "#7fbf8a";
        };
        return "-fx-background-color: " + bg + "; -fx-text-fill: white;"
                + " -fx-font-weight: bold; -fx-padding: 3 10;"
                + " -fx-background-radius: 10;";
    }

    /** Бодит task-ийн thread ба хугацааны тайлбар. */
    static String taskInfo(SortNode t) {
        StringBuilder sb = new StringBuilder();
        sb.append("Бодит гүйцэтгэл: ").append(t.getLabel()).append(' ')
                .append(t.getRange()).append(" → ").append(t.getThread())
                .append(" thread\n");
        sb.append(String.format("эхэлсэн +%.3f ms, дууссан +%.3f ms",
                t.getStartNs() / 1e6, t.getEndNs() / 1e6));
        if (t.getParent() == null) {
            sb.append("\nroot task (pool.invoke)");
        } else if (t.isForked()) {
            sb.append(t.isStolen()
                    ? "\nfork хийгдсэн → өөр worker хулгайлж (steal) авсан"
                    : "\nfork хийгдсэн → эцэг thread өөрөө буцааж авсан");
        } else {
            sb.append("\ncompute() → эцэгтэйгээ нэг thread дээр");
        }
        if (!t.getChildren().isEmpty() && t.getJoinWaitNs() >= 0) {
            sb.append(String.format("\njoin хүлээсэн: %.3f ms",
                    (t.getJoinedNs() - t.getJoinWaitNs()) / 1e6));
        }
        return sb.toString();
    }

    private void setControlsDisabled(boolean disabled) {
        restartBtn.setDisable(disabled);
        backBtn.setDisable(disabled);
        playBtn.setDisable(disabled);
        fwdBtn.setDisable(disabled);
        scrubber.setDisable(disabled);
    }
}