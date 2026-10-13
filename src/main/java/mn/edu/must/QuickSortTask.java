
package mn.edu.must;

import java.util.concurrent.RecursiveAction;
import java.util.Arrays;

public class QuickSortTask extends RecursiveAction {

    private final int[] array;
    private final int left;
    private final int right;
    private final int threshold;
    private final SortNode node;

    public QuickSortTask(
            int[] array, int left, int right, int threshold) {
        this(
                array,
                left,
                right,
                threshold,
                new SortNode(left, right)
        );
    }

    private QuickSortTask(
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

        if (right - left + 1 <= threshold) {
            insertionSort();

            node.setSortedValues(
                Arrays.toString(Arrays.copyOfRange(array, left, right + 1))
            );

            node.setStatus("Энгийн эрэмбэлэлтээр дууссан");
            return;
        }

        int pivotIndex = partition();

        QuickSortTask first = null;
        QuickSortTask second = null;

        if (left < pivotIndex - 1) {
            SortNode leftNode = new SortNode(left, pivotIndex - 1);
            node.addChild(leftNode);

            first = new QuickSortTask(
                array, left, pivotIndex - 1, threshold, leftNode
            );
        }

        if (pivotIndex + 1 < right) {
            SortNode rightNode = new SortNode(pivotIndex + 1, right);
            node.addChild(rightNode);

            second = new QuickSortTask(
                array, pivotIndex + 1, right, threshold, rightNode
            );
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
            Arrays.toString(Arrays.copyOfRange(array, left, right + 1))
        );

        node.setStatus("Эрэмбэлэлт дууссан");
    }


    private int partition() {
        int pivot = array[right];
        int i = left - 1;

        for (int j = left; j < right; j++) {
            if (array[j] <= pivot) {
                i++;
                swap(i, j);
            }
        }

        swap(i + 1, right);
        return i + 1;
    }

    private void swap(int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
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
}
