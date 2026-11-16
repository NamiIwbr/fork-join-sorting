import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Алгоритмын тест. JUnit-гүйгээр `java SortTests` хэлбэрээр, эсвэл
 * `mvn test` (SortSuiteTest-ээр дамжиж) ажиллана.
 *
 * BD1-ийн regression тестүүд (R...) өөрчлөгдөөгүй, BD2-т шинэ тестүүд
 * (N...) нэмэгдсэн.
 */
public class SortTests {

    private int passed = 0;
    private int failed = 0;
    private final StringBuilder failures = new StringBuilder();
    private final boolean verbose;

    private SortTests(boolean verbose) {
        this.verbose = verbose;
    }

    /** Нийт шалгалтын үр дүн. */
    public record Summary(int passed, int failed, String failures) {
    }

    private void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            failures.append("FAIL: ").append(name).append('\n');
            if (verbose) {
                System.out.println("FAIL: " + name);
            }
        }
    }

    private static <T extends Comparable<T>> SortNode run(
            T[] a, int threshold, boolean merge) {
        return SortRunner.run(a, threshold, merge, 0, 0).root();
    }

    private static Integer[] box(int[] a) {
        Integer[] b = new Integer[a.length];
        for (int i = 0; i < a.length; i++) {
            b[i] = a[i];
        }
        return b;
    }

    private static boolean sameTree(SortNode a, SortTrace.TNode b) {
        if (a.getLeft() != b.left || a.getRight() != b.right) {
            return false;
        }
        if (a.getChildren().size() != b.children.size()) {
            return false;
        }
        for (int i = 0; i < b.children.size(); i++) {
            if (!sameTree(a.getChildren().get(i), b.children.get(i))) {
                return false;
            }
        }
        return true;
    }

    private void testInts(String name, int[] input, int threshold) {
        for (boolean merge : new boolean[]{true, false}) {
            String id = name + " th=" + threshold
                    + (merge ? " merge" : " quick");
            int[] expected = input.clone();
            Arrays.sort(expected);

            Integer[] a = box(input);
            SortNode tree = run(a, threshold, merge);

            int[] got = new int[a.length];
            for (int i = 0; i < a.length; i++) {
                got[i] = a[i];
            }
            check(id + " sorted", Arrays.equals(expected, got));

            if (input.length <= SortTrace.MAX_SIZE) {
                SortTrace tr = SortTrace.build(input, threshold, merge);
                check(id + " trace result",
                        Arrays.equals(expected, tr.sortedValues()));
                check(id + " same tree", sameTree(tree, tr.root));
            }
        }
    }

    private void expectReject(String name, Runnable r) {
        try {
            r.run();
            check(name, false);
        } catch (IllegalArgumentException | NullPointerException ok) {
            check(name, true);
        }
    }

    // ------------------------------------------------------------------
    // BD1 regression
    // ------------------------------------------------------------------

    private void regression() {
        int[] book = {7, 12, 19, 3, 18, 4, 2, 6, 15, 8};
        for (int th = 1; th <= 6; th++) {
            testInts("book", book, th);
        }
        testInts("single", new int[]{5}, 2);
        testInts("two", new int[]{2, 1}, 1);
        testInts("duplicates", new int[]{3, 3, 3, 1, 1, 2, 2, 2}, 2);
        testInts("negatives", new int[]{-5, 3, -1, 0, -9, 8}, 1);
        testInts("allEqual", new int[]{5, 5, 5, 5, 5, 5, 5, 5}, 2);
        int[] sorted = new int[32];
        int[] reverse = new int[32];
        for (int i = 0; i < 32; i++) {
            sorted[i] = i;
            reverse[i] = 32 - i;
        }
        testInts("sorted", sorted, 2);
        testInts("reverse", reverse, 2);

        Random r = new Random(42);
        for (int it = 0; it < 1500; it++) {
            int n = 1 + r.nextInt(32);
            int[] v = new int[n];
            for (int i = 0; i < n; i++) {
                v[i] = r.nextInt(25) - 12;
            }
            testInts("random#" + it, v, 1 + r.nextInt(6));
        }

        int n = 10000;
        int[] rnd = new int[n];
        int[] asc = new int[n];
        int[] desc = new int[n];
        int[] eq = new int[n];
        int[] few = new int[n];
        for (int i = 0; i < n; i++) {
            rnd[i] = r.nextInt(2001) - 1000;
            asc[i] = i;
            desc[i] = n - i;
            eq[i] = 7;
            few[i] = r.nextInt(3);
        }
        for (int th : new int[]{1, 2, 100}) {
            testInts("random10000", rnd, th);
            testInts("sorted10000", asc, th);
            testInts("reverse10000", desc, th);
            testInts("allEqual10000", eq, th);
            testInts("fewDistinct10000", few, th);
        }

        String[] words = {"pear", "apple", "fig", "banana", "cherry", "date"};
        for (boolean merge : new boolean[]{true, false}) {
            String[] w = words.clone();
            String[] e = words.clone();
            Arrays.sort(e);
            run(w, 2, merge);
            check("strings " + (merge ? "merge" : "quick"),
                    Arrays.equals(e, w));
        }

        expectReject("threshold 0 rejected",
                () -> new MergeSortTask<>(box(book), 0, book.length - 1, 0));
    }

    // ------------------------------------------------------------------
    // BD2 шинэ тестүүд
    // ------------------------------------------------------------------

    /** N1 (A4, T06): API түвшинд хоосон массив алдаагүй боловсруулагдана. */
    private void emptyArray() {
        for (boolean merge : new boolean[]{true, false}) {
            String id = "N1 empty " + (merge ? "merge" : "quick");
            Integer[] a = new Integer[0];
            try {
                SortRunner.Result res = SortRunner.run(a, 2, merge, 0, 0);
                check(id + " no error", a.length == 0);
                check(id + " one task", res.nodes() == 1);
                SortTrace tr = SortTrace.build(new int[0], 2, merge);
                check(id + " trace empty", tr.sortedValues().length == 0);
            } catch (RuntimeException e) {
                check(id + " no error (" + e + ")", false);
            }
        }
    }

    /** N2 (A4): Quick Sort threshold=0 ба бусад буруу параметр татгалзагдана. */
    private void invalidArguments() {
        Integer[] a = box(new int[]{3, 1, 2});
        expectReject("N2 quick threshold 0",
                () -> new QuickSortTask<>(a, 0, 2, 0));
        expectReject("N2 quick threshold -1",
                () -> new QuickSortTask<>(a, 0, 2, -1));
        expectReject("N2 merge threshold -5",
                () -> new MergeSortTask<>(a, 0, 2, -5));
        expectReject("N2 null array",
                () -> new MergeSortTask<Integer>(null, 0, 0, 1));
        expectReject("N2 range past end",
                () -> new QuickSortTask<>(a, 0, 3, 1));
        expectReject("N2 negative left",
                () -> new MergeSortTask<>(a, -1, 2, 1));
        expectReject("N2 trace threshold 0",
                () -> SortTrace.build(new int[]{1, 2}, 0, true));
        expectReject("N2 negative delay",
                () -> new ExecutionLog(-1));
    }

    /** N3: Integer-ийн хязгаарын утгууд (overflow-гүй харьцуулалт). */
    private void extremes() {
        testInts("N3 int extremes",
                new int[]{Integer.MAX_VALUE, 0, Integer.MIN_VALUE, -1, 1,
                        Integer.MIN_VALUE, Integer.MAX_VALUE}, 1);
        testInts("N3 reviewer case", new int[]{3, -1, 3, 0}, 1);
    }

    /** N4 (A4): GUI оролтын шалгалт — хоосон текст GUI-д алдаа байна. */
    private void parser() {
        check("N4 parse ok", Arrays.equals(
                InputParser.parseNumbers(" 8, -3 ,0,2147483647 "),
                new Integer[]{8, -3, 0, Integer.MAX_VALUE}));
        expectReject("N4 empty text", () -> InputParser.parseNumbers("   "));
        expectReject("N4 null text", () -> InputParser.parseNumbers(null));
        expectReject("N4 double comma", () -> InputParser.parseNumbers("1,,2"));
        expectReject("N4 trailing comma", () -> InputParser.parseNumbers("1,2,"));
        expectReject("N4 decimal", () -> InputParser.parseNumbers("2.5"));
        expectReject("N4 word", () -> InputParser.parseNumbers("1, a"));
        expectReject("N4 overflow",
                () -> InputParser.parseNumbers("2147483648"));
        check("N4 threshold ok", InputParser.parseThreshold(" 3 ") == 3);
        expectReject("N4 threshold 0", () -> InputParser.parseThreshold("0"));
        expectReject("N4 threshold text", () -> InputParser.parseThreshold("x"));
        expectReject("N4 threshold empty", () -> InputParser.parseThreshold(""));
        check("N4 size ok", InputParser.parseRandomSize("10000") == 10000);
        expectReject("N4 size 0", () -> InputParser.parseRandomSize("0"));
        expectReject("N4 size big", () -> InputParser.parseRandomSize("10001"));
        check("N4 delay ok", InputParser.parseLeafDelay("0") == 0);
        expectReject("N4 delay negative",
                () -> InputParser.parseLeafDelay("-1"));
        expectReject("N4 delay big", () -> InputParser.parseLeafDelay("501"));
    }

    /** N5: fork/join үйл явдлын бүртгэл ба хугацааны дараалал зөв эсэх. */
    private void instrumentation() {
        Random r = new Random(7);
        for (int it = 0; it < 200; it++) {
            int n = 1 + r.nextInt(64);
            int[] v = new int[n];
            for (int i = 0; i < n; i++) {
                v[i] = r.nextInt(50) - 25;
            }
            int th = 1 + r.nextInt(4);
            int workers = 1 + r.nextInt(4);
            for (boolean merge : new boolean[]{true, false}) {
                String id = "N5 #" + it + (merge ? " merge" : " quick");
                SortRunner.Result res = SortRunner.run(
                        box(v), th, merge, workers, 0);
                check(id + " timings", timingsOk(res.root()));
                check(id + " workers <= parallelism",
                        res.workersUsed() <= workers);

                long forks = res.log().count(ExecutionLog.Type.FORK);
                long joins = res.log().count(ExecutionLog.Type.JOIN);
                check(id + " fork == join", forks == joins);
                check(id + " fork == forked nodes",
                        forks == countForked(res.root()));
                check(id + " start == end == tasks",
                        res.log().count(ExecutionLog.Type.START) == res.nodes()
                        && res.log().count(ExecutionLog.Type.END)
                        == res.nodes());
                check(id + " preorder ids", idsOk(res.root(), new int[]{1}));

                if (n <= SortTrace.MAX_SIZE) {
                    SortTrace tr = SortTrace.build(v, th, merge);
                    check(id + " trace FORK steps == real forks",
                            tr.countPhase(SortTrace.FORK) == forks);
                    check(id + " trace JOIN steps == real joins",
                            tr.countPhase(SortTrace.JOIN) == joins);
                }
            }
        }
    }

    /** N6: parallelism=1 үед бүх task нэг thread дээр, steal байхгүй. */
    private void singleWorker() {
        int[] v = new int[1000];
        Random r = new Random(1);
        for (int i = 0; i < v.length; i++) {
            v[i] = r.nextInt();
        }
        for (boolean merge : new boolean[]{true, false}) {
            String id = "N6 one worker " + (merge ? "merge" : "quick");
            SortRunner.Result res = SortRunner.run(box(v), 4, merge, 1, 0);
            check(id + " parallelism 1", res.parallelism() == 1);
            check(id + " single thread", res.workersUsed() == 1);
            check(id + " no steal", res.steals() == 0);
        }
    }

    /**
     * N7: leaf саатал нэмж, 4 worker-тэй үед leaf-үүд зэрэг ажиллаж,
     * нийт хугацаа дараалсан хугацаанаас бага байх ёстой
     * (8 leaf × 40 ms = 320 ms дараалсан).
     */
    private void parallelOverlap() {
        int[] v = {8, 3, 6, 1, 9, 2, 7, 4, 16, 11, 14, 10, 13, 12, 15, 5};
        SortRunner.Result res = SortRunner.run(box(v), 2, true, 4, 40);
        long leaves = countLeaves(res.root());
        check("N7 leaves", leaves == 8);
        check("N7 >1 worker used", res.workersUsed() > 1);
        check("N7 steal happened", res.steals() > 0);
        check("N7 elapsed < sequential",
                res.elapsedMs() < leaves * 40 * 0.9);
        check("N7 overlapping leaves", maxConcurrentLeaves(res.root()) >= 2);
    }

    private static boolean timingsOk(SortNode n) {
        if (n.getThread() == null || n.getStartNs() < 0
                || n.getEndNs() < n.getStartNs()) {
            return false;
        }
        if (!n.getChildren().isEmpty()) {
            if (n.getSplitEndNs() < n.getStartNs()
                    || n.getJoinWaitNs() < n.getSplitEndNs()
                    || n.getJoinedNs() < n.getJoinWaitNs()
                    || n.getEndNs() < n.getJoinedNs()) {
                return false;
            }
        }
        for (SortNode c : n.getChildren()) {
            // Хүүхэд нь эцгийн fork/compute-оос хойш эхэлж,
            // эцгийн join дуусахаас өмнө дуусна.
            if (c.getStartNs() < n.getSplitEndNs()
                    || c.getEndNs() > n.getJoinedNs()) {
                return false;
            }
            if (!c.isForked() && !c.getThread().equals(n.getThread())) {
                return false;   // compute() нь заавал эцгийн thread дээр
            }
            if (!timingsOk(c)) {
                return false;
            }
        }
        return true;
    }

    private static long countForked(SortNode n) {
        long c = n.isForked() ? 1 : 0;
        for (SortNode k : n.getChildren()) {
            c += countForked(k);
        }
        return c;
    }

    private static long countLeaves(SortNode n) {
        if (n.getChildren().isEmpty()) {
            return 1;
        }
        long c = 0;
        for (SortNode k : n.getChildren()) {
            c += countLeaves(k);
        }
        return c;
    }

    private static boolean idsOk(SortNode n, int[] next) {
        if (n.getId() != next[0]++) {
            return false;
        }
        for (SortNode c : n.getChildren()) {
            if (!idsOk(c, next)) {
                return false;
            }
        }
        return true;
    }

    private static int maxConcurrentLeaves(SortNode root) {
        List<SortNode> leaves = new java.util.ArrayList<>();
        collectLeaves(root, leaves);
        int best = 0;
        for (SortNode a : leaves) {
            int c = 0;
            Set<String> threads = new HashSet<>();
            for (SortNode b : leaves) {
                if (b.getStartNs() <= a.getStartNs()
                        && b.getEndNs() > a.getStartNs()) {
                    c++;
                    threads.add(b.getThread());
                }
            }
            if (threads.size() == c) {
                best = Math.max(best, c);
            }
        }
        return best;
    }

    private static void collectLeaves(SortNode n, List<SortNode> out) {
        if (n.getChildren().isEmpty()) {
            out.add(n);
        }
        for (SortNode c : n.getChildren()) {
            collectLeaves(c, out);
        }
    }

    // ------------------------------------------------------------------

    public static Summary runAll(boolean verbose) {
        SortTests t = new SortTests(verbose);
        t.regression();
        int regressionPassed = t.passed;
        int regressionFailed = t.failed;
        t.emptyArray();
        t.invalidArguments();
        t.extremes();
        t.parser();
        t.instrumentation();
        t.singleWorker();
        t.parallelOverlap();
        if (verbose) {
            System.out.println("regression(BD1): passed=" + regressionPassed
                    + " failed=" + regressionFailed);
            System.out.println("new(BD2):        passed="
                    + (t.passed - regressionPassed)
                    + " failed=" + (t.failed - regressionFailed));
        }
        return new Summary(t.passed, t.failed, t.failures.toString());
    }

    public static void main(String[] args) {
        Summary s = runAll(true);
        System.out.println("passed=" + s.passed() + " failed=" + s.failed());
        System.exit(s.failed() == 0 ? 0 : 1);
    }
}
