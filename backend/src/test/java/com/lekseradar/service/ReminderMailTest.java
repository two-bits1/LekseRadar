package com.lekseradar.service;

import com.lekseradar.model.AssignmentEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReminderMailTest {
    static final Instant NOW = Instant.parse("2026-11-09T10:00:00Z");

    static AssignmentEntity hw(long id, String title, String course, Instant due, String url) {
        AssignmentEntity a = mock(AssignmentEntity.class);
        when(a.getId()).thenReturn(id);
        when(a.getUserId()).thenReturn(7L);
        when(a.getTitle()).thenReturn(title);
        when(a.getCourse()).thenReturn(course);
        when(a.getDueAt()).thenReturn(due);
        when(a.getUrl()).thenReturn(url);
        return a;
    }

    @Test
    void norwegianByDefaultWithLocalTimeAndLink() {
        var a = hw(1, "Matte 3.1", "Matematikk 1T", Instant.parse("2026-11-10T21:59:00Z"), "https://skole.instructure.com/x");
        var m = ReminderMail.compose("nb", "Europe/Oslo", "Ola", List.of(a), "https://lekseradar.app", NOW);
        assertEquals("LekseRadar: «Matte 3.1» har frist snart", m.subject());
        assertTrue(m.body().startsWith("Hei Ola!"));
        assertTrue(m.body().contains("• Matte 3.1 (Matematikk 1T)"));
        assertTrue(m.body().contains("frist: tirsdag 10. november kl. 22:59"), m.body());   // UTC+1 в ноябре
        assertTrue(m.body().contains("https://skole.instructure.com/x"));
        assertTrue(m.body().contains("https://lekseradar.app/homework"));
    }

    @Test
    void russianAndEnglishTextsAndOverdueMark() {
        var late = hw(1, "Эссе", "", Instant.parse("2026-11-08T10:00:00Z"), null);
        var soon = hw(2, "Тест", "Физика", Instant.parse("2026-11-09T18:00:00Z"), "javascript:alert(1)");
        var ru = ReminderMail.compose("ru", "Europe/Oslo", "Илья", List.of(late, soon), "", NOW);
        assertEquals("LekseRadar: скоро срок у заданий (2)", ru.subject());
        assertTrue(ru.body().contains("просрочено: "));
        assertTrue(ru.body().contains("• Эссе\n"));                 // пустой предмет без скобок
        assertFalse(ru.body().contains("javascript:"));             // в письмо попадают только http(s)-ссылки
        assertFalse(ru.body().contains("/homework"));               // адрес сайта не задан
        var en = ReminderMail.compose("uk", "bad/zone", "Kim", List.of(soon), "", NOW);
        assertTrue(en.body().startsWith("Hi Kim!"));
        assertTrue(en.body().contains("due: Monday 9 November, 19:00"), en.body());   // неизвестный пояс → Europe/Oslo
    }

    @Test
    void emptyListSaysNothingIsDue() {
        var m = ReminderMail.compose("nb", "Europe/Oslo", "Ola", List.of(), "", NOW);
        assertEquals("LekseRadar", m.subject());
        assertTrue(m.body().contains("ingen lekser"));
    }
}
