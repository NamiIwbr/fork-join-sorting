# II-4-7 Fork-Join Sorting Application — Manual (Final, БД2)

F.CSM302 Үйлдлийн систем · Сэдэв 7 · Хувилбар 2.0.0

---

## 8.1 Хэрэглэгчийн заавар

### Програмын зорилго

Merge Sort ба Quick Sort-ийг Java Fork-Join (`ForkJoinPool`, `RecursiveAction`) загвараар зэрэгцээ эрэмбэлж:
- **recursion tree** дээр массив хэрхэн хуваагдаж (FORK), жижиг хэсэг insertion sort-оор эрэмбэлэгдэж (LEAF),
  буцаж нэгтгэгдэхийг (JOIN, MERGE) алхам алхмаар харуулах,
- **thread timeline** дээр бодит worker thread бүр аль task-ийг хэзээ гүйцэтгэж, fork хийгдсэн task-ийг өөр worker
  хэрхэн хулгайлж (work stealing), join дээр хэр удаан хүлээснийг бодит хугацаагаар харуулна.

### Дэмжих орчин

| Зүйл | Шаардлага |
|---|---|
| OS | macOS, Windows, Linux (macOS 14 / Apple M1 дээр шалгасан) |
| JDK | 23 буюу түүнээс дээш (`pom.xml`: `maven.compiler.release=23`) |
| Apache Maven | 3.9+ |
| JavaFX | 23.0.2 (Maven dependency, автоматаар татна) |
| Интернэт | Эхний build-д dependency татахад хэрэгтэй |

### Суулгах / нээх алхам

1. Repository-г clone хийнэ (8.2-г үз) эсвэл ZIP-ийг задална.
2. `pom.xml` байгаа root хавтас руу Terminal-аар орно.
3. `java -version`, `mvn -version`-оор орчныг шалгана.
4. `mvn clean javafx:run` ажиллуулна. Цонх нээгдэнэ.

### Оролт ба тохиргоо

| Талбар | Тайлбар | Зөв утга |
|---|---|---|
| Тоонууд | Таслалаар тусгаарласан бүхэл тоо | `8, 3, 6, 1`; −2147483648…2147483647 |
| Threshold | Хэсгийн урт ≤ threshold үед fork хийхгүй, insertion sort хийнэ | ≥ 1 |
| Алгоритм | Merge Sort / Quick Sort | |
| Worker thread | `ForkJoinPool`-ийн parallelism | Auto (CPU цөм), 1, 2, 4, 8 |
| Leaf саатал | Leaf task бүрт нэмэлтээр хийсэн зориудын саатал. Жижиг өгөгдөл дээр зэрэгцээ ажиллагааг нүдэнд харуулна. Бодит хугацааг хэмжих бол 0 | 0–500 ms (> 32 тоонд автоматаар 0) |
| Санамсаргүй тоо | «Үүсгэх» дарахад −1000…1000 мужид санамсаргүй тоо үүсгэнэ | 1–10000 |
| Жишээ | Бэлэн өгөгдөл (номын жишээ, эрэмбэлэгдсэн, урвуу, бүгд адил, давхардсан, сөрөг, Int-ийн хязгаар, нэг тоо) | |

Анхны (default) тохиргоо: `8, 3, 6, 1, 9, 2, 7, 4`, threshold 2, Merge Sort, Worker Auto, саатал 20 ms.

### Ажиллуулах, зогсоох, reset хийх

| Удирдлага | Үүрэг |
|---|---|
| Эрэмбэлэх | Оролтыг шалгаад эрэмбэлэлтийг **background thread** дээр эхлүүлнэ. Ажиллаж байхад товч «Эрэмбэлж байна…» болж, оролт идэвхгүй байна (давхар эхлүүлэхгүй) |
| Тоглуулах / Зогсоох | Модны алхамчилсан анимацийг эхлүүлэх / түр зогсоох |
| Өмнөх / Дараах | Нэг алхам ухрах / урагшлах |
| Эхнээс | Эхний алхамд буцаах |
| Хурд | Анимацийн хурд |
| Гүйлгэгч | Алхамыг шууд сонгох |
| Thread timeline дээр дарах | Task-ийн thread, эхэлсэн/дууссан хугацаа, fork/steal, join хүлээсэн хугацааг харуулна |
| Fork/Join үйл явдлын бүртгэл (таб) | Бүх START / FORK / COMPUTE / LEAF / JOIN WAIT / JOIN / END үйл явдлыг хугацаагаар |
| Reset | Бүх талбар, мод, timeline-ийг анхны байдалд |

### Дүрслэлийн тэмдэг, өнгө, төлөв

Recursion tree:

| Тэмдэг | Утга |
|---|---|
| Цэнхэр хүрээ | Одоо ажиллаж буй зангилаа |
| Ногоон | Дууссан / эрэмбэлэгдсэн хэсэг |
| Шар нүд | Харьцуулж / сольж буй элемент |
| Нил ягаан нүд | Quick Sort-ийн pivot |
| Тасархай нил ягаан ирмэг `fork()` | Эцэг task энэ хэсгийг `fork()` хийж pool-ийн queue-д хийсэн |
| Цэнхэр ирмэг `compute()` | Эцэг task энэ хэсгийг өөрийн thread дээр шууд гүйцэтгэнэ |
| Ногоон ирмэг `↑ join()` / `↑ буцсан` | Дэд task дуусч эцэгтээ буцсан |
| `T3·W2` тэмдэг | Task-ийн дугаар · түүнийг **бодитоор** гүйцэтгэсэн worker thread (өнгө нь worker-ийн өнгө) |
| Дээд баруун талын шошго | Алхмын төрөл: FORK, COMPUTE, PARTITION, LEAF, JOIN, MERGE, DONE |

Thread timeline (хэвтээ тэнхлэг = pool эхэлснээс хойших ms, мөр бүр = нэг worker):

| Өнгө | Утга |
|---|---|
| Цэнхэр | Хуваах / partition (task эхэлснээс fork хүртэл) |
| Цайвар ногоон-цэнхэр | Leaf: insertion sort (+ саатал) |
| Саарал нимгэн зурвас | `join()` дээр хүлээж байна (энэ хооронд тэр thread өөр task хулгайлж ажиллуулж болно) |
| Улбар шар | Join-ийн дараах нэгтгэл (merge) |
| Нил ягаан тасархай сум | Fork хийгдсэн task-ийг өөр worker хулгайлсан (steal) |
| Ногоон сум | Хулгайлагдсан task дуусч эцэг рүүгээ join-оор буцсан |
| ▼ тэмдэг | Steal болоогүй fork (нил ягаан) / join (ногоон) цэг |

Модны анимаци нь дарааллыг ойлгуулах л зориулалттай. Бодит зэрэгцээ гүйцэтгэлийн дараалал, хугацааг timeline ба
`T·W` тэмдэг харуулна.

### Жишээнүүд

**Жишээ 1. Merge Sort, fork/join-ийг ажиглах**
- Тоонууд `8, 3, 6, 1, 9, 2, 7, 4`; threshold 2; Merge Sort; Worker Auto; саатал 20 ms.
- Хүлээгдэх үр дүн: `[1, 2, 3, 4, 6, 7, 8, 9]`, 7 task, 3 fork, 3 join.
- Ажиглах: 2-р алхамд (FORK) `[0–7]` → `[0–3]` `compute()`, `[4–7]` `fork()`. Timeline дээр 4 leaf (T3, T4, T6, T7)
  ~20 ms-ийн турш **өөр өөр worker дээр зэрэг** ажиллана.

**Жишээ 2. Worker = 1 ба Auto-г харьцуулах**
- Жишээ 1-ийн тохиргоог Worker = 1 болгоно.
- Хүлээгдэх: үр дүн ижил. Timeline-д ганц W1 мөр, 4 leaf дараалан, нийт ~4 × 20 ms (≈ 98 ms), steal 0.
  Auto үед ≈ 28 ms. (Тоо машинаас хамаарч бага зэрэг өөр гарна.)

**Жишээ 3. Quick Sort, давхардсан утга ба work stealing**
- Тоонууд `4, 2, 4, 1, 2, 4, 1, 3, 3, 2`; threshold 2; Quick Sort.
- Хүлээгдэх үр дүн: `[1, 1, 2, 2, 2, 3, 3, 4, 4, 4]`.
- Ажиглах: PARTITION алхамд pivot (нил ягаан) өнгөтэй байх ба `<`, `=`, `>`нь хуваалтыг дүрсэлнэ. Ганц дэд хэсэг үлдвэл fork хийхгүй. Timeline дээр join хүлээж буй thread өөр task хулгайлна.

**Жишээ 4. Сөрөг ба тэг утга:** `-5, 3, 0, -9, 8, -1, 0, 7`, threshold 2 → `[-9, -5, -1, 0, 0, 3, 7, 8]`.

**Жишээ 5. Том өгөгдөл:** Санамсаргүй тоо 5000 → Үүсгэх → Эрэмбэлэх. Мод зурагдахгүй (> 32), харин timeline-д
бүх worker, хэдэн зуун fork/join, steal харагдана.

### Түгээмэл алдаа ба шийдэл

| Алдаа / нөхцөл | Програмын хариу | Шийдэл |
|---|---|---|
| Хоосон оролт | «Тоонуудын жагсаалт хоосон байна» | Нэг буюу хэд хэдэн бүхэл тоо оруулна |
| `1,,2` эсвэл `1,2,` | «Тоонуудын хооронд илүү таслал байна (N-р байрлал)» | Нэг таслал |
| `2.5`, `a` | «… нь бүхэл тоо биш…» | Бүхэл тоо |
| `2147483648` | «… мужаас гарсан байна» | −2147483648…2147483647 |
| Threshold 0, сөрөг, хоосон | «Threshold нь 1 буюу түүнээс их…» | ≥ 1 |
| Leaf саатал > 500 | «Leaf саатал 0–500 ms…» | 0–500 |
| > 32 тоо | Мод оронд тайлбар, timeline харагдана | Хэвийн үйлдэл |
| `java`/`mvn` олдохгүй | Terminal алдаа | JDK 23, Maven суулгаж PATH-ийг шалгана |
| `release 23 not supported` | Maven compile алдаа | JDK 23+ ашиглана (`java -version`) |

---

## 8.2 Operational заавар

### Repository clone хийх

```bash
git clone https://github.com/NamiIwbr/fork-join-sorting.git
cd fork-join-sorting
git checkout v2.0.0
```

### OS, compiler / runtime, хувилбар

```bash
java -version            # 23 буюу түүнээс дээш
mvn -version             # 3.9+
```

Шалгасан орчин: macOS 14, Apple M1 (aarch64, 8 цөм), Oracle JDK 23.0.1, Apache Maven 3.10.0.
CI: GitHub Actions, Ubuntu, Temurin JDK 23 (`.github/workflows/ci.yml`).

### Dependency (pom.xml)

| Dependency | Хувилбар | Scope |
|---|---|---|
| `org.openjfx:javafx-controls` | 23.0.2 | compile |
| `org.junit.jupiter:junit-jupiter` | 5.10.2 | test |
| `maven-surefire-plugin` | 3.5.4 | plugin |
| `javafx-maven-plugin` | 0.0.8 | plugin |

Maven бүгдийг автоматаар татна. Тусад нь суулгах шаардлагагүй.

### Build ба Run

```bash
mvn clean compile
mvn clean javafx:run
```

### Тест ажиллуулах

```bash
mvn clean test
```

Хүлээгдэх үр дүн:
```
SortTests: passed=11946 failed=0
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Maven-гүйгээр (JavaFX шаардлагагүй), **нэг мөрөөр** хуулж ажиллуулна:

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

### Үндсэн тестийн өгөгдөл

| Бүлэг | Өгөгдөл |
|---|---|
| Номын жишээ | `7, 12, 19, 3, 18, 4, 2, 6, 15, 8`, threshold 1–6 |
| Захын тохиолдол | 1 элемент, 2 элемент, **хоосон массив** |
| Утгын хэлбэр | Давхардсан, бүгд ижил, сөрөг/тэг, `Integer.MIN_VALUE` / `MAX_VALUE` |
| Эрэмбийн хэлбэр | Өсөх/буурах 32 элемент |
| Санамсаргүй | 1500 массив (seed 42), урт 1–32, threshold 1–6 |
| Том | 10 000 элемент: санамсаргүй, өсөх, буурах, бүгд ижил, цөөн ялгаатай; threshold 1, 2, 100 |
| Төрөл | `String` массив |
| Thread / fork-join | 200 санамсаргүй массив (seed 7) × 2 алгоритм, 1–4 worker; parallelism 1; 4 worker + 40 ms leaf |

### Нэмэлт boundary / error тест

| Оролт | Хүлээгдэх үр дүн |
|---|---|
| `42`, threshold 2 | `[42]` |
| `2, 1`, threshold 1 | `[1, 2]` |
| `3, -1, 3, 0`, threshold 1 | `[-1, 0, 3, 3]` |
| `2147483647, 0, -2147483648` | `[-2147483648, 0, 2147483647]` |
| Хоосон, `1,,2`, `2.5`, threshold 0 | Алдааны цонх |
| API: хоосон массив, Quick/Merge | Алдаагүй, 1 task |
| API: Quick Sort threshold 0 | `IllegalArgumentException` |

### Гүйцэтгэлийн хэмжилт

```bash
mvn -q test-compile && java -Xmx4g -cp target/classes:target/test-classes Benchmark
```
Windows: `-cp "target/classes;target/test-classes"`. Үр дүн ба тайлбар: `docs/benchmark-results.md`.

### Known issues / анхаарах зүйл

- Модны алхамчилсан дүрслэл ≤ 32 элементэд л ажиллана (дэлгэцэнд багтах). Timeline харин ямар ч үед ажиллана.
- Timeline дээрх хугацаа нь `System.nanoTime` бөгөөд OS-ийн thread scheduling-ээс хамаарч ажиллуулах бүрт өөр байна.
  Жижиг өгөгдөлд саатал 0 үед task-ууд микросекундэд дуусч нэг-хоёр worker л ашиглагдаж болно. Энэ нь хэвийн
  fork-ийн зардал ажлаас их.
- Leaf саатлыг зориудаар нэмсэн `Thread.sleep` тул нийт хугацаанд орно. Статистикийн мөрөнд тэмдэглэгдэнэ.
- Event log ≤ 20 000 мөр харуулна.

### Нөөц ба эх сурвалж

- Repository: https://github.com/NamiIwbr/fork-join-sorting
- A. Silberschatz, P. B. Galvin, G. Gagne, *Operating System Concepts*, 10th ed., Wiley, 2018: Ch. 4, Fork-Join, Programming Projects.
- Java SE 23 API: `java.util.concurrent.ForkJoinPool`, `RecursiveAction`
- OpenJFX 23 documentation; Apache Maven; JUnit 5 User Guide
