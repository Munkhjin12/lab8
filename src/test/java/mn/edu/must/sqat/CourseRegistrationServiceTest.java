package mn.edu.must.sqat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    private static final String STUDENT_ID = "S001";
    private static final String COURSE_ID = "CS201";
    private static final String PREREQ = "MATH101";
    private static final String EMAIL = "bat@must.edu.mn";

    @Mock StudentRepository repository;

    @Mock EmailSender emailSender;

    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    // ---- туслах: оюутан үүсгэх ----
    private static Student student(boolean active, List<String> taken) {
        return new Student(STUDENT_ID, EMAIL, active, taken);
    }

    // A1: when + verify
    @Test
    void register_activeStudentWithPrerequisites_returnsOkSavesAndSendsEmail() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, List.of(PREREQ)));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of(PREREQ));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
        verify(emailSender).send(eq(EMAIL), anyString(), anyString());
    }

    // A2: verify(..., never())
    @Test
    void register_unknownStudent_returnsNoStudentAndDoesNotSaveOrEmail() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(null);

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A3: verifyNoInteractions
    @Test
    void register_inactiveStudent_returnsInactiveErrorAndNeverTouchesEmail() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(false, List.of()));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verifyNoInteractions(emailSender);
        verify(repository, never()).saveRegistration(anyString(), anyString());
    }

    // A4: when + never
    @Test
    void register_missingPrerequisite_returnsPrerequisitesErrorAndDoesNotSave() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, List.of()));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of(PREREQ));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // A5: doThrow(...).when(...)
    @Test
    void register_emailSenderThrows_stillReturnsOkAndRegistrationSaved() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, List.of(PREREQ)));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of(PREREQ));
        doThrow(new RuntimeException("SMTP server down"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals("OK", result);
        verify(repository).saveRegistration(STUDENT_ID, COURSE_ID);
    }

    // A6: assertThrows — аргумент тус бүрийн null-ийг тусад нь шалгана
    @Test
    void register_nullArgument_throwsIllegalArgumentAndTouchesNoDependency() {
        assertThrows(IllegalArgumentException.class, () -> service.register(null, COURSE_ID));
        assertThrows(IllegalArgumentException.class, () -> service.register(STUDENT_ID, null));
        assertThrows(IllegalArgumentException.class, () -> service.register(null, null));

        verifyNoInteractions(repository, emailSender);
    }

    // A7: ArgumentCaptor
    @Test
    void register_success_emailBodyContainsCourseCode() {
        when(repository.findStudent(STUDENT_ID)).thenReturn(student(true, List.of(PREREQ)));
        when(repository.courseExists(COURSE_ID)).thenReturn(true);
        when(repository.prerequisites(COURSE_ID)).thenReturn(List.of(PREREQ));
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        service.register(STUDENT_ID, COURSE_ID);

        verify(emailSender).send(eq(EMAIL), anyString(), bodyCaptor.capture());
        assertTrue(bodyCaptor.getValue().contains(COURSE_ID),
                "И-мэйлийн бие хичээлийн кодыг агуулах ёстой: " + bodyCaptor.getValue());
    }

    // A8: параметржүүлсэн тест — гурван алдааны анги
    @ParameterizedTest(name = "[{index}] found={0}, active={1}, prereqTaken={2} -> {3}")
    @CsvSource({
            "false, true,  true,  ERROR_NO_STUDENT",
            "true,  false, true,  ERROR_INACTIVE_STUDENT",
            "true,  true,  false, ERROR_PREREQUISITES"
    })
    void register_invalidScenario_returnsExpectedErrorCode(
            boolean studentFound, boolean active, boolean prereqTaken, String expected) {
        // Мөр бүр өөр stub ашиглана (жишээ нь NO_STUDENT мөрд courseExists огт дуудагдахгүй),
        // тиймээс strict stubs-ийн UnnecessaryStubbingException-ээс сэргийлж lenient() хэрэглэв.
        Student s = studentFound
                ? student(active, prereqTaken ? List.of(PREREQ) : List.of())
                : null;
        lenient().when(repository.findStudent(STUDENT_ID)).thenReturn(s);
        lenient().when(repository.courseExists(COURSE_ID)).thenReturn(true);
        lenient().when(repository.prerequisites(COURSE_ID)).thenReturn(List.of(PREREQ));

        String result = service.register(STUDENT_ID, COURSE_ID);

        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }
}
