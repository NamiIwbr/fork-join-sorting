import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.RecursiveAction;

public class QuickSortTask<T extends Comparable<T>> extends RecursiveAction {

    private final T[] array;
    private final int left;
    private final int right;
    private final int threshold;
    private final SortNode node;
    private final int depth;
    private final int maxDepth;

    public QuickSortTask(T[] array, int left, int right, int threshold) {
        this(checked(array, left, right, threshold), left, right, threshold,
                new SortNode(left, right), 0, depthLimit(right - left + 1));
    }

    private QuickSortTask(
            T[] array, int left, int right, int threshold,
            SortNode node, int depth, int maxDepth) {
        this.array = array;
        this.left = left;
        this.right = right;
        this.threshold = threshold;
        this.node = node;
        this.depth = depth;
        this.maxDepth = maxDepth;
    }

    /** Maximum recursion depth before the fallback sort is used. */
    public static int depthLimit(int n) {
        return 2 * (32 - Integer.numberOfLeadingZeros(Math.max(1, n))) + 4;
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

        if (right - left + 1 <= threshold) {
            insertionSort();
            node.setSortedValues(
                    Arrays.toString(
                            Arrays.copyOfRange(array, left, right + 1)));
            node.setStatus("Энгийн эрэмбэлэлтээр дууссан");
            return;
        }

        if (depth > maxDepth) {
            Arrays.sort(array, left, right + 1);
            node.setSortedValues(
                    Arrays.toString(
                            Arrays.copyOfRange(array, left, right + 1)));
            node.setStatus("Гүн хэтэрсэн — Arrays.sort ашиглав");
            return;
        }

        int[] bounds = partition();
        int lt = bounds[0];  
        int gt = bounds[1];   // [lt, gt]      == pivot, [gt+1, right] > pivot

        QuickSortTask<T> first = null;
        QuickSortTask<T> second = null;

        if (lt - left >= 2) {
            SortNode leftNode = new SortNode(left, lt - 1);
            node.addChild(leftNode);
            first = new QuickSortTask<>(array, left, lt - 1, threshold,
                    leftNode, depth + 1, maxDepth);
        }

        if (right - gt >= 2) {
            SortNode rightNode = new SortNode(gt + 1, right);
            node.addChild(rightNode);
            second = new QuickSortTask<>(array, gt + 1, right, threshold,
                    rightNode, depth + 1, maxDepth);
        }

        node.setStatus("Дэд даалгавруудыг ажиллуулж байна");

        if (first != null && second != null) {
            invokeAll(first, second);
        } else if (first != null) {
            first.compute();
        } else if (second != null) {
            second.compute();
        }

        node.setSortedValues(
                Arrays.toString(Arrays.copyOfRange(array, left, right + 1)));
        node.setStatus("Эрэмбэлэлт дууссан");
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