# Fork-Join Sorting

F.CSM302 Үйлдлийн систем - Бие даалт 1

Сэдэв 7: Fork-Join Sorting Application
Багийн код: II-4-7

Merge Sort болон Quick Sort-ийг Java-ийн fork-join параллелизмаар хэрэгжүүлж, fork, дэд ажил, join-г recursion tree хэлбэрээр алхам алхмаар харуулна.

## Зорилго

- Сурах бичгийн "Multithreaded Sorting Application" төслийг `ForkJoinPool` болон `RecursiveAction` ашиглан хэрэгжүүлэх.
- Хоёр хувилбартай байх: `Merge Sort` (хоёр тэнцүү хагас) ба `Quick Sort` (pivot-оор зүүн, баруун хэсэг болгох).
- Дэд жагсаалт threshold-оос бага болмогц insertion sort-оор шууд эрэмбэлэх.
- Эрэмбэлэх объект нь `Comparable` байх.
- Хэрэглэгч өгөгдөл, threshold, алгоритмыг өөрчилж дахин туршиж, эрэмбэлэлтийг recursion tree дээр харах.

## Quick Start

Шаардлага
- JDK 23 эсвэл түүнээс дээш (`java -version`-р шалгана)
- Apache Maven
- JavaFX 23.0.2 (Maven dependency-ээр татагдана)

Ажиллуулах (Maven)
    mvn clean javafx:run

Ажиллуулах (JavaFX SDK-ээр, Maven-гүйгээр)
    JavaFX SDK-г татаж задлаад `lib` хавтасны замыг `PATH_TO_FX` гэж тохируулна.

    Mac дээр эхлээд хамгаалалтыг арилгана:
    xattr -dr com.apple.quarantine ~/javafx-sdk-21.0.2
    export PATH_TO_FX=~/javafx-sdk-21.0.2/lib

    Compile хийх:
    mkdir -p out
    javac -encoding UTF-8 --module-path $PATH_TO_FX --add-modules javafx.controls -d out src/*.java

    Ажиллуулах:
    java --module-path $PATH_TO_FX --add-modules javafx.controls -cp out Main
    IntelliJ IDEA дээр: Run Configuration-ийн Main class-д `Main`, VM options-д
    `--module-path <PATH_TO_FX> --add-modules javafx.controls` гэж нэмнэ.

Ашиглах
1. Тоонуудыг талбарт таслалаар тусгаарлан бичнэ (эсвэл Жишээ сонгох, Үүсгэх товчийг ашиглаж болно).
2. Threshold болон Алгоритм (Merge Sort / Quick Sort)-ийг сонгоно.
3. Эрэмбэлэх дарна. Баруун талд recursion tree гарч, эрэмбэлэлт автоматаар хийгдэнэ.
4. Зогсоох, Өмнөх / Дараах, Хурд, доорх гүйлгэгчээр алхам бүрийг нарийвчлан харна.
5. Reset товч анхны тохиргоог сэргээнэ.

Анхаарах зүйл:
- Анимаци нь 32 хүртэл тоотой үед ажиллана.
- Санамсаргүй өгөгдлийн хэмжээ 1–10000.
- Буруу оролт (хоосон, илүү таслал, бүхэл биш тоо, threshold < 1) ойлгомжтой алдааны цонхоор мэдэгдэнэ.

Тест ажиллуулах
    tests/SortTests.java нь JUnit тест биш, main() функцээр ажилладаг тусдаа тестийн програм юм. Иймээс mvn test командаар эдгээр тест автоматаар ажиллахгүй.

    Төслийн root хавтаснаас дараах командуудыг дарааллаар ажиллуулна:
        mkdir -p out
        javac -encoding UTF-8 -d out src/SortNode.java src/MergeSortTask.java src/QuickSortTask.java src/SortTrace.java tests/SortTests.java
        java -cp out SortTests

    Амжилттай ажиллавал хүлээгдэх үр дүн: passed=9111 failed=0

### Ашигласан технологи, сангууд
- **Java** — програмын үндсэн логик болон эрэмбэлэлтийн алгоритмуудыг хэрэгжүүлэхэд ашигласан.
- **JavaFX** — график хэрэглэгчийн интерфейс бүтээхэд ашигласан.
- **Java Fork-Join Framework** — эрэмбэлэлтийн дэд бодлогуудыг parallel боловсруулахад ашигласан.
- **Apache Maven** — төслийн хамаарал болон build тохиргоог удирдахад ашигласан.

### AI-ийн хэрэглээ
ChatGPT-ийг кодын бүтэц, алгоритмын ажиллагаа, алдаа засварлалтын талаар зөвлөгөө авахад ашигласан.

