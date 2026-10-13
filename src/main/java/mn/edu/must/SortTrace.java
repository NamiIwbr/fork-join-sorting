package mn.edu.must;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Records, step by step, how Merge Sort / Quick Sort (with the same
 * threshold rules as MergeSortTask / QuickSortTask) rearrange the numbers
 * in every node of the recursion tree.
 *
 * Every number is tracked by an "id" (its original index), so the UI can
 * animate each number moving from one slot to another.
 *
 * The trace is generated sequentially (left subtree, then right subtree),
 * so the replay is deterministic even though the real fork-join run is
 * parallel.
 */
public class SortTrace {

    /** Visualization limit (more numbers make the tree unreadable). */
    public static final int MAX_SIZE = 32;

    public static class TNode {
        public final int left;
        public final int right;
        public int[] input;
        public final List<TNode> children = new ArrayList<>();

        TNode(int left, int right) {
            this.left = left;
            this.right = right;
        }

        public int size() {
            return right - left + 1;
        }

        public String range() {
            return "[" + left + "–" + right + "]";
        }
    }

    public static class Step {
        public TNode node;                 // node whose cells change
        public int[] order;                // new order of ids (null = unchanged)
        public int[] hl = new int[0];      // highlighted ids (compared / moved)
        public int pivot = -1;             // pivot id (Quick Sort)
        public String status;              // short status shown in the node
        public boolean done;               // node finished
        public final List<TNode> reveal = new ArrayList<>(); // nodes appearing
        public String text;                // explanation shown to the user
    }

    public final int[] values;
    public final List<Step> steps = new ArrayList<>();
    public TNode root;

    private final int threshold;
    private int[] resultIds;

    private SortTrace(int[] values, int threshold) {
        this.values = values.clone();
        this.threshold = threshold;
    }

    public static SortTrace build(int[] values, int threshold,
                                  boolean mergeSort) {
        SortTrace t = new SortTrace(values, threshold);

        int n = values.length;
        int[] ids = new int[n];
        for (int i = 0; i < n; i++) {
            ids[i] = i;
        }

        t.root = new TNode(0, n - 1);
        t.root.input = ids.clone();

        t.resultIds = mergeSort ? t.merge(t.root, ids) : t.quick(t.root, ids);
        return t;
    }

    /** Final sorted values according to the trace. */
    public int[] sortedValues() {
        int[] out = new int[resultIds.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = values[resultIds[i]];
        }
        return out;
    }

    // ------------------------------------------------------------ helpers

    private Step step(TNode n, int[] order, String status,
                      boolean done, String text) {
        Step s = new Step();
        s.node = n;
        s.order = order == null ? null : order.clone();
        s.status = status;
        s.done = done;
        s.text = text;
        steps.add(s);
        return s;
    }

    private int[] insertion(TNode n, int[] ids) {
        int[] cur = ids.clone();

        step(n, null, "Insertion sort", false,
                n.range() + ": хэмжээ ≤ threshold → энгийн (insertion) эрэмбэлэлт");

        for (int i = 1; i < cur.length; i++) {
            for (int j = i; j > 0
                    && values[cur[j - 1]] > values[cur[j]]; j--) {

                int x = values[cur[j - 1]];
                int y = values[cur[j]];

                int tmp = cur[j - 1];
                cur[j - 1] = cur[j];
                cur[j] = tmp;

                Step s = step(n, cur, "Insertion sort", false,
                        "Солилцоо: " + x + " ↔ " + y);
                s.hl = new int[]{cur[j], cur[j - 1]};
            }
        }

        step(n, cur, "Дууссан", true, n.range() + " эрэмбэлэгдлээ");
        return cur;
    }

    // --------------------------------------------------------- merge sort

    private int[] merge(TNode n, int[] ids) {
        n.input = ids.clone();
        int len = ids.length;

        if (len == 1) {
            step(n, null, "Нэг элемент — дууссан", true,
                    n.range() + ": нэг элемент, эрэмбэлэх шаардлагагүй");
            return ids;
        }

        if (len <= threshold) {
            return insertion(n, ids);
        }

        int m = (len - 1) / 2 + 1;   // same split as MergeSortTask
        int[] lIds = Arrays.copyOfRange(ids, 0, m);
        int[] rIds = Arrays.copyOfRange(ids, m, len);

        TNode ln = new TNode(n.left, n.left + m - 1);
        TNode rn = new TNode(n.left + m, n.right);
        ln.input = lIds.clone();
        rn.input = rIds.clone();
        n.children.add(ln);
        n.children.add(rn);

        Step split = step(n, null, "Задарч байна (fork)", false,
                "fork: " + n.range() + " → " + ln.range() + " + " + rn.range());
        split.reveal.add(ln);
        split.reveal.add(rn);

        int[] l = merge(ln, lIds);
        int[] r = merge(rn, rIds);

        // join: sorted halves come back to the parent
        int[] joined = new int[len];
        System.arraycopy(l, 0, joined, 0, l.length);
        System.arraycopy(r, 0, joined, l.length, r.length);

        step(n, joined, "Нэгтгэл (join)", false,
                "join: эрэмбэлэгдсэн хагасууд " + n.range() + "-д буцаж ирлээ");

        int i = 0;
        int j = 0;
        int k = 0;
        int[] out = new int[len];

        while (i < l.length && j < r.length) {
            int a = l[i];
            int b = r[j];
            boolean takeLeft = values[a] <= values[b];

            if (takeLeft) {
                out[k++] = l[i++];
            } else {
                out[k++] = r[j++];
            }

            int[] order = new int[len];
            System.arraycopy(out, 0, order, 0, k);
            int p = k;
            for (int x = i; x < l.length; x++) {
                order[p++] = l[x];
            }
            for (int x = j; x < r.length; x++) {
                order[p++] = r[x];
            }

            Step s = step(n, order, "Нэгтгэж байна", false,
                    values[a] + " ба " + values[b] + "-г харьцуулав → "
                            + (takeLeft ? values[a] : values[b]) + "-г авлаа");
            s.hl = new int[]{a, b};
        }

        while (i < l.length) {
            out[k++] = l[i++];
        }
        while (j < r.length) {
            out[k++] = r[j++];
        }

        step(n, out, "Дууссан", true, n.range() + " нэгтгэл дууслаа");
        return out;
    }

    // --------------------------------------------------------- quick sort

    private int[] quick(TNode n, int[] ids) {
        n.input = ids.clone();
        int len = ids.length;

        if (len == 1) {
            step(n, null, "Нэг элемент — дууссан", true,
                    n.range() + ": нэг элемент, эрэмбэлэх шаардлагагүй");
            return ids;
        }

        if (len <= threshold) {
            return insertion(n, ids);
        }

        int[] cur = ids.clone();
        int pivotId = cur[len - 1];
        int pv = values[pivotId];

        Step first = step(n, null, "Partition", false,
                n.range() + ": pivot = " + pv + " (сүүлийн элемент)");
        first.pivot = pivotId;

        int i = -1;

        for (int j = 0; j < len - 1; j++) {
            int idJ = cur[j];
            int[] hl;
            String text;

            if (values[idJ] <= pv) {
                i++;
                int idI = cur[i];

                if (i != j) {
                    cur[i] = idJ;
                    cur[j] = idI;
                    text = values[idJ] + " ≤ " + pv + " → " + i
                            + "-р байрлалтай солилцоо";
                    hl = new int[]{idJ, idI};
                } else {
                    text = values[idJ] + " ≤ " + pv + " → байрандаа үлдлээ";
                    hl = new int[]{idJ};
                }
            } else {
                text = values[idJ] + " > " + pv + " → баруун талд үлдэнэ";
                hl = new int[]{idJ};
            }

            Step s = step(n, cur, "Partition", false, text);
            s.hl = hl;
            s.pivot = pivotId;
        }

        int p = i + 1;
        int tmp = cur[p];
        cur[p] = cur[len - 1];
        cur[len - 1] = tmp;

        Step placed = step(n, cur, "Partition", false,
                "pivot " + pv + " эцсийн байрандаа орлоо ("
                        + (n.left + p) + "-р байр)");
        placed.pivot = pivotId;

        TNode ln = null;
        TNode rn = null;

        if (p >= 2) {
            ln = new TNode(n.left, n.left + p - 1);
            ln.input = Arrays.copyOfRange(cur, 0, p);
            n.children.add(ln);
        }

        if (len - 1 - p >= 2) {
            rn = new TNode(n.left + p + 1, n.right);
            rn.input = Arrays.copyOfRange(cur, p + 1, len);
            n.children.add(rn);
        }

        if (ln != null || rn != null) {
            Step split = step(n, null, "Задарч байна (fork)", false,
                    "fork: pivot-ын зүүн, баруун талыг дэд даалгавар болгов");
            split.pivot = pivotId;
            if (ln != null) {
                split.reveal.add(ln);
            }
            if (rn != null) {
                split.reveal.add(rn);
            }
        }

        int[] res = cur.clone();

        if (ln != null) {
            int[] s = quick(ln, ln.input);
            System.arraycopy(s, 0, res, 0, s.length);
        }

        if (rn != null) {
            int[] s = quick(rn, rn.input);
            System.arraycopy(s, 0, res, p + 1, s.length);
        }

        step(n, res, "Дууссан", true,
                "join: " + n.range() + " эрэмбэлэгдлээ");
        return res;
    }
}