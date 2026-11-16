import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Бодит fork-join гүйцэтгэлийн үйл явдлын бүртгэл (BD2).
 *
 * Task бүр аль worker thread дээр, хэзээ эхэлж, хэзээ fork хийж,
 * хэзээ join-оор хүлээж, хэзээ дууссаныг nanoTime-аар бичнэ.
 * Олон thread зэрэг бичдэг тул ConcurrentLinkedQueue ашиглана.
 */
public final class ExecutionLog {

    public enum Type {
        START("START", "task эхэллээ"),
        FORK("FORK", "дэд task-ийг fork() хийж queue-д хийв"),
        INLINE("COMPUTE", "дэд task-ийг энэ thread дээр шууд ажиллуулав"),
        LEAF("LEAF", "threshold-оос жижиг тул insertion sort хийв"),
        FALLBACK("FALLBACK", "гүн хэтэрсэн тул Arrays.sort хийв"),
        JOIN_WAIT("JOIN WAIT", "join() дуудаж дэд task-ийг хүлээж байна"),
        JOIN("JOIN", "дэд task дуусч, үр дүн буцаж ирэв"),
        END("END", "task дууслаа");

        public final String code;
        public final String text;

        Type(String code, String text) {
            this.code = code;
            this.text = text;
        }
    }

    /** Нэг үйл явдал. nanos нь бүртгэл эхэлснээс хойших хугацаа. */
    public record Event(long nanos, Type type, SortNode node,
                        SortNode other, String thread) {
    }

    private final long t0 = System.nanoTime();
    private final long leafDelayMs;
    private final ConcurrentLinkedQueue<Event> events =
            new ConcurrentLinkedQueue<>();

    public ExecutionLog() {
        this(0);
    }

    /**
     * @param leafDelayMs leaf task бүрт нэмэх зориудын саатал (ms).
     *                    Жижиг өгөгдөл дээр worker-уудын зэрэгцээ ажиллагааг
     *                    нүдэнд харагдахуйц болгоход ашиглана. 0 = саатал байхгүй.
     */
    public ExecutionLog(long leafDelayMs) {
        if (leafDelayMs < 0) {
            throw new IllegalArgumentException("leafDelayMs must be >= 0");
        }
        this.leafDelayMs = leafDelayMs;
    }

    public long now() {
        return System.nanoTime() - t0;
    }

    public long leafDelayMs() {
        return leafDelayMs;
    }

    void record(Type type, SortNode node, SortNode other, long nanos) {
        events.add(new Event(nanos, type, node, other,
                Thread.currentThread().getName()));
    }

    /** Leaf task дээр зориудын ажлын ачаалал (sleep) нэмнэ. */
    void simulateWork() {
        if (leafDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(leafDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Бүх үйл явдал хугацааны дарааллаар. */
    public List<Event> events() {
        List<Event> list = new ArrayList<>(events);
        list.sort(Comparator.comparingLong(Event::nanos));
        return list;
    }

    public long count(Type type) {
        return events.stream().filter(e -> e.type() == type).count();
    }

    // ------------------------------------------------------------------
    // Хураангуй статистик (модноос тооцно)
    // ------------------------------------------------------------------

    /** Worker нэрсийг дугаараар нь эрэмбэлэн буцаана (W1, W2, ...). */
    public static List<String> threads(SortNode root) {
        TreeMap<Integer, String> sorted = new TreeMap<>();
        List<String> other = new ArrayList<>();
        collectThreads(root, sorted, other);
        List<String> out = new ArrayList<>(sorted.values());
        for (String s : other) {
            if (!out.contains(s)) {
                out.add(s);
            }
        }
        return out;
    }

    private static void collectThreads(SortNode n, TreeMap<Integer, String> sorted,
                                       List<String> other) {
        String t = n.getThread();
        if (t != null) {
            int idx = workerIndex(t);
            if (idx > 0) {
                sorted.put(idx, t);
            } else if (!other.contains(t)) {
                other.add(t);
            }
        }
        for (SortNode c : n.getChildren()) {
            collectThreads(c, sorted, other);
        }
    }

    /** "W3" → 3, бусад нэрд 0. */
    public static int workerIndex(String thread) {
        if (thread != null && thread.length() > 1 && thread.charAt(0) == 'W') {
            try {
                return Integer.parseInt(thread.substring(1));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    /** Fork хийгдсэн дэд task-ийг өөр worker хулгайлж (steal) ажиллуулсан тоо. */
    public static int steals(SortNode n) {
        int s = n.isStolen() ? 1 : 0;
        for (SortNode c : n.getChildren()) {
            s += steals(c);
        }
        return s;
    }

    /** Worker бүрийн гүйцэтгэсэн task-ийн тоо. */
    public static Map<String, Integer> tasksPerThread(SortNode root) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (String t : threads(root)) {
            m.put(t, 0);
        }
        countTasks(root, m);
        return m;
    }

    private static void countTasks(SortNode n, Map<String, Integer> m) {
        if (n.getThread() != null) {
            m.merge(n.getThread(), 1, Integer::sum);
        }
        for (SortNode c : n.getChildren()) {
            countTasks(c, m);
        }
    }

    /**
     * Worker бүрийн "өөрийн ажил" хийсэн хугацаа (ns): leaf-ийн бүтэн хугацаа,
     * дотоод зангилааны хуваах [start, fork] ба нэгтгэх [join, end] хэсэг.
     * join-ийг хүлээж байх хугацааг оруулахгүй.
     */
    public static Map<String, Long> busyPerThread(SortNode root) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (String t : threads(root)) {
            m.put(t, 0L);
        }
        sumBusy(root, m);
        return m;
    }

    private static void sumBusy(SortNode n, Map<String, Long> m) {
        if (n.getThread() != null) {
            long busy;
            if (n.getChildren().isEmpty()) {
                busy = n.getEndNs() - n.getStartNs();
            } else {
                busy = (n.getSplitEndNs() - n.getStartNs())
                        + (n.getEndNs() - n.getJoinedNs());
            }
            m.merge(n.getThread(), Math.max(0, busy), Long::sum);
        }
        for (SortNode c : n.getChildren()) {
            sumBusy(c, m);
        }
    }
}
