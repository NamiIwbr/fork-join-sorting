import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BD2: бодит ForkJoinPool worker thread-үүдийн гүйцэтгэлийг хугацааны
 * тэнхлэг дээр (Gantt хэлбэрээр) харуулна.
 *
 * Мөр бүр нэг worker thread. Хайрцаг бүр нэг task-ийн өөрийн ажил:
 * хуваах (fork хүртэл), leaf insertion sort, join хүлээх, нэгтгэх.
 * Нил ягаан сум = fork хийгдсэн task-ийг өөр worker хулгайлсан (steal),
 * ногоон сум = тэр task дуусч эцэг рүүгээ join-оор буцсан.
 *
 * Энд харагдах бүх зүйл ExecutionLog-д бодитоор бичигдсэн хугацаа
 * (System.nanoTime) бөгөөд зохиомол анимаци биш.
 */
public class ThreadTimeline extends VBox {

    private static final String[] PALETTE = {
            "#3b6fd4", "#e07b39", "#2e9e6b", "#b44fb5",
            "#c94c4c", "#2a9fb0", "#8a6d3b", "#5c6bc0"
    };

    static final String C_SPLIT = "#3b6fd4";
    static final String C_LEAF = "#2a9fb0";
    static final String C_WAIT = "#c9ccd2";
    static final String C_MERGE = "#e07b39";
    static final String C_FORK = "#8f74d6";
    static final String C_JOIN = "#2e9e6b";

    private static final double LABEL_W = 56;
    private static final double AXIS_H = 24;
    private static final double LANE_H = 26;
    private static final double BAR_H = 16;
    private static final int MAX_EVENTS_SHOWN = 20_000;
    private static final int MAX_ARROWS = 3_000;

    /** Worker-ийн нэрээр тогтмол өнгө (W1 → 1-р өнгө, ...). */
    public static String workerColor(String thread) {
        int idx = ExecutionLog.workerIndex(thread);
        if (idx <= 0) {
            return "#6b7280";
        }
        return PALETTE[(idx - 1) % PALETTE.length];
    }

    private record Hit(double x, double y, double w, double h, SortNode node) {
    }

    private final Label summary = new Label();
    private final Label perWorker = new Label();
    private final Label selection = new Label();
    private final Canvas canvas = new Canvas(600, 120);
    private final ScrollPane scroll;
    private final ListView<String> eventList = new ListView<>();

    private SortNode root;
    private List<String> lanes = new ArrayList<>();
    private final Map<String, Integer> laneIndex = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private SortNode highlighted;
    private long total = 1;

    public ThreadTimeline() {
        super(8);
        getStyleClass().add("card");

        Label title = new Label("Бодит thread-үүдийн гүйцэтгэл (ForkJoinPool)");
        title.getStyleClass().add("key");

        summary.getStyleClass().add("stats");
        summary.setWrapText(true);
        perWorker.getStyleClass().add("stats");
        perWorker.setWrapText(true);

        Pane holder = new Pane(canvas);
        scroll = new ScrollPane(holder);
        scroll.getStyleClass().add("viz-scroll");
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setMinHeight(90);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        scroll.viewportBoundsProperty().addListener((o, a, b) -> redraw());

        canvas.setOnMouseClicked(e -> {
            SortNode hit = null;
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (e.getX() >= h.x && e.getX() <= h.x + h.w
                        && e.getY() >= h.y && e.getY() <= h.y + h.h) {
                    hit = h.node;
                    break;
                }
            }
            highlight(hit);
        });

        selection.getStyleClass().add("side-task");
        selection.setWrapText(true);
        selection.setMaxWidth(Double.MAX_VALUE);
        selection.setText("Хайрцаг дээр дарж task-ийн мэдээллийг харна.");

        FlowPane legend = new FlowPane(14, 4,
                swatch(C_SPLIT, "хуваах / partition (fork хүртэл)"),
                swatch(C_LEAF, "leaf: insertion sort"),
                swatch(C_WAIT, "join() хүлээж байна"),
                swatch(C_MERGE, "join-ийн дараах нэгтгэл"),
                swatch(C_FORK, "fork → steal (өөр worker)"),
                swatch(C_JOIN, "join ← буцсан"));

        VBox timelineBox = new VBox(6, scroll, legend, selection);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        eventList.getStyleClass().add("event-list");
        eventList.setPlaceholder(new Label("Бүртгэл хоосон."));

        TabPane tabs = new TabPane(
                new Tab("Thread timeline", timelineBox),
                new Tab("Fork/Join үйл явдлын бүртгэл", eventList));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabs, Priority.ALWAYS);

        getChildren().addAll(title, summary, perWorker, tabs);
        clear("«Эрэмбэлэх» дарахад worker thread бүр аль task-ийг хэзээ "
                + "гүйцэтгэснийг энд харуулна.");
    }

    private HBox swatch(String color, String text) {
        Rectangle r = new Rectangle(14, 10, Color.web(color));
        r.setArcWidth(3);
        r.setArcHeight(3);
        Label l = new Label(text);
        l.getStyleClass().add("muted");
        l.setStyle("-fx-font-size: 11px;");
        HBox h = new HBox(5, r, l);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    public void clear(String message) {
        root = null;
        lanes = new ArrayList<>();
        laneIndex.clear();
        hits.clear();
        highlighted = null;
        summary.setText(message);
        perWorker.setText("");
        eventList.getItems().clear();
        selection.setText("Хайрцаг дээр дарж task-ийн мэдээллийг харна.");
        redraw();
    }

    public void load(SortRunner.Result r) {
        root = r.root();
        highlighted = null;
        lanes = ExecutionLog.threads(root);
        laneIndex.clear();
        for (int i = 0; i < lanes.size(); i++) {
            laneIndex.put(lanes.get(i), i);
        }
        total = Math.max(1, root.getEndNs());

        ExecutionLog log = r.log();
        summary.setText(String.format(
                "Worker ашигласан: %d / parallelism %d  ·  task: %d  ·  "
                + "fork: %d  ·  join: %d  ·  steal: %d  ·  pool доторх хугацаа: "
                + "%.3f ms%s",
                lanes.size(), r.parallelism(), r.nodes(),
                log.count(ExecutionLog.Type.FORK),
                log.count(ExecutionLog.Type.JOIN),
                r.steals(), root.getEndNs() / 1e6,
                log.leafDelayMs() > 0
                        ? "  ·  leaf саатал " + log.leafDelayMs() + " ms"
                        : ""));

        Map<String, Integer> tasks = ExecutionLog.tasksPerThread(root);
        Map<String, Long> busy = ExecutionLog.busyPerThread(root);
        StringBuilder sb = new StringBuilder();
        for (String t : lanes) {
            if (sb.length() > 0) {
                sb.append("   ");
            }
            sb.append(t).append(": ").append(tasks.getOrDefault(t, 0))
                    .append(" task, ")
                    .append(String.format("%.2f ms ажилласан",
                            busy.getOrDefault(t, 0L) / 1e6));
        }
        perWorker.setText(sb.toString());

        List<ExecutionLog.Event> events = log.events();
        List<String> rows = new ArrayList<>(
                Math.min(events.size(), MAX_EVENTS_SHOWN) + 1);
        for (ExecutionLog.Event e : events) {
            if (rows.size() >= MAX_EVENTS_SHOWN) {
                rows.add("… нийт " + events.size() + " үйл явдлаас эхний "
                        + MAX_EVENTS_SHOWN + "-г харууллаа");
                break;
            }
            rows.add(format(e));
        }
        eventList.getItems().setAll(rows);

        selection.setText("Хайрцаг дээр дарж task-ийн мэдээллийг харна. "
                + "Модны алхам солигдоход харгалзах task тодорно.");
        redraw();
    }

    static String format(ExecutionLog.Event e) {
        SortNode n = e.node();
        SortNode o = e.other();
        String who = n.getLabel() + " " + n.getRange();
        String what = switch (e.type()) {
            case START -> who + " эхэллээ";
            case FORK -> who + " → fork() " + o.getLabel() + " " + o.getRange()
                    + " (queue-д)";
            case INLINE -> o == null
                    ? who + " — дэд task шаардлагагүй"
                    : who + " → compute() " + o.getLabel() + " " + o.getRange()
                    + " (энэ thread)";
            case LEAF -> who + " insertion sort (leaf)";
            case FALLBACK -> who + " Arrays.sort (гүн хэтэрсэн)";
            case JOIN_WAIT -> who + " join() дуудаж " + o.getLabel()
                    + "-г хүлээж байна";
            case JOIN -> who + " ← " + o.getLabel() + " join дууслаа ("
                    + o.getLabel() + " нь " + o.getThread() + " дээр ажилласан"
                    + (o.isStolen() ? ", steal" : "") + ")";
            case END -> who + " дууслаа";
        };
        return String.format("%12s  %-4s %-9s %s",
                String.format("+%.3f ms", e.nanos() / 1e6), e.thread(),
                e.type().code, what);
    }

    /** Модны одоогийн алхамтай холбогдох task-ийг тодруулна. */
    public void highlight(SortNode node) {
        highlighted = node;
        if (node != null && node.getThread() != null) {
            selection.setText(TreeCanvas.taskInfo(node).replace("\n", "  ·  ")
                    .replace("Бодит гүйцэтгэл: ", ""));
        }
        redraw();
    }

    // ------------------------------------------------------------------
    // Зурах
    // ------------------------------------------------------------------

    private double xOf(long ns, double width) {
        double plot = Math.max(10, width - LABEL_W - 14);
        return LABEL_W + (double) ns / total * plot;
    }

    private double laneY(String thread) {
        Integer i = laneIndex.get(thread);
        return AXIS_H + (i == null ? 0 : i) * LANE_H;
    }

    private void redraw() {
        double width = Math.max(420, scroll.getViewportBounds().getWidth() - 2);
        double height = root == null ? 90
                : AXIS_H + lanes.size() * LANE_H + 8;
        canvas.setWidth(width);
        canvas.setHeight(height);

        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setFill(Color.WHITE);
        g.fillRect(0, 0, width, height);
        hits.clear();

        if (root == null) {
            g.setFill(Color.web("#6b7280"));
            g.setFont(Font.font(13));
            g.setTextAlign(TextAlignment.LEFT);
            g.setTextBaseline(VPos.TOP);
            g.fillText("Одоогоор гүйцэтгэлийн мэдээлэл алга.", 14, 14);
            return;
        }

        drawAxis(g, width, height);

        // Мөрийн (lane) дэвсгэр ба нэр
        for (int i = 0; i < lanes.size(); i++) {
            String t = lanes.get(i);
            double y = AXIS_H + i * LANE_H;
            g.setFill(Color.web(i % 2 == 0 ? "#f7f8fa" : "#ffffff"));
            g.fillRect(LABEL_W, y, width - LABEL_W, LANE_H);
            g.setFill(Color.web(workerColor(t)));
            g.fillRoundRect(6, y + 7, LABEL_W - 14, LANE_H - 14, 8, 8);
            g.setFill(Color.WHITE);
            g.setFont(Font.font("System", FontWeight.BOLD, 12));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.fillText(t, 6 + (LABEL_W - 14) / 2, y + LANE_H / 2);
        }

        List<SortNode> all = new ArrayList<>();
        collect(root, all);
        boolean small = all.size() <= MAX_ARROWS;

        // 1) join хүлээх нимгэн зурвас
        for (SortNode n : all) {
            if (!n.getChildren().isEmpty() && n.getJoinedNs() > n.getJoinWaitNs()
                    && n.getJoinWaitNs() >= 0) {
                double y = laneY(n.getThread()) + LANE_H / 2 - 3;
                double x1 = xOf(n.getJoinWaitNs(), width);
                double x2 = xOf(n.getJoinedNs(), width);
                g.setFill(Color.web(C_WAIT));
                g.fillRect(x1, y, Math.max(1, x2 - x1), 6);
            }
        }

        // 2) өөрийн ажлын хэсгүүд (эцэг эхэлж, хүүхэд дээр нь)
        for (SortNode n : all) {
            if (n.getThread() == null) {
                continue;
            }
            if (n.getChildren().isEmpty()) {
                bar(g, n, n.getStartNs(), n.getEndNs(),
                        n.isLeaf() ? C_LEAF : C_SPLIT, width, true);
            } else {
                bar(g, n, n.getStartNs(), n.getSplitEndNs(), C_SPLIT, width,
                        false);
                bar(g, n, n.getJoinedNs(), n.getEndNs(), C_MERGE, width,
                        false);
            }
        }

        // 3) fork (steal) ба join сумнууд
        if (small) {
            for (SortNode n : all) {
                SortNode p = n.getParent();
                if (p == null || !n.isForked()) {
                    continue;
                }
                double py = laneY(p.getThread()) + LANE_H / 2;
                double cy = laneY(n.getThread()) + LANE_H / 2;
                double fx = xOf(p.getSplitEndNs(), width);
                double sx = xOf(n.getStartNs(), width);
                double ex = xOf(n.getEndNs(), width);
                double jx = xOf(p.getJoinedNs(), width);

                if (n.isStolen()) {
                    arrow(g, fx, py, sx, cy, C_FORK, true);
                    arrow(g, ex, cy, jx, py, C_JOIN, false);
                } else {
                    tick(g, fx, py, C_FORK);
                    tick(g, jx, py, C_JOIN);
                }
            }
        }

        // 4) сонгосон task
        if (highlighted != null && highlighted.getThread() != null) {
            g.setStroke(Color.web("#111827"));
            g.setLineWidth(2);
            g.setLineDashes();
            double y = laneY(highlighted.getThread()) + (LANE_H - BAR_H) / 2;
            double x1 = xOf(highlighted.getStartNs(), width);
            double x2 = xOf(highlighted.getEndNs(), width);
            g.strokeRoundRect(x1 - 2, y - 3, Math.max(4, x2 - x1) + 4,
                    BAR_H + 6, 6, 6);
        }
    }

    private void drawAxis(GraphicsContext g, double width, double height) {
        g.setStroke(Color.web("#d8dbe0"));
        g.setLineWidth(1);
        g.setFill(Color.web("#6b7280"));
        g.setFont(Font.font(10));
        g.setTextBaseline(VPos.TOP);
        int ticks = 6;
        for (int i = 0; i <= ticks; i++) {
            long ns = total * i / ticks;
            double x = xOf(ns, width);
            g.strokeLine(x, AXIS_H - 4, x, height);
            g.setTextAlign(i == 0 ? TextAlignment.LEFT
                    : i == ticks ? TextAlignment.RIGHT : TextAlignment.CENTER);
            g.fillText(String.format("%.2f ms", ns / 1e6), x, 4);
        }
    }

    private void bar(GraphicsContext g, SortNode n, long from, long to,
                     String color, double width, boolean label) {
        if (from < 0 || to < from) {
            return;
        }
        double x1 = xOf(from, width);
        double w = Math.max(1.5, xOf(to, width) - x1);
        double y = laneY(n.getThread()) + (LANE_H - BAR_H) / 2;
        g.setFill(Color.web(color));
        g.fillRoundRect(x1, y, w, BAR_H, 4, 4);
        hits.add(new Hit(x1, y, w, BAR_H, n));

        String text = n.getLabel();
        if (label && w >= 8 * text.length() + 6) {
            g.setFill(Color.WHITE);
            g.setFont(Font.font("System", FontWeight.BOLD, 10));
            g.setTextAlign(TextAlignment.CENTER);
            g.setTextBaseline(VPos.CENTER);
            g.fillText(text, x1 + w / 2, y + BAR_H / 2);
        }
    }

    private void arrow(GraphicsContext g, double x1, double y1, double x2,
                       double y2, String color, boolean dashed) {
        g.setStroke(Color.web(color));
        g.setFill(Color.web(color));
        g.setLineWidth(1.6);
        if (dashed) {
            g.setLineDashes(5, 4);
        } else {
            g.setLineDashes();
        }
        g.strokeLine(x1, y1, x2, y2);
        g.setLineDashes();
        double ang = Math.atan2(y2 - y1, x2 - x1);
        double s = 7;
        g.fillPolygon(
                new double[]{x2, x2 - s * Math.cos(ang - 0.4),
                        x2 - s * Math.cos(ang + 0.4)},
                new double[]{y2, y2 - s * Math.sin(ang - 0.4),
                        y2 - s * Math.sin(ang + 0.4)}, 3);
    }

    private void tick(GraphicsContext g, double x, double y, String color) {
        g.setFill(Color.web(color));
        g.fillPolygon(new double[]{x - 4, x + 4, x},
                new double[]{y - BAR_H / 2 - 6, y - BAR_H / 2 - 6,
                        y - BAR_H / 2}, 3);
    }

    private static void collect(SortNode n, List<SortNode> out) {
        out.add(n);
        for (SortNode c : n.getChildren()) {
            collect(c, out);
        }
    }
}
