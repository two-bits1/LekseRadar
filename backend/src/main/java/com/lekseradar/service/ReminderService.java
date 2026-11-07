package com.lekseradar.service;

import com.lekseradar.dto.reminder.ReminderTestResponseDTO;
import com.lekseradar.exception.ApiException;
import com.lekseradar.exception.NotFoundException;
import com.lekseradar.exception.TooManyRequestsException;
import com.lekseradar.model.AssignmentEntity;
import com.lekseradar.model.ReminderEntity;
import com.lekseradar.model.UserEntity;
import com.lekseradar.repository.AssignmentRepository;
import com.lekseradar.repository.ReminderRepository;
import com.lekseradar.repository.UserRepository;
import com.lekseradar.security.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Напоминания о домашке на почту ученика (адрес из регистрации).
 * {@link #remindDue()}: одно письмо на ученика со всеми несданными заданиями, у которых срок в ближайшие 24 часа;
 * о каждом задании пишем один раз (V25). {@link #sendTest(Long)} — пробное письмо по кнопке в настройках.
 */
@Service
public class ReminderService {
    private static final Logger log = LoggerFactory.getLogger(ReminderService.class);
    static final String WINDOW = "24 hours";
    static final Duration TEST_WINDOW = Duration.ofDays(7);
    static final int TEST_PER_HOUR = 3;

    private final AssignmentRepository assignments;
    private final ReminderRepository reminders;
    private final UserRepository users;
    private final MailService mail;
    private final RateLimiter limiter;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public ReminderService(AssignmentRepository assignments, ReminderRepository reminders, UserRepository users,
                           MailService mail, RateLimiter limiter) {
        this(assignments, reminders, users, mail, limiter, Clock.systemUTC());
    }

    ReminderService(AssignmentRepository assignments, ReminderRepository reminders, UserRepository users,
                    MailService mail, RateLimiter limiter, Clock clock) {
        this.assignments = assignments;
        this.reminders = reminders;
        this.users = users;
        this.mail = mail;
        this.limiter = limiter;
        this.clock = clock;
    }

    /** @return сколько учеников получили письмо */
    public int remindDue() {
        Map<Long, List<AssignmentEntity>> byUser = assignments.findToRemind(WINDOW).stream()
                .collect(Collectors.groupingBy(AssignmentEntity::getUserId, LinkedHashMap::new, Collectors.toList()));
        int sent = 0;
        for (var e : byUser.entrySet()) {
            UserEntity user = users.findById(e.getKey()).orElse(null);
            if (user == null || !user.isEmailReminders()) continue;
            List<AssignmentEntity> items = e.getValue();
            if (!mail.sendReminder(user.getEmail(), compose(user, items))) continue;   // почта не настроена: попробуем позже
            sent++;
            for (AssignmentEntity a : items) {
                try {
                    reminders.save(ReminderEntity.sent(user.getId(), a.getId(), a.getTitle(), ReminderEntity.CHANNEL_EMAIL));
                } catch (DataIntegrityViolationException dup) {
                    // другая копия бэкенда уже записала это напоминание
                }
            }
        }
        if (sent > 0) log.info("email reminders: {} students", sent);
        return sent;
    }

    public ReminderTestResponseDTO sendTest(Long userId) {
        UserEntity user = users.findById(userId).orElseThrow(() -> new NotFoundException("Пользователь не найден"));
        if (!limiter.tryAcquire("reminder-test:" + userId, TEST_PER_HOUR, Duration.ofHours(1))) {
            throw new TooManyRequestsException("Пробное письмо можно отправить 3 раза в час", 3600L);
        }
        List<AssignmentEntity> items = assignments.findOpenDueBefore(userId, clock.instant().plus(TEST_WINDOW));
        if (!mail.sendReminder(user.getEmail(), compose(user, items))) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Отправка писем не настроена на сервере (MAIL_USERNAME, MAIL_PASSWORD)");
        }
        return new ReminderTestResponseDTO(user.getEmail(), items.size());
    }

    private ReminderMail.Mail compose(UserEntity user, List<AssignmentEntity> items) {
        Instant now = clock.instant();
        return ReminderMail.compose(user.getLocale(), user.getTimezone(), user.getDisplayName(), items, mail.site(), now);
    }
}
