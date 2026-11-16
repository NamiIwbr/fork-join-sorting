import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.RecursiveAction;

/**
 * Fork-join Merge Sort. Elements must implement Comparable.
 * The list is divided into two (almost) equal halves; when a sub-list is
 * not longer than the threshold, insertion sort is applied directly.
 *
 * BD2: invokeAll()-ийн оронд сурах бичгийн загвар болох
 *   second.fork();  first.compute();  second.join();
 * гэсэн тодорхой fork / join цэгүүдийг ашиглаж, цэг бүрийг ExecutionLog-д
 * thread-ийн нэр, хугацаатай нь бүртгэнэ.
 */
public class MergeSortTask<T extends Comparable<T>> extends RecursiveAction {

    private final T[] array;
    private final int left;
    private final int right;
    private final int threshold;
    private final SortNode node;
    private final ExecutionLog log;

    public MergeSortTask(T[] array, int left, int right, int threshold) {
        this(array, left, right, threshold, new ExecutionLog());
    }

    public MergeSortTask(T[] array, int left, int right, int threshold,
                         ExecutionLog log) {
        this(checked(array, left, right, threshold),
                left, right, threshold, new SortNode(left, right),
                Objects.requireNonNull(log, "log"));
    }

    private MergeSortTask(T[] array, int left, int right, int threshold,
                          SortNode node, ExecutionLog log) {
        this.array = array;
        this.left = left;
        this.right = right;
        this.threshold = threshold;
        this.node = node;
        this.log = log;
    }

    static <E> E[] checked(E[] array, int left, int right, int threshold) {
        Objects.requireNonNull(array, "array");
        if (threshold < 1) {
            throw new IllegalArgumentException("threshold must be >= 1");
        }
        if (left < 0 || right >= array.length || left > right + 1) {
            throw new IllegalArgumentException("invalid range");
        }
        return array;
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

        // Base case: 0/1 элемент эсвэл threshold-оос жижиг → insertion sort.
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

        int middle = left + (right - left) / 2;

        SortNode leftNode = new SortNode(left, middle);
        SortNode rightNode = new SortNode(middle + 1, right);
        node.addChild(leftNode);
        node.addChild(rightNode);

        MergeSortTask<T> first = new MergeSortTask<>(
                array, left, middle, threshold, leftNode, log);
        MergeSortTask<T> second = new MergeSortTask<>(
                array, middle + 1, right, threshold, rightNode, log);

        // FORK: баруун хагасыг энэ worker-ийн deque-д хийнэ. Сул worker
        // байвал түүнийг хулгайлж (work stealing) зэрэг ажиллуулна.
        rightNode.setForked(true);
        node.markFork(log, rightNode);
        second.fork();

        // Зүүн хагасыг энэ thread өөрөө шууд гүйцэтгэнэ.
        node.markInline(log, leftNode);
        first.compute();

        // JOIN: баруун хагас дуустал хүлээнэ (хулгайлагдаагүй бол
        // энэ thread өөрөө буцааж аваад ажиллуулна).
        node.markJoinWait(log, rightNode);
        second.join();
        node.markJoined(log, rightNode);

        node.setStatus("Нэгтгэж байна");
        merge(middle);

        node.setSortedValues(
                SortNode.describe(array, left, right));
        node.setStatus("Нэгтгэл дууссан");
        node.finish(log);
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

    private void merge(int middle) {
        T[] copy = Arrays.copyOfRange(array, left, right + 1);
        int mid = middle - left;

        int i = 0;
        int j = mid + 1;
        int k = left;

        while (i <= mid && j < copy.length) {
            if (copy[i].compareTo(copy[j]) <= 0) {
                array[k++] = copy[i++];
            } else {
                array[k++] = copy[j++];
            }
        }

        while (i <= mid) {
            array[k++] = copy[i++];
        }

        while (j < copy.length) {
            array[k++] = copy[j++];
        }
    }
}
