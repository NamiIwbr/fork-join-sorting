import java.util.Arrays;
import java.util.Random;

/**
 * Гүйцэтгэлийн хэмжилт (BD2). JUnit тест биш — гараар ажиллуулна:
 *
 *   mvn -q test-compile
 *   java -Xmx4g -cp target/classes:target/test-classes Benchmark
 *
 * (Windows-д classpath тусгаарлагч нь ';')
 *
 * Тохиргоо бүрийг 3 удаа халааж (warm-up), дараа нь 5 удаа хэмжиж
 * median-ийг авна. Өгөгдөл нь тогтмол seed (2026)-тэй санамсаргүй Integer.
 */
public class Benchmark {

    private static final int WARMUP = 3;
    private static final int RUNS = 5;

    public static void main(String[] args) {
        int n = args.length > 0 ? Integer.parseInt(args[0]) : 1_000_000;
        int cores = Runtime.getRuntime().availableProcessors();

        System.out.printf("# Орчин: %s %s, Java %s, CPU цөм %d, n=%,d, "
                        + "warm-up %d, хэмжилт %d (median)%n%n",
                System.getProperty("os.name"), System.getProperty("os.arch"),
                System.getProperty("java.version"), cores, n, WARMUP, RUNS);

        Random r = new Random(2026);
        Integer[] base = new Integer[n];
        for (int i = 0; i < n; i++) {
            base[i] = r.nextInt();
        }

        boolean onlyBd1 = args.length > 1 && args[1].equals("bd1");
        if (onlyBd1) {
            bd1(base);
            return;
        }

        double seq = median(() -> {
            Integer[] d = base.clone();
            long s = System.nanoTime();
            Arrays.sort(d);
            return ms(s);
        });
        System.out.printf("Arrays.sort (дараалсан, 1 thread): %.1f ms%n%n", seq);

        // 1) Worker-ийн тоо (parallelism) ба хурдсалт
        System.out.println("## 1. Parallelism (threshold = 1000)");
        System.out.println("| Алгоритм | Worker | Median ms | Хурдсалт T(1)/T(p) "
                + "| Ашигласан worker | Steal |");
        System.out.println("|---|---|---|---|---|---|");
        for (boolean merge : new boolean[]{true, false}) {
            double t1 = 0;
            for (int p : new int[]{1, 2, 4, 8}) {
                if (p > cores * 2) {
                    continue;
                }
                SortRunner.Result[] last = new SortRunner.Result[1];
                double t = median(() -> {
                    Integer[] d = base.clone();
                    last[0] = SortRunner.run(d, 1000, merge, p, 0);
                    check(d);
                    return last[0].elapsedMs();
                });
                if (p == 1) {
                    t1 = t;
                }
                System.out.printf("| %s | %d | %.1f | %.2f× | %d | %d |%n",
                        merge ? "Merge" : "Quick", p, t, t1 / t,
                        last[0].workersUsed(), last[0].steals());
            }
        }

        // 2) Threshold-ийн нөлөө
        System.out.println();
        System.out.println("## 2. Threshold (worker = " + cores + ")");
        System.out.println("| Алгоритм | Threshold | Task-ийн тоо | Median ms |");
        System.out.println("|---|---|---|---|");
        for (boolean merge : new boolean[]{true, false}) {
            for (int th : new int[]{16, 64, 256, 1024, 4096, 16384}) {
                SortRunner.Result[] last = new SortRunner.Result[1];
                double t = median(() -> {
                    Integer[] d = base.clone();
                    last[0] = SortRunner.run(d, th, merge, cores, 0);
                    check(d);
                    return last[0].elapsedMs();
                });
                System.out.printf("| %s | %d | %,d | %.1f |%n",
                        merge ? "Merge" : "Quick", th, last[0].nodes(), t);
            }
        }

        System.out.println();
        bd1(base);
    }

    /** 3) BD1-тэй харьцуулах тохиргоо (8 worker). `... Benchmark 1000000 bd1` */
    private static void bd1(Integer[] base) {
        System.out.println("## 3. BD1-тэй ижил тохиргоо (worker = 8)");
        System.out.println("| Алгоритм | Threshold | Median ms |");
        System.out.println("|---|---|---|");
        for (int th : new int[]{1000, 16}) {
            for (boolean merge : new boolean[]{true, false}) {
                double t = median(() -> {
                    Integer[] d = base.clone();
                    SortRunner.Result res = SortRunner.run(d, th, merge, 8, 0);
                    check(d);
                    return res.elapsedMs();
                });
                System.out.printf("| %s | %d | %.1f |%n",
                        merge ? "Merge" : "Quick", th, t);
            }
        }
    }

    private interface Run {
        double once();
    }

    private static double median(Run run) {
        for (int i = 0; i < WARMUP; i++) {
            run.once();
        }
        double[] t = new double[RUNS];
        for (int i = 0; i < RUNS; i++) {
            t[i] = run.once();
        }
        Arrays.sort(t);
        return t[RUNS / 2];
    }

    private static double ms(long start) {
        return (System.nanoTime() - start) / 1_000_000.0;
    }

    private static void check(Integer[] d) {
        for (int i = 1; i < d.length; i++) {
            if (d[i - 1] > d[i]) {
                throw new AssertionError("эрэмбэлэгдээгүй байна: " + i);
            }
        }
    }
}
