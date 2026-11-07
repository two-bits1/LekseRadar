package com.lekseradar.service;

import com.lekseradar.exception.ApiException;
import com.lekseradar.exception.TooManyRequestsException;
import com.lekseradar.model.AssignmentEntity;
import com.lekseradar.model.ReminderEntity;
import com.lekseradar.model.UserEntity;
import com.lekseradar.repository.AssignmentRepository;
import com.lekseradar.repository.ReminderRepository;
import com.lekseradar.repository.UserRepository;
import com.lekseradar.security.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static com.lekseradar.service.ReminderMailTest.hw;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReminderServiceTest {
    AssignmentRepository assignments = mock(AssignmentRepository.class);
    ReminderRepository reminders = mock(ReminderRepository.class);
    UserRepository users = mock(UserRepository.class);
    MailService mail = mock(MailService.class);
    ReminderService service;
    UserEntity user = mock(UserEntity.class);

    @BeforeEach
    void setUp() {
        service = new ReminderService(assignments, reminders, users, mail, new RateLimiter(),
                Clock.fixed(ReminderMailTest.NOW, ZoneOffset.UTC));
        when(user.getId()).thenReturn(7L);
        when(user.getEmail()).thenReturn("ola@example.com");
        when(user.getDisplayName()).thenReturn("Ola");
        when(user.getLocale()).thenReturn("nb");
        when(user.getTimezone()).thenReturn("Europe/Oslo");
        when(user.isEmailReminders()).thenReturn(true);
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(mail.site()).thenReturn("");
    }

    @Test
    void oneMailPerStudentAndEachAssignmentRecorded() {
        AssignmentEntity a = hw(1, "A", "", Instant.parse("2026-11-09T20:00:00Z"), null);
        AssignmentEntity b = hw(2, "B", "", Instant.parse("2026-11-10T08:00:00Z"), null);
        when(assignments.findToRemind("24 hours")).thenReturn(List.of(a, b));
        when(mail.sendReminder(eq("ola@example.com"), any())).thenReturn(true);

        assertEquals(1, service.remindDue());
        verify(mail, times(1)).sendReminder(eq("ola@example.com"), any());
        ArgumentCaptor<ReminderEntity> saved = ArgumentCaptor.forClass(ReminderEntity.class);
        verify(reminders, times(2)).save(saved.capture());
        assertEquals(List.of(1L, 2L), saved.getAllValues().stream().map(ReminderEntity::getAssignmentId).toList());
        assertTrue(saved.getAllValues().stream().allMatch(r -> "email".equals(r.getChannel()) && r.getSentAt() != null));
    }

    @Test
    void notRecordedWhenMailIsNotConfigured() {
        AssignmentEntity a = hw(1, "A", "", Instant.parse("2026-11-09T20:00:00Z"), null);
        when(assignments.findToRemind("24 hours")).thenReturn(List.of(a));
        when(mail.sendReminder(any(), any())).thenReturn(false);
        assertEquals(0, service.remindDue());
        verify(reminders, never()).save(any());   // отправим, когда почту настроят
    }

    @Test
    void studentWhoTurnedRemindersOffGetsNothing() {
        when(user.isEmailReminders()).thenReturn(false);
        AssignmentEntity a = hw(1, "A", "", Instant.parse("2026-11-09T20:00:00Z"), null);
        when(assignments.findToRemind("24 hours")).thenReturn(List.of(a));
        assertEquals(0, service.remindDue());
        verify(mail, never()).sendReminder(any(), any());
    }

    @Test
    void testMailListsTheWeekAndIsRateLimited() {
        AssignmentEntity a = hw(1, "A", "", Instant.parse("2026-11-12T20:00:00Z"), null);
        when(assignments.findOpenDueBefore(eq(7L), eq(Instant.parse("2026-11-16T10:00:00Z")))).thenReturn(List.of(a));
        when(mail.sendReminder(any(), any())).thenReturn(true);
        for (int i = 0; i < 3; i++) assertEquals(1, service.sendTest(7L).count());
        assertThrows(TooManyRequestsException.class, () -> service.sendTest(7L));
    }

    @Test
    void testMailWithoutMailSetupIs503() {
        when(assignments.findOpenDueBefore(anyLong(), any())).thenReturn(List.of());
        when(mail.sendReminder(any(), any())).thenReturn(false);
        ApiException e = assertThrows(ApiException.class, () -> service.sendTest(7L));
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, e.getStatus());
    }
}
