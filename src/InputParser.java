/**
 * GUI-ийн текст оролтыг шалгаж хөрвүүлнэ. JavaFX-ээс хамааралгүй тул
 * тестээр шууд шалгагдана.
 *
 * Анхаар: алгоритмын API (MergeSortTask / QuickSortTask) хоосон массивыг
 * алдаагүй боловсруулдаг. Харин GUI-д хоосон талбар нь хэрэглэгчийн алдаа
 * тул энд татгалзана (BD2, шүүмж A4).
 */
public final class InputParser {

    public static final int MAX_RANDOM_SIZE = 10_000;
    public static final int MAX_LEAF_DELAY_MS = 500;
    public static final int MAX_WORKERS = 64;

    private InputParser() {
    }

    public static Integer[] parseNumbers(String text) {
        String input = text == null ? "" : text.trim();
        if (input.isEmpty()) {
            throw new IllegalArgumentException(
                    "Тоонуудын жагсаалт хоосон байна. "
                    + "Жишээ нь: 8, 3, 6, 1 гэж оруулна уу.");
        }

        String[] parts = input.split(",", -1);
        Integer[] numbers = new Integer[parts.length];

        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.isEmpty()) {
                throw new IllegalArgumentException(
                        "Тоонуудын хооронд илүү таслал байна ("
                        + (i + 1) + "-р байрлал).");
            }
            try {
                numbers[i] = Integer.parseInt(p);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "«" + shorten(p) + "» нь бүхэл тоо биш эсвэл "
                        + "−2147483648 … 2147483647 мужаас гарсан байна.");
            }
        }
        return numbers;
    }

    public static int parseThreshold(String text) {
        int t = parseInt(text, "Threshold");
        if (t < 1) {
            throw new IllegalArgumentException(
                    "Threshold нь 1 буюу түүнээс их байх ёстой.");
        }
        return t;
    }

    public static int parseRandomSize(String text) {
        int n = parseInt(text, "Санамсаргүй тооны хэмжээ");
        if (n < 1 || n > MAX_RANDOM_SIZE) {
            throw new IllegalArgumentException(
                    "Өгөгдлийн хэмжээ 1–" + MAX_RANDOM_SIZE + " байх ёстой.");
        }
        return n;
    }

    public static int parseLeafDelay(String text) {
        int d = parseInt(text, "Leaf саатал");
        if (d < 0 || d > MAX_LEAF_DELAY_MS) {
            throw new IllegalArgumentException(
                    "Leaf саатал 0–" + MAX_LEAF_DELAY_MS + " ms байх ёстой.");
        }
        return d;
    }

    private static int parseInt(String text, String field) {
        String s = text == null ? "" : text.trim();
        if (s.isEmpty()) {
            throw new IllegalArgumentException(field + " хоосон байна.");
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    field + "-д бүхэл тоо оруулна уу («" + shorten(s) + "»).");
        }
    }

    private static String shorten(String s) {
        return s.length() <= 20 ? s : s.substring(0, 20) + "…";
    }
}
