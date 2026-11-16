# II-4-7 — Reviewer Comments and Responses (БД2)

Эх сурвалж:
- **R** — IV-2-10 багийн албан ёсны шүүмж `Review_II_4_7.pdf` (2026-10-28, шалгасан хувилбар 831c55c).
- **T** — багшийн feedback: *"Хэзээ хаана fork, join хийгдэж байгааг нь эвтэйхэн харуулдаг байхаар сайжруулмаар юм
  уу. Манай хичээл чинь Үйлдлийн систем болохоор процесс, урсгал нь үндсэн агуулга юм."*

Priority-г `OS_Peer_Review_Guide.pdf`-ийн 7-р хэсгийн дагуу ангилсан (Critical / Major / Minor / Suggestion).

> `[commit]` гэсэн газарт BD2-ийн эцсийн commit hash / `v2.0.0` tag-ийг push хийсний дараа бичнэ.

## 1. Reviewer Comments and Responses

| № | Шүүмж (эх утга) | Priority | Шийдвэр | Өөрчлөлт ба нотолгоо |
|---|---|---|---|---|
| 1 | **T**: Хэзээ, хаана fork, join хийгдэж байгааг эвтэйхэн харуулах; процесс, урсгал бол үндсэн агуулга. | Major | Зөвшөөрсөн | (a) `invokeAll`-ийг сурах бичгийн `second.fork(); first.compute(); second.join();` хэлбэрээр сольж fork/join цэгийг кодонд тодорхой болгосон (`MergeSortTask`, `QuickSortTask`). (b) `ExecutionLog`: task бүрийн thread, START/FORK/JOIN WAIT/JOIN/END хугацааг `System.nanoTime`-аар бүртгэнэ. (c) Шинэ **Thread timeline** (`ThreadTimeline.java`): worker бүр нэг мөр, fork→steal ба join←буцалтын сум. (d) Модон дээр FORK/JOIN алхам, `fork()`/`compute()`/`join()` ирмэг, `T3·W2` тэмдэг. (e) Worker тоо (1/2/4/8/Auto) ба leaf саатлын тохиргоо. **Нотолгоо:** зураг 1–6; тест N5–N7. |
| 2 | **R-A1**: UI thread эрэмбэлэлтийг хүлээнэ (`pool.invoke()` handler дотор, Main.java:349–359). | Major | Зөвшөөрсөн | Эрэмбэлэлт ба trace үүсгэлт `javafx.concurrent.Task`-аар `sort-runner` daemon thread дээр явна. Ажиллаж байх үед оролтын карт идэвхгүй, товч «Эрэмбэлж байна…», `ProgressIndicator` харагдана, `running` туг давхар эхлүүлэхээс хамгаална. Алдааг `onFailed`-д барина. **Нотолгоо:** `Main.sortNumbers()`; 5000 элемент, leaf 20 ms үед UI хариу өгсөөр (зураг 7). |
| 3 | **R-A2**: «Мод нь бодит fork-join-той яг таарч байна» гэдэг мэдэгдэл хэт ерөнхий; task ID, thread ID, эхлэх/дуусах үйл явдлыг бүртгэх. | Minor | Зөвшөөрсөн | Мэдэгдлийг «Эцсийн үр дүн ба модны бүтэц бодит task-уудтай таарч байна. Модны анимаци нь дарааллыг тайлбарлах зориулалттай; бодит thread, хугацааг timeline харуулна» болгосон. Task ID (preorder T1…), worker нэр (W1…), START/FORK/COMPUTE/LEAF/JOIN WAIT/JOIN/END үйл явдлыг «Fork/Join үйл явдлын бүртгэл» табд харуулна. **Нотолгоо:** зураг 4; тест N5 (START = END = task-ийн тоо, FORK = JOIN, хугацааны дараалал). |
| 4 | **R-A3**: Manual-ийн `javac` команд гурван мөрөнд тасарсан. Windows хувилбар, тайлан/Manual/README-ийн командыг ижил болгох. | Minor | Зөвшөөрсөн | Үндсэн команд нь `mvn clean test`. Maven-гүй хувилбарыг macOS/Linux ба Windows PowerShell-д тус бүр **нэг мөрөөр** бичсэн, README ба Manual-д ижил. **Нотолгоо:** README-ийн командыг хуулж ажиллуулахад `passed=11946 failed=0`. |
| 5 | **R-A4**: Тест нь тусдаа `main()`, `mvn test` ажиллуулдаггүй. CI нэмэх, Quick Sort threshold=0 ба хоосон массивын (T06) тест, API ба GUI-ийн хоосон оролтыг ялгах. | Suggestion | Зөвшөөрсөн | JUnit 5.10.2 + Surefire 3.5.4: `SortSuiteTest` нь `mvn test`-ээр бүх шалгалтыг ажиллуулна. `.github/workflows/ci.yml` push/PR бүрт `mvn -B clean test`. Шинэ тест: N1 хоосон массив (API алдаагүй), N2 Quick Sort threshold 0/−1, null, буруу муж, N4 GUI-ийн хоосон текст = алдаа (`InputParser`). **Нотолгоо:** `Tests run: 1, Failures: 0`, `passed=11946`. |
| 6 | **R-A4 (хэсэг)**: Багийн гишүүдийн үүрэг, бодит commit/file холбоос. | Minor | Хэсэгчлэн зөвшөөрсөн | Final Report-ийн «Гишүүдийн ажил хариуцлага» хүснэгтийг гишүүн бүрийн бодит оролцоогоор шинэчилнэ (баг бөглөнө). Кодын commit түүх нэг хүний account-оос хийгдсэн тул бусад гишүүдийн оролцоог (тест, баримт бичиг, постер, шүүмжийн дүн шинжилгээ) тайланд тодорхой бичнэ. |
| 7 | **R-A5**: `target/maven-status/...` build файлууд repository-д байна. | Minor | Зөвшөөрсөн | `git rm --cached -r target`; `target/` аль хэдийн `.gitignore`-д байсан. Курсын PDF-үүдийг (`/*.pdf`) ignore хийсэн. **Нотолгоо:** `git ls-files target` хоосон. |
| 8 | **Өөрсдийн олсон**: task бүр дэд массивыг `Arrays.toString`-оор хоёр удаа хадгалдаг, хаана ч уншдаггүй (1 сая элементэд O(n log n) нэмэлт). | Major | Зөвшөөрсөн | Зөвхөн ≤ 32 элементтэй хэсэгт хадгална (`SortNode.describe`). **Нотолгоо:** 1 000 000 элемент, 8 worker: 2.3–3.7× хурдан (`docs/benchmark-results.md`, хүснэгт 3). |
| 9 | **R (дүрслэл)**: «Бодит thread-үүдийн гүйцэтгэлийн дараалал, зэрэгцээ ажилласан хугацааг тусгайлан хэмждэггүй.» | Major | Зөвшөөрсөн | №1-тэй хамт шийдсэн: timeline ба `Benchmark.java` (worker 1/2/4/8 хурдсалт, threshold-ийн нөлөө). |

Зөвшөөрөөгүй санал байхгүй. Хязгаарлалт (хамрах хүрээний шалтгаан):
- Модны **алхамчилсан анимаци** нь нэг thread-ээр тооцоолсон тайлбарын дараалал хэвээр (тодорхой, давтагдах дараалал
  хэрэгтэй тул). Бодит зэрэгцээ дарааллыг timeline тусад нь харуулна, хоёрыг `T·W` тэмдэг ба сонголтоор холбосон.
- Leaf саатал (`Thread.sleep`) нь жижиг өгөгдөл дээр зэрэгцээ ажиллагааг нүдэнд харагдуулах зорилготой. Бодит хугацааг
  хэмжихдээ 0 болгоно, 32-оос олон элементэд автоматаар 0.

## 2. Дахин туршилт (regression ба шинэ тест)

| Тест | Засварын өмнө (BD1, 831c55c) | Засварын дараа (BD2) | Дүгнэлт |
|---|---|---|---|
| BD1 regression (номын жишээ, захын тохиолдол, 1500 санамсаргүй, 10 000 элемент, String) | passed=9111 failed=0 | passed=9111 failed=0 | Үндсэн функц хадгалагдсан |
| N1 хоосон массив (T06) | тест байхгүй | 6/6 PASS | API алдаагүй боловсруулна |
| N2 буруу параметр (Quick threshold 0, −1, null, муж) | Quick threshold=0 тест байхгүй | 8/8 PASS | Татгалзана |
| N3 Int хязгаар, шүүмжлэгчийн `3, -1, 3, 0` | — | PASS | Overflow-гүй |
| N4 GUI оролт (`InputParser`) | UI-д л шалгагддаг | 18/18 PASS | Хоосон, `1,,2`, `2.5`, overflow, threshold 0 |
| N5 fork/join бүртгэл (200 санамсаргүй × 2 алгоритм, 1–4 worker) | — | PASS | START=END=task, FORK=JOIN, хугацааны дараалал, trace-ийн FORK/JOIN алхам = бодит fork/join |
| N6 parallelism = 1 | — | PASS | Бүх task нэг thread, steal 0 |
| N7 4 worker, leaf 40 ms | — | PASS (15/15 давтан ажиллуулалт) | Leaf-үүд зэрэг ажилласан, нийт < дараалсан |
| `mvn test` | ажилладаггүй | BUILD SUCCESS | CI-д ажиллана |
| **Нийт** | **9111 / 0** | **11946 / 0** | |
| 1 сая элемент, 8 worker, threshold 1000 (Merge / Quick) | 604.7 / 695.9 ms | 241.6 / 187.5 ms | 2.5× / 3.7× хурдан |
| GUI 8 тоо, threshold 2, саатал 20 ms: 1 worker vs Auto | — | 98.5 ms vs 28.2 ms | Зэрэгцээ ажиллагаа timeline дээр харагдана |

## 3. Хавсралт A. Сайжруулалтын бүртгэл

| № | Шүүмж | Priority | Шийдвэр | Хийсэн өөрчлөлт | Нотолгоо |
|---|---|---|---|---|---|
| 1 | Fork/join, thread харагдах (багш) | Major | Зөвшөөрсөн | ExecutionLog, ThreadTimeline, fork/join ирмэг, T·W | Зураг 1–6, N5–N7 |
| 2 | A1 UI thread | Major | Зөвшөөрсөн | javafx Task, давхар эхлүүлэхээс хамгаалах | Main.sortNumbers, зураг 7 |
| 3 | A2 Мэдэгдэл, бүртгэл | Minor | Зөвшөөрсөн | Мэдэгдэл засав, үйл явдлын бүртгэл | Зураг 4, N5 |
| 4 | A3 Команд | Minor | Зөвшөөрсөн | Нэг мөр, Windows | README, Manual |
| 5 | A4 Тест/CI | Suggestion | Зөвшөөрсөн | JUnit, CI, N1–N7 | `mvn test` |
| 6 | A5 target/ | Minor | Зөвшөөрсөн | git rm --cached | `git ls-files target` |
| 7 | Гишүүдийн оролцоо | Minor | Хэсэгчлэн | Тайланд бөглөнө | Final Report |
| 8 | toString overhead (өөрсдөө) | Major | Зөвшөөрсөн | SortNode.describe | Benchmark хүснэгт 3 |

## Зургийн жагсаалт (`docs/screenshots/bd2/`, бодит програмаас авсан)

1. `01_fork_step_merge.png`: Merge Sort FORK алхам: `compute()` / `fork()` ирмэг, 4 worker зэрэг (timeline).
2. `02_join_step_merge.png`: JOIN алхам: `↑ join()` ирмэг, сонгосон task timeline дээр тодорсон.
3. `03_merge_done.png`: эцсийн үр дүн, бүх ирмэг join-оор буцсан.
4. `04_event_log.png`: Fork/Join үйл явдлын бүртгэл (task ID, thread, хугацаа).
5. `05_quick_work_stealing.png`: Quick Sort, work stealing: W1-ийн fork хийсэн T3-г W2, W2-ийн fork хийсэн T6 (leaf)-г W3
   хулгайлж (steal) ажиллуулсан. Энэ хооронд W1, W2 нь `join()` дээр хүлээж байна (саарал зурвас).
6. `06_one_worker.png`: Worker = 1: бүх task W1 дээр дараалан (98.5 ms).
7. `07_large_input_timeline.png`: 5000 элемент, Quick Sort: 8 worker, 1550 task, 596 fork/join, 54 steal.
