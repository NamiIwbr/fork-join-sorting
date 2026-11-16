import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Бодит fork-join даалгаврын (task) recursion tree-ийн зангилаа.
 *
 * BD2: зангилаа бүр өөрийг нь гүйцэтгэсэн worker thread-ийн нэр болон
 * эхэлсэн / fork хийсэн / join хүлээсэн / join дууссан / дууссан хугацааг
 * хадгална. Эдгээрийг ExecutionLog-ийн t0-оос хойших наносекундээр бичнэ.
 *
 * Талбаруудыг тухайн task-ийг гүйцэтгэж буй thread л бичдэг. Бусад thread
 * уншихаас өмнө ForkJoinTask.join() / ForkJoinPool.invoke() нь
 * happens-before холбоо үүсгэдэг тул нэмэлт синхрончлол шаардлагагүй.
 */
public class SortNode {

    /** Утгын текстийг хадгалах дээд хэмжээ (дүрслэлийн хязгаартай ижил). */
    static final int VALUES_LIMIT = 32;

    private final int left;
    private final int right;

    private String status;
    private String inputValues = "";
    private String sortedValues = "";

    private final List<SortNode> children = new ArrayList<>();
    private SortNode parent;

    // ---- BD2: бодит гүйцэтгэлийн мэдээлэл ----
    private int id;
    private String thread;
    private boolean forked;
    private boolean leaf;
    private long startNs = -1;
    private long splitEndNs = -1;
    private long joinWaitNs = -1;
    private long joinedNs = -1;
    private long endNs = -1;

    public SortNode(int left, int right) {
        this.left = left;
        this.right = right;
        this.status = "Хүлээгдэж байна";
    }

    public int getLeft() {
        return left;
    }

    public int getRight() {
        return right;
    }

    public int size() {
        return right - left + 1;
    }

    public String getRange() {
        return "[" + left + "–" + right + "]";
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getInputValues() {
        return inputValues;
    }

    public void setInputValues(String inputValues) {
        this.inputValues = inputValues;
    }

    public String getSortedValues() {
        return sortedValues;
    }

    public void setSortedValues(String sortedValues) {
        this.sortedValues = sortedValues;
    }

    /**
     * Дэд массивын утгуудыг текст болгоно. BD1-д бүх зангилаа (1 сая
     * элементтэй root хүртэл) Arrays.toString хийдэг байсан нь O(n log n)
     * нэмэлт хугацаа, санах ой зарцуулж байсан тул зөвхөн жижиг (≤ 32)
     * хэсэгт хадгална.
     */
    static String describe(Object[] array, int left, int right) {
        int n = right - left + 1;
        if (n > VALUES_LIMIT) {
            return n + " элемент";
        }
        return Arrays.toString(Arrays.copyOfRange(array, left, right + 1));
    }

    public List<SortNode> getChildren() {
        return children;
    }

    public SortNode getParent() {
        return parent;
    }

    public void addChild(SortNode child) {
        child.parent = this;
        children.add(child);
    }

    // ------------------------------------------------------------------
    // BD2: гүйцэтгэлийн мэдээлэл
    // ------------------------------------------------------------------

    /** Preorder дугаар (T1 = root). Гүйцэтгэл дууссаны дараа олгогдоно. */
    public int getId() {
        return id;
    }

    public String getLabel() {
        return "T" + id;
    }

    void setId(int id) {
        this.id = id;
    }

    /** Энэ task-ийг гүйцэтгэсэн worker thread-ийн нэр (жишээ нь W3). */
    public String getThread() {
        return thread;
    }

    /** Эцэг task нь энэ task-ийг fork() хийж queue-д хийсэн эсэх. */
    public boolean isForked() {
        return forked;
    }

    void setForked(boolean forked) {
        this.forked = forked;
    }

    /** Fork хийгдсэн task-ийг эцгээсээ өөр worker хулгайлж ажиллуулсан эсэх. */
    public boolean isStolen() {
        return forked && parent != null && thread != null
                && !thread.equals(parent.thread);
    }

    /** insertion sort / Arrays.sort хийсэн base-case task эсэх. */
    public boolean isLeaf() {
        return leaf;
    }

    public long getStartNs() {
        return startNs;
    }

    /** Хуваалт дуусч дэд task-ууд эхлэх мөч (fork эсвэл inline compute). */
    public long getSplitEndNs() {
        return splitEndNs;
    }

    public long getJoinWaitNs() {
        return joinWaitNs;
    }

    public long getJoinedNs() {
        return joinedNs;
    }

    public long getEndNs() {
        return endNs;
    }

    // ---- task-ууд дуудна ----

    void begin(ExecutionLog log) {
        thread = Thread.currentThread().getName();
        startNs = log.now();
        log.record(ExecutionLog.Type.START, this, null, startNs);
    }

    void markFork(ExecutionLog log, SortNode child) {
        splitEndNs = log.now();
        log.record(ExecutionLog.Type.FORK, this, child, splitEndNs);
    }

    void markInline(ExecutionLog log, SortNode child) {
        if (splitEndNs < 0) {
            splitEndNs = log.now();
        }
        log.record(ExecutionLog.Type.INLINE, this, child, log.now());
    }

    void markJoinWait(ExecutionLog log, SortNode child) {
        joinWaitNs = log.now();
        log.record(ExecutionLog.Type.JOIN_WAIT, this, child, joinWaitNs);
    }

    void markJoined(ExecutionLog log, SortNode child) {
        joinedNs = log.now();
        if (joinWaitNs < 0) {
            joinWaitNs = joinedNs;
        }
        log.record(ExecutionLog.Type.JOIN, this, child, joinedNs);
    }

    /** Ганц дэд task-ийг inline ажиллуулсны дараа (join байхгүй). */
    void markInlineReturned(ExecutionLog log) {
        joinedNs = log.now();
        joinWaitNs = joinedNs;
    }

    void markLeaf(ExecutionLog log, boolean fallback) {
        leaf = true;
        log.record(fallback ? ExecutionLog.Type.FALLBACK
                : ExecutionLog.Type.LEAF, this, null, log.now());
    }

    void finish(ExecutionLog log) {
        endNs = log.now();
        log.record(ExecutionLog.Type.END, this, null, endNs);
    }
}
