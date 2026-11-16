import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * `mvn test` үед SortTests-ийн бүх шалгалтыг JUnit 5-аар ажиллуулна
 * (BD2, шүүмж A4). SortTests нь JUnit-гүйгээр `java SortTests` хэлбэрээр
 * ч ажилладаг хэвээр.
 */
class SortSuiteTest {

    @Test
    void allAlgorithmChecksPass() {
        SortTests.Summary s = SortTests.runAll(false);
        System.out.println("SortTests: passed=" + s.passed()
                + " failed=" + s.failed());
        assertTrue(s.passed() > 9111, "BD1 regression + BD2 тестүүд ажилласан");
        assertEquals(0, s.failed(), s.failures());
    }
}
