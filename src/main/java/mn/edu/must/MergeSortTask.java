
package mn.edu.must;

import java.util.concurrent.RecursiveAction;
import java.util.Arrays;

public class MergeSortTask extends RecursiveAction {

    private final int[] array;
    private final int left;
    private final int right;
    private final int threshold;
    private final SortNode node;

    public MergeSortTask(
            int[] array, int left, int right, int threshold) {
        this(array, left, right, threshold, new SortNode(left, right));
    }

    private MergeSortTask(
            int[] array,
            int left,
            int right,
            int threshold,
            SortNode node) {
        this.array = array;
        this.left = left;
        this.right = right;
        this.threshold = threshold;
        this.node = node;
    }

    public SortNode getRootNode() {
        return node;
    }

    @Override
    protected void compute() {
        node.setInputValues(
            Arrays.toString(Arrays.copyOfRange(array, left, right + 1))
        );

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
                Arrays.toString(Arrays.copyOfRange(array, left, right + 1))
            );

            node.setStatus("Энгийн эрэмбэлэлтээр дууссан");
            return;
        }

        int middle = left + (right - left) / 2;

        SortNode leftNode = new SortNode(left, middle);
        SortNode rightNode = new SortNode(middle + 1, right);

        node.addChild(leftNode);
        node.addChild(rightNode);

        MergeSortTask first = new MergeSortTask(
            array, left, middle, threshold, leftNode
        );

        MergeSortTask second = new MergeSortTask(
            array, middle + 1, right, threshold, rightNode
        );

        invokeAll(first, second);

        node.setStatus("Нэгтгэж байна");

        merge(middle);

        node.setSortedValues(
            Arrays.toString(Arrays.copyOfRange(array, left, right + 1))
        );

        node.setStatus("Нэгтгэл дууссан");
    }

    private void insertionSort() {
        for (int i = left + 1; i <= right; i++) {
            int key = array[i];
            int j = i - 1;

            while (j >= left && array[j] > key) {
                array[j + 1] = array[j];
                j--;
            }

            array[j + 1] = key;
        }
    }

    private void merge(int middle) {
        int[] temp = new int[right - left + 1];

        int i = left;
        int j = middle + 1;
        int k = 0;

        while (i <= middle && j <= right) {
            if (array[i] <= array[j]) {
                temp[k++] = array[i++];
            } else {
                temp[k++] = array[j++];
            }
        }

        while (i <= middle) {
            temp[k++] = array[i++];
        }

        while (j <= right) {
            temp[k++] = array[j++];
        }

        System.arraycopy(temp, 0, array, left, temp.length);
    }
}
