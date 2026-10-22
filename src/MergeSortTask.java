import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.RecursiveAction;

/**
 * Fork-join Merge Sort. Elements must implement Comparable.
 * The list is divided into two (almost) equal halves; when a sub-list is
 * not longer than the threshold, insertion sort is applied directly.
 */
public class MergeSortTask<T extends Comparable<T>> extends RecursiveAction {

    private final T[] array;
    private final int left;
    private final int right;
    private final int threshold;
    private final SortNode node;

    public MergeSortTask(T[] array, int left, int right, int threshold) {
        this(checked(array, left, right, threshold),
                left, right, threshold, new SortNode(left, right));
    }

    private MergeSortTask(
            T[] array, int left, int right, int threshold, SortNode node) {
        this.array = array;
        this.left = left;
        this.right = right;
        this.threshold = threshold;
        this.node = node;
    }

    private static <E> E[] checked(
            E[] array, int left, int right, int threshold) {
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

    @Override
    protected void compute() {
        node.setInputValues(
                Arrays.toString(Arrays.copyOfRange(array, left, right + 1)));
        node.setStatus("Боловсруулж байна");

        if (left >= right) {
            node.setSortedValues(node.getInputValues());
            node.setStatus("Дууссан");
            return;
        }

        int length = right - left + 1;

        if (length <= threshold) {
            insertionSort();
            node.setSortedValues(
                    Arrays.toString(
                            Arrays.copyOfRange(array, left, right + 1)));
            node.setStatus("Энгийн эрэмбэлэлтээр дууссан");
            return;
        }

        int middle = left + (right - left) / 2;

        SortNode leftNode = new SortNode(left, middle);
        SortNode rightNode = new SortNode(middle + 1, right);
        node.addChild(leftNode);
        node.addChild(rightNode);

        MergeSortTask<T> first =
                new MergeSortTask<>(array, left, middle, threshold, leftNode);
        MergeSortTask<T> second =
                new MergeSortTask<>(array, middle + 1, right, threshold,
                        rightNode);

        invokeAll(first, second);

        node.setStatus("Нэгтгэж байна");
        merge(middle);

        node.setSortedValues(
                Arrays.toString(Arrays.copyOfRange(array, left, right + 1)));
        node.setStatus("Нэгтгэл дууссан");
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