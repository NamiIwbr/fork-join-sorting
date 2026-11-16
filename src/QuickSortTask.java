import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.RecursiveAction;

/**
 * Fork-join Quick Sort: median-of-three pivot, three-way partition,
 * threshold-оос жижиг хэсэгт insertion sort, рекурсийн гүний хамгаалалт.
 *
 * BD2: хоёр дэд хэсэгтэй үед second.fork(); first.compute(); second.join();
 * ганц дэд хэсэгтэй үед fork хийлгүйгээр энэ thread дээр шууд үргэлжлүүлнэ.
 * Fork / join цэг бүр ExecutionLog-д бүртгэгдэнэ.
 */
public class QuickSortTask<T extends Comparable<T>> extends RecursiveAction {

    private final T[] array;
    private final int left;
    private final int right;
    private final int threshold;
    private final SortNode node;
    private final int depth;
    private final int maxDepth;
    private final ExecutionLog log;

    public QuickSortTask(T[] array, int left, int right, int threshold) {
        this(array, left, right, threshold, new ExecutionLog());
    }

    public QuickSortTask(T[] array, int left, int right, int threshold,
                         ExecutionLog log) {
        this(MergeSortTask.checked(array, left, right, threshold),
                left, right, threshold, new SortNode(left, right), 0,
                depthLimit(right - left + 1),
                Objects.requireNonNull(log, "log"));
    }

    private QuickSortTask(
            T[] array, int left, int right, int threshold,
            SortNode node, int depth, int maxDepth, ExecutionLog log) {
        this.array = array;
        this.left = left;
        this.right = right;
        this.threshold = threshold;
        this.node = node;
        this.depth = depth;
        this.maxDepth = maxDepth;
        this.log = log;
    }

    /** Maximum recursion depth before the fallback sort is used. */
    public static int depthLimit(int n) {
        return 2 * (32 - Integer.numberOfLeadingZeros(Math.max(1, n))) + 4;
    }

    public SortNode getRootNode() {
        return node;
    }

    public ExecutionLog getLog() {
        return log;
    }

    @Override
    protected void compute() {
        node.begin(log);
        node.setInputValues(
                SortNode.describe(array, left, right));
        node.setStatus("Боловсруулж байна");

        int length = right - left + 1;

        if (length <= 1 || length <= threshold) {
            insertionSort();
            log.simulateWork();
            node.markLeaf(log, false);
            node.setSortedValues(
                    SortNode.describe(array, left, right));
            node.setStatus(length <= 1 ? "Дууссан"
                    : "Энгийн эрэмбэлэлтээр дууссан");
            node.finish(log);
            return;
        }

        if (depth > maxDepth) {
            Arrays.sort(array, left, right + 1);
            log.simulateWork();
            node.markLeaf(log, true);
            node.setSortedValues(
                    SortNode.describe(array, left, right));
            node.setStatus("Гүн хэтэрсэн — Arrays.sort ашиглав");
            node.finish(log);
            return;
        }

        int[] bounds = partition();
        int lt = bounds[0];   // [left, lt-1] < pivot
        int gt = bounds[1];   // [lt, gt] == pivot, [gt+1, right] > pivot

        QuickSortTask<T> first = null;
        QuickSortTask<T> second = null;
        SortNode leftNode = null;
        SortNode rightNode = null;

        if (lt - left >= 2) {
            leftNode = new SortNode(left, lt - 1);
            node.addChild(leftNode);
            first = new QuickSortTask<>(array, left, lt - 1, threshold,
                    leftNode, depth + 1, maxDepth, log);
        }

        if (right - gt >= 2) {
            rightNode = new SortNode(gt + 1, right);
            node.addChild(rightNode);
            second = new QuickSortTask<>(array, gt + 1, right, threshold,
                    rightNode, depth + 1, maxDepth, log);
        }

        node.setStatus("Дэд даалгавруудыг ажиллуулж байна");

        if (first != null && second != null) {
            // FORK баруун хэсэг → queue; зүүн хэсгийг энэ thread хийнэ.
            rightNode.setForked(true);
            node.markFork(log, rightNode);
            second.fork();

            node.markInline(log, leftNode);
            first.compute();

            // JOIN: баруун хэсэг дуустал хүлээнэ.
            node.markJoinWait(log, rightNode);
            second.join();
            node.markJoined(log, rightNode);

        } else if (first != null || second != null) {
            // Ганц хэсэг үлдсэн → fork хийх шаардлагагүй.
            QuickSortTask<T> only = first != null ? first : second;
            node.markInline(log, first != null ? leftNode : rightNode);
            only.compute();
            node.markInlineReturned(log);
        } else {
            // Бүх элемент pivot-той тэнцүү эсвэл хоёр тал ≤ 1 элементтэй.
            node.markInline(log, null);
            node.markInlineReturned(log);
        }

        node.setSortedValues(
                SortNode.describe(array, left, right));
        node.setStatus("Эрэмбэлэлт дууссан");
        node.finish(log);
    }

    /** Median-of-three pivot + three-way partition. Returns {lt, gt}. */
    private int[] partition() {
        int mid = left + (right - left) / 2;

        if (array[mid].compareTo(array[left]) < 0) {
            swap(mid, left);
        }
        if (array[right].compareTo(array[left]) < 0) {
            swap(right, left);
        }
        if (array[right].compareTo(array[mid]) < 0) {
            swap(right, mid);
        }

        T pivot = array[mid];

        int lt = left;
        int i = left;
        int gt = right;

        while (i <= gt) {
            int c = array[i].compareTo(pivot);

            if (c < 0) {
                swap(lt++, i++);
            } else if (c > 0) {
                swap(i, gt--);
            } else {
                i++;
            }
        }

        return new int[]{lt, gt};
    }

    private void swap(int i, int j) {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    private void insertionSort() {
        for (int i = left + 1; i <= right; i++) {
            T key = array[i];
            int j = i - 1;

            while (j >= left && array[j].compareTo(key) > 0) {
                array[j + 1] = array[j];
                j--;
            }

            array[j + 1] = key;
        }
    }
}
