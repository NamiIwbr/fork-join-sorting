import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * GUI-ээс хамааралгүйгээр нэг удаагийн fork-join эрэмбэлэлтийг ажиллуулна.
 * Main (background thread дээр) болон тестүүд хоёулаа үүнийг ашигладаг.
 *
 * Pool бүр шинээр үүсч, worker thread-үүд W1, W2, ... гэж нэрлэгдэнэ.
 */
public final class SortRunner {

    private SortRunner() {
    }

    /** Нэг ажиллагааны үр дүн. */
    public record Result(SortNode root, ExecutionLog log, double elapsedMs,
                         int parallelism, int nodes, int depth) {

        public int workersUsed() {
            return ExecutionLog.threads(root).size();
        }

        public int steals() {
            return ExecutionLog.steals(root);
        }
    }

    /** Нэрлэсэн worker thread-тэй ForkJoinPool үүсгэнэ. */
    public static ForkJoinPool newPool(int parallelism) {
        int p = parallelism > 0 ? parallelism
                : Runtime.getRuntime().availableProcessors();
        AtomicInteger counter = new AtomicInteger();
        ForkJoinPool.ForkJoinWorkerThreadFactory factory = pool -> {
            ForkJoinWorkerThread t =
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory
                            .newThread(pool);
            t.setName("W" + counter.incrementAndGet());
            return t;
        };
        return new ForkJoinPool(p, factory, null, false);
    }

    /**
     * @param parallelism worker-ийн тоо; 0 буюу сөрөг бол CPU цөмийн тоо
     * @param leafDelayMs leaf task бүрт нэмэх зориудын саатал (0 = байхгүй)
     */
    public static <T extends Comparable<T>> Result run(
            T[] data, int threshold, boolean mergeSort,
            int parallelism, long leafDelayMs) {

        ForkJoinPool pool = newPool(parallelism);
        try {
            long start = System.nanoTime();
            ExecutionLog log = new ExecutionLog(leafDelayMs);
            SortNode root;

            if (mergeSort) {
                MergeSortTask<T> task = new MergeSortTask<>(
                        data, 0, data.length - 1, threshold, log);
                pool.invoke(task);
                root = task.getRootNode();
            } else {
                QuickSortTask<T> task = new QuickSortTask<>(
                        data, 0, data.length - 1, threshold, log);
                pool.invoke(task);
                root = task.getRootNode();
            }

            double elapsedMs = (System.nanoTime() - start) / 1_000_000.0;
            int nodes = number(root, 1) - 1;
            return new Result(root, log, elapsedMs, pool.getParallelism(),
                    nodes, depth(root));
        } finally {
            pool.shutdown();
        }
    }

    /** Preorder дугаар олгоно (T1 = root). Дараагийн чөлөөт дугаарыг буцаана. */
    private static int number(SortNode n, int next) {
        n.setId(next++);
        for (SortNode c : n.getChildren()) {
            next = number(c, next);
        }
        return next;
    }

    public static int depth(SortNode n) {
        int d = 0;
        for (SortNode c : n.getChildren()) {
            d = Math.max(d, depth(c));
        }
        return 1 + d;
    }
}
