package mn.edu.must;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;


public class SortTests {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + name);
        }
    }

    private static <T extends Comparable<T>> SortNode run(
            T[] a, int threshold, boolean merge) {
        ForkJoinPool pool = new ForkJoinPool();
        try {
            if (merge) {
                MergeSortTask<T> t =
                        new MergeSortTask<>(a, 0, a.length - 1, threshold);
                pool.invoke(t);
                return t.getRootNode();
            } else {
                QuickSortTask<T> t =
                        new QuickSortTask<>(a, 0, a.length - 1, threshold);
                pool.invoke(t);
                return t.getRootNode();
            }
        } finally {
            pool.shutdown();
        }
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

    private static void testInts(String name, int[] input, int threshold) {
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

    public static void main(String[] args) {
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

        try {
            new MergeSortTask<>(box(book), 0, book.length - 1, 0);
            check("threshold 0 rejected", false);
        } catch (IllegalArgumentException ok) {
            check("threshold 0 rejected", true);
        }

        System.out.println("passed=" + passed + " failed=" + failed);
        System.exit(failed == 0 ? 0 : 1);
    }
}