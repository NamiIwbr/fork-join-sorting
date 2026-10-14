package mn.edu.must;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SortTrace {

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
        public TNode node;    
        public int[] order;             
        public int[] hl = new int[0];   
        public int pivot = -1;        
        public String status;        
        public boolean done;           
        public final List<TNode> reveal = new ArrayList<>();
        public String text;         
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

        t.resultIds = mergeSort ? t.merge(t.root, ids) : t.quick(t.root, ids, 0);
        return t;
    }

    public int[] sortedValues() {
        int[] out = new int[resultIds.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = values[resultIds[i]];
        }
        return out;
    }


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

        step(n, null, "Энгийн эрэмбэлэлт", false,
                n.range() + " жижиг тул энгийн эрэмбэлэлт хийнэ");

        for (int i = 1; i < cur.length; i++) {
            for (int j = i; j > 0
                    && values[cur[j - 1]] > values[cur[j]]; j--) {

                int x = values[cur[j - 1]];
                int y = values[cur[j]];

                int tmp = cur[j - 1];
                cur[j - 1] = cur[j];
                cur[j] = tmp;

                Step s = step(n, cur, "Энгийн эрэмбэлэлт", false,
                        "Солилцлоо: " + x + " ба " + y);
                s.hl = new int[]{cur[j], cur[j - 1]};
            }
        }

        step(n, cur, "Дууссан", true, n.range() + " эрэмбэлэгдлээ");
        return cur;
    }

    private int[] merge(TNode n, int[] ids) {
        n.input = ids.clone();
        int len = ids.length;

        if (len == 1) {
            step(n, null, "Дууссан", true,
                    n.range() + " нэг тоо тул эрэмбэлэх шаардлагагүй");
            return ids;
        }

        if (len <= threshold) {
            return insertion(n, ids);
        }

        int m = (len - 1) / 2 + 1;
        int[] lIds = Arrays.copyOfRange(ids, 0, m);
        int[] rIds = Arrays.copyOfRange(ids, m, len);

        TNode ln = new TNode(n.left, n.left + m - 1);
        TNode rn = new TNode(n.left + m, n.right);
        ln.input = lIds.clone();
        rn.input = rIds.clone();
        n.children.add(ln);
        n.children.add(rn);

        Step split = step(n, null, "Хуваагдаж байна", false,
                n.range() + "-г хоёр хэсэг болгов: " + ln.range() + " ба " + rn.range());
        split.reveal.add(ln);
        split.reveal.add(rn);

        int[] l = merge(ln, lIds);
        int[] r = merge(rn, rIds);

        // join: sorted halves come back to the parent
        int[] joined = new int[len];
        System.arraycopy(l, 0, joined, 0, l.length);
        System.arraycopy(r, 0, joined, l.length, r.length);

        step(n, joined, "Хоёр хагас ирлээ", false,
                "Хоёр хагас эрэмбэлэгдээд " + n.range() + "-д буцаж ирлээ");

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
                    values[a] + " ба " + values[b] + "-г харьцууллаа → "
                            + (takeLeft ? values[a] : values[b]) + "-г авлаа");
            s.hl = new int[]{a, b};
        }

        while (i < l.length) {
            out[k++] = l[i++];
        }
        while (j < r.length) {
            out[k++] = r[j++];
        }

        step(n, out, "Дууссан", true, n.range() + " эрэмбэлэгдлээ");
        return out;
    }
    private int cmp(int idA, int idB) {
        return Integer.compare(values[idA], values[idB]);
    }

    private static void swap(int[] cur, int i, int j) {
        int t = cur[i];
        cur[i] = cur[j];
        cur[j] = t;
    }

    private void medianSwap(TNode n, int[] cur, int i, int j) {
        int idI = cur[i];
        int idJ = cur[j];
        int a = values[idI];
        int b = values[idJ];

        swap(cur, i, j);

        Step s = step(n, cur, "Pivot сонгож байна", false,
                "Pivot сонгохын тулд " + a + " ба " + b + "-г солилцлоо");
        s.hl = new int[]{idI, idJ};
    }

    private int[] quick(TNode n, int[] ids, int depth) {
        n.input = ids.clone();
        int len = ids.length;

        if (len == 1) {
            step(n, null, "Дууссан", true,
                    n.range() + " нэг тоо тул эрэмбэлэх шаардлагагүй");
            return ids;
        }

        if (len <= threshold) {
            return insertion(n, ids);
        }

        if (depth > QuickSortTask.depthLimit(values.length)) {
            Integer[] boxed = new Integer[len];
            for (int i = 0; i < len; i++) {
                boxed[i] = ids[i];
            }
            Arrays.sort(boxed, (a, b) -> Integer.compare(values[a], values[b]));
            int[] sorted = new int[len];
            for (int i = 0; i < len; i++) {
                sorted[i] = boxed[i];
            }
            step(n, sorted, "Arrays.sort", true,
                    n.range() + " гүн хэтэрсэн тул Arrays.sort ашиглав");
            return sorted;
        }

        int[] cur = ids.clone();
        int l = 0;
        int r = len - 1;
        int m = (len - 1) / 2;

        if (cmp(cur[m], cur[l]) < 0) {
            medianSwap(n, cur, m, l);
        }
        if (cmp(cur[r], cur[l]) < 0) {
            medianSwap(n, cur, r, l);
        }
        if (cmp(cur[r], cur[m]) < 0) {
            medianSwap(n, cur, r, m);
        }

        int pivotId = cur[m];
        int pv = values[pivotId];

        Step first = step(n, null, "Pivot-оор хуваана", false,
                n.range() + " Pivot = " + pv
                        + " (эхний, дунд, сүүлийн 3 тооны голын утга)");
        first.pivot = pivotId;

        int lt = 0;
        int i = 0;
        int gt = len - 1;

        while (i <= gt) {
            int idI = cur[i];
            int c = cmp(idI, pivotId);
            int[] hl;
            String text;

            if (c < 0) {
                int idL = cur[lt];
                swap(cur, lt, i);
                lt++;
                i++;
                hl = new int[]{idI, idL};
                text = values[idI] + " < " + pv + " → зүүн тийш";
            } else if (c > 0) {
                int idG = cur[gt];
                swap(cur, i, gt);
                gt--;
                hl = new int[]{idI, idG};
                text = values[idI] + " > " + pv + " → баруун тийш";
            } else {
                i++;
                hl = new int[]{idI};
                text = values[idI] + " = " + pv + " → голд үлдэнэ";
            }

            Step s = step(n, cur, "Pivot-оор хуваана", false, text);
            s.hl = hl;
            s.pivot = pivotId;
        }

        Step placed = step(n, cur, "Pivot-оор хуваана", false,
                "Хуваалт дууслаа: зүүн тал " + lt + " тоо, голд "
                        + (gt - lt + 1) + " тоо, баруун тал "
                        + (len - 1 - gt) + " тоо");
        placed.pivot = pivotId;

        TNode ln = null;
        TNode rn = null;

        if (lt >= 2) {
            ln = new TNode(n.left, n.left + lt - 1);
            ln.input = Arrays.copyOfRange(cur, 0, lt);
            n.children.add(ln);
        }

        if (len - 1 - gt >= 2) {
            rn = new TNode(n.left + gt + 1, n.right);
            rn.input = Arrays.copyOfRange(cur, gt + 1, len);
            n.children.add(rn);
        }

        if (ln != null || rn != null) {
            Step split = step(n, null, "Хуваагдаж байна", false,
                    "Pivot-ын зүүн, баруун талыг тусад нь эрэмбэлнэ");
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
            int[] s = quick(ln, ln.input, depth + 1);
            System.arraycopy(s, 0, res, 0, s.length);
        }

        if (rn != null) {
            int[] s = quick(rn, rn.input, depth + 1);
            System.arraycopy(s, 0, res, gt + 1, s.length);
        }

        step(n, res, "Дууссан", true,
                n.range() + " эрэмбэлэгдлээ");
        return res;
    }
}