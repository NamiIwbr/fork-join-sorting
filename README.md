# Fork-Join Sorting — II-4-7 • Final (БД2)

F.CSM302 Үйлдлийн систем - Сэдэв 7: Fork-Join Sorting Application · Хуваарилалтын код **II-4-7**

Merge Sort ба Quick Sort-ийг Java-ийн `ForkJoinPool` / `RecursiveAction` ашиглан зэрэгцээ эрэмбэлж,
**хэзээ, хаана fork хийгдэж, хэзээ join-оор буцаж нэгтгэгдэхийг** recursion tree болон
**бодит worker thread-үүдийн timeline** дээр харуулна.

[FORK алхам ба бодит thread timeline](docs/screenshots/bd2/01_fork_step_merge.png)

## БД2-т юу өөрчлөгдсөн бэ?

| Сайжруулалт | Шалтгаан |
|---|---|
| **Бодит thread timeline**: worker бүр (W1, W2, …) аль task-ийг хэзээ ажиллуулсан, fork → steal, join ← буцалт | Багшийн санал: процесс, thread бол үндсэн агуулга |
| `fork()` / `compute()` / `join()` ирмэг, FORK/JOIN алхмын тэмдэг, зангилаа бүрт `T3·W2` (task · worker) | Багшийн санал; шүүмж A2 |
| Fork/Join бүртгэл (task ID, thread, START/FORK/JOIN/END, хугацаа) | Шүүмж A2 |
| Эрэмбэлэлт background thread дээр (`javafx.concurrent.Task`), ажиллаж байхад товч идэвхгүй | Шүүмж A1 |
| Worker-ийн тоо (1/2/4/8/Auto), leaf саатал тохиргоо | Туршилт: юуг өөрчилж, юу ажиглах |
| `mvn test` (JUnit 5) + GitHub Actions CI, хоосон массив, Quick Sort threshold=0, parser, thread тест | Шүүмж A4 |
| Manual/README-ийн команд нэг мөрөнд, Windows хувилбартай | Шүүмж A3 |
| `target/` repository-оос хасагдсан | Шүүмж A5 |
| 1 сая элемент дээр BD1-ээс ~2.3–3.7× хурдан (зангилаа бүрт `Arrays.toString` хийдэг байсныг зассан) | Гүйцэтгэлийн хэмжилт |

Дэлгэрэнгүй: [docs/BD2_Reviewer_Responses.md](docs/BD2_Reviewer_Responses.md)

## Quick Start

Шаардлага (шалгасан хувилбар):

| Хэрэгсэл | Хувилбар |
|---|---|
| JDK | 23 (23.0.1 дээр шалгасан; `maven.compiler.release=23`) |
| Apache Maven | 3.9+ (3.10.0 дээр шалгасан) |
| JavaFX | 23.0.2 (Maven автоматаар татна) |
| JUnit | 5.10.2 (зөвхөн тест, Maven автоматаар татна) |

```bash
git clone https://github.com/NamiIwbr/fork-join-sorting.git
cd fork-join-sorting
mvn clean javafx:run
mvn clean test
```

Эхний удаа Maven dependency татахад интернэт шаардлагатай.

### Програмыг ашиглах

1. Тоонуудыг таслалаар бичнэ (эсвэл **Жишээ**, **Үүсгэх** ашиглана).
2. **Threshold**, **Алгоритм**, **Worker thread** (Auto/1/2/4/8), **Leaf саатал** (ms)-ийг сонгоно.
3. **Эрэмбэлэх** дарна. Эрэмбэлэлт background thread дээр ажиллана.
4. Дээд хэсэг: recursion tree алхамчлан зурна. **FORK** алхамд ирмэг дээр `fork()` (тасархай нил ягаан) ба
   `compute()` (цэнхэр) гарна, **JOIN** алхамд `join()` (ногоон) болно.
5. Доод хэсэг: **Thread timeline**. Мөр бүр нэг worker thread, хайрцаг бүр нэг task. Хайрцаг дээр дарж
   дэлгэрэнгүйг харна. **Fork/Join үйл явдлын бүртгэл** табд бүх START/FORK/JOIN/END үйл явдал хугацаатайгаа харагдана.
6. **Reset** анхны тохиргоог сэргээнэ.

Санал болгох туршилт: ижил `8, 3, 6, 1, 9, 2, 7, 4`, threshold 2, саатал 20 ms-ийг **Worker = 1** ба **Auto** гэж
ажиллуулж timeline-ийг харьцуулна (1 worker: ~98 ms, бүх task W1 дээр; Auto: ~28 ms, 4 leaf зэрэг).

Анхаарах зүйл:
- Модны алхамчилсан дүрслэл ≤ 32 тоонд ажиллана. Түүнээс их үед thread timeline, бүртгэл, үр дүн гарна.
- Leaf саатал нь зөвхөн дүрслэлд зориулж зориудаар хийсэн `Thread.sleep` бөгөөд 32-оос олон тоонд автоматаар 0 болно.
- Модны анимаци нь дарааллыг тайлбарлах зориулалттай. Бодит зэрэгцээ гүйцэтгэлийн дараалал, хугацааг
  **timeline** ба зангилааны `T·W` тэмдэг харуулна.

## Тест

| Команд | Үр дүн |
|---|---|
| `mvn clean test` | `Tests run: 1, Failures: 0` · `SortTests: passed=11946 failed=0` |

Maven-гүйгээр (JavaFX шаардлагагүй):

macOS / Linux:
```bash
mkdir -p out && javac -encoding UTF-8 -d out src/SortNode.java src/ExecutionLog.java src/SortRunner.java src/InputParser.java src/MergeSortTask.java src/QuickSortTask.java src/SortTrace.java tests/SortTests.java && java -cp out SortTests
```

Windows (PowerShell):
```powershell
mkdir out -Force; javac -encoding UTF-8 -d out src/SortNode.java src/ExecutionLog.java src/SortRunner.java src/InputParser.java src/MergeSortTask.java src/QuickSortTask.java src/SortTrace.java tests/SortTests.java; java -cp out SortTests
```

Хүлээгдэх гаралт:
```
regression(BD1): passed=9111 failed=0
new(BD2):        passed=2835 failed=0
passed=11946 failed=0
```

Тестийн өгөгдөл: `tests/SortTests.java` (номын жишээ `7, 12, 19, 3, 18, 4, 2, 6, 15, 8`, захын тохиолдол, seed 42/7/1-тэй
санамсаргүй өгөгдөл, 10 000 элемент, String, хоосон массив, Int-ийн хязгаар, буруу оролт, thread/fork/join бүртгэл).

### Гүйцэтгэлийн хэмжилт

```bash
mvn -q test-compile && java -Xmx4g -cp target/classes:target/test-classes Benchmark
```
(Windows-д `-cp target/classes;target/test-classes`.) Үр дүн: [docs/benchmark-results.md](docs/benchmark-results.md)

## Бүтэц

```
src/
  Main.java            JavaFX UI, оролт, background Task
  MergeSortTask.java   RecursiveAction: fork() / compute() / join(), merge
  QuickSortTask.java   RecursiveAction: median-of-3, 3-way partition, гүний хамгаалалт
  SortRunner.java      Нэрлэсэн worker (W1..Wn)-тэй ForkJoinPool үүсгэж нэг удаа ажиллуулна
  ExecutionLog.java    Бодит START/FORK/JOIN/END үйл явдлын бүртгэл (thread, nanoTime)
  SortNode.java        Бодит task-ийн мод: муж, thread, хугацаа, fork/steal
  SortTrace.java       Алхамчилсан тайлбарын trace (FORK, PARTITION, LEAF, JOIN, MERGE)
  TreeCanvas.java      Recursion tree, fork/join ирмэг, алхмын удирдлага
  ThreadTimeline.java  Worker thread-ийн timeline ба үйл явдлын бүртгэл
  InputParser.java     GUI оролтын шалгалт
  Theme.java           CSS
tests/
  SortTests.java       Бүх шалгалт (main() эсвэл JUnit-ээр)
  SortSuiteTest.java   `mvn test`-ийн JUnit 5 оролт
  Benchmark.java       Гүйцэтгэлийн хэмжилт
docs/                  Manual, шүүмжийн хариу, хэмжилт, screenshot, тайлан
```

## Ашигласан технологи ба AI

- Java 23, Fork-Join Framework (`ForkJoinPool`, `RecursiveAction`), JavaFX 23.0.2, Apache Maven, JUnit 5.10.2.
- AI: BD1-д ChatGPT-ийг кодын бүтэц, алгоритм, алдаа засах тал дээр зөвлөгөө авах гэж ашигласан. BD2-т Claude-ийг
  шүүмжийн дагуух сайжруулалтад туслуулж ашигласан. Бүх өөрчлөлтийг өөрөө шалгаж, баталгаажуулсан.
