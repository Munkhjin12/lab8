# Лаб 8 — Mock объектууд: Mockito-гоор хамаарлыг орлуулж тестлэх

- **Оюутан:** Л.Мөнхжин
- **Код:** B242270094
- **Хичээл:** F.CSA313 — Программ хангамжийн чанарын баталгаа ба туршилт

## Орчин


```text
openjdk version "21.0.10" 2026-01-20
OpenJDK Runtime Environment Homebrew (build 21.0.10)
OpenJDK 64-Bit Server VM Homebrew (build 21.0.10, mixed mode, sharing)
```

```text
Apache Maven 3.9.12 (848fbb4bf2d427b72bdb2471c22fced7ebd9a7a1)
Maven home: /opt/homebrew/Cellar/maven/3.9.12/libexec
Java version: 21.0.10, vendor: Homebrew, runtime: /opt/homebrew/Cellar/openjdk@21/21.0.10/libexec/openjdk.jdk/Contents/Home
Default locale: en_MN, platform encoding: UTF-8
OS name: "mac os x", version: "26.3.1", arch: "aarch64", family: "mac"
```

## Тестийн тоо

`results/mvn-test.txt`-ийн сүүлийн мөр: **Tests run: 10, Failures: 0, Errors: 0, Skipped: 0** — `BUILD SUCCESS`.
(7 энгийн тест A1–A7 + A8-ийн 3 мөр, мөр бүр тусдаа тест болж тоологдсон.)

## A8 дахь `lenient()`-ийн тайлбар

A8 параметржүүлсэн тестэд мөр бүр өөр замаар явдаг (жишээ нь `ERROR_NO_STUDENT` мөрд `courseExists`, `prerequisites` огт дуудагдахгүй) тул бүх мөрд нийтлэг stub тохируулахад MockitoExtension-ий strict stubs горим ашиглагдаагүй stub бүрийг `UnnecessaryStubbingException`-оор унагаадаг. Иймд `lenient().when(...)` хэрэглэж, "заавал ашиглагдах" шаардлагыг энэ тестэд л сулруулсан; бусад 7 тест strict хэвээр.

## Хэсэг B — мини мутаци

| № | Эвдрэл (CourseRegistrationService.java) | Унасан тестүүд | Үр дүн | Файл |
|---|---|---|---|---|
| 1 | `if (!student.active()) ...` комментлосон | `register_inactiveStudent_returnsInactiveErrorAndNeverTouchesEmail` (A3), `register_invalidScenario_returnsExpectedErrorCode` мөр [2] (A8) | Failures: 2, BUILD FAILURE | `results/mvn-test-mutant-1.txt` |
| 2 | `repository.saveRegistration(...)` комментлосон | `register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail` (A1), `register_emailSenderThrows_stillReturnsOkAndRegistrationSaved` (A5) | Failures: 2, BUILD FAILURE | `results/mvn-test-mutant-2.txt` |
| 3 | `|| courseId == null` устгасан | `register_nullArgument_throwsIllegalArgumentAndTouchesNoDependency` (A6) | Failures: 1, BUILD FAILURE | `results/mvn-test-mutant-3.txt` |


**Мутант 3-ын тухай:** A6-г эхнээс нь `studentId == null`, `courseId == null`, хоёулаа `null` гэсэн гурван тохиолдлыг тусад нь шалгадаг болгож бичсэн тул мутант 3-ыг эхний оролдлогоор барьсан — тестийн цоорхой илрээгүй. Хэрэв A6 зөвхөн нэг аргументыг шалгадаг байсан бол энэ мутант амьд үлдэх байсан.

**Дүгнэлт:** Гурван мутант бүгд баригдсан. Мутант 1 дээр A3 нь `ERROR_INACTIVE_STUDENT` оронд `ERROR_NO_COURSE` ирснээр унав: идэвхгүй оюутны шалгалтыг авмагц урсгал дараагийн алхам руу орсныг assertion шууд илрүүлсэн. Мутант 2-ыг зөвхөн `verify(...saveRegistration...)` барьж чадна, учир нь буцаах утга ("OK") өөрчлөгдөөгүй — энэ нь side-effect-ийг verify-аар шалгах шаардлагатайг харуулна. Анхаарах цоорхой: `ERROR_NO_COURSE` салбарыг ямар ч тест шууд шалгадаггүй (даалгаврын 8 тестэд ороогүй), тиймээс тэр шалгалтыг устгавал бүх тест ногоон хэвээр үлдэнэ.

## Дүгнэлт (mock, fake, verify)

Mock нь дуудалтыг бүртгэж, "энэ метод ийм аргументтай дуудагдсан уу" гэж шалгах боломж өгдөг, харин гараар бичсэн fake нь бодитоор ажилладаг хөнгөн хэрэгжүүлэлт (жишээ нь санах ойн `Map`-тай repository) бөгөөд төлөвөөр нь үр дүнг шалгадаг. Fake нь олон тестэд дахин ашиглагдаж, дотоод хэрэгжүүлэлтийн өөрчлөлтөд бага мэдрэмтгий боловч өөрөө засвар шаарддаг; mock нь хурдан тохируулагддаг ч тестийг кодын дотоод дуудалтын дараалалтай уяж өгдөг. Өгөгдлийн бүтэц (`Student` record), `List`, `String` зэрэг энгийн утга объект болон өөрийн тестлэж буй классыг mock хийх ёсгүй — эс бөгөөс бодит логик биш mock-ийн тохиргоог шалгасан тест болно. Мөн бидний мэдэхгүй гуравдагч этгээдийн сангийн төрлийг шууд mock хийхээс зайлсхийж, өөрсдийн нимгэн интерфейсээр (`StudentRepository`, `EmailSender`) ороож mock хийх нь зөв. `verify`-г хэтрүүлбэл (бүх дотоод дуудалт, дарааллыг шалгавал) тест нь зан төлөвийг биш хэрэгжүүлэлтийг шалгах болж, зөв рефакторинг хийхэд ч тест унадаг "хэврэг" тест үүснэ. Харин `verify`-г огт хийхгүй бол мутант 2-ын адил side-effect алга болсныг ямар ч тест анзаарахгүй. Иймд буцаах утга ба гадагш нөлөөлөх чухал side-effect-ийг (хадгалалт, илгээлт) шалгаж, үлдсэн дотоод дуудалтыг чөлөөтэй үлдээх тэнцвэрийг баримтлав.
