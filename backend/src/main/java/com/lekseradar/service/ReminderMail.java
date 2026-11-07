package com.lekseradar.service;

import com.lekseradar.model.AssignmentEntity;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Текст письма-напоминания о домашке на языке ученика (nb/nn, en/uk, ru), сроки — в его часовом поясе.
 * Отдельно от отправки, чтобы проверять текст тестами.
 */
public final class ReminderMail {
    public record Mail(String subject, String body) {}

    private record Lang(Locale locale, String datePattern, String hello, String intro, String introNone,
                        String overdue, String due, String open, String off, String subjectOne, String subjectMany) {}

    private static final Lang NB = new Lang(Locale.forLanguageTag("nb-NO"), "EEEE d. MMMM 'kl.' HH:mm",
            "Hei %s!", "Disse leksene har frist snart:", "Du har ingen lekser med frist de neste dagene. Bra jobba!",
            "for sent", "frist", "Åpne leksene i LekseRadar:", "Påminnelser kan slås av under Innstillinger.",
            "LekseRadar: «%s» har frist snart", "LekseRadar: %d lekser har frist snart");
    private static final Lang EN = new Lang(Locale.forLanguageTag("en-GB"), "EEEE d MMMM, HH:mm",
            "Hi %s!", "These assignments are due soon:", "You have no assignments due in the next few days. Nice work!",
            "overdue", "due", "Open your homework in LekseRadar:", "You can turn reminders off in Settings.",
            "LekseRadar: “%s” is due soon", "LekseRadar: %d assignments are due soon");
    private static final Lang RU = new Lang(Locale.forLanguageTag("ru-RU"), "EEEE, d MMMM, HH:mm",
            "Привет, %s!", "Скоро срок сдачи этих заданий:", "В ближайшие дни сдавать нечего. Так держать!",
            "просрочено", "срок", "Открыть домашку в LekseRadar:", "Напоминания можно выключить в настройках.",
            "LekseRadar: скоро срок «%s»", "LekseRadar: скоро срок у заданий (%d)");

    private ReminderMail() {}

    static Lang lang(String locale) {
        if ("ru".equals(locale)) return RU;
        if ("en".equals(locale) || "uk".equals(locale)) return EN;
        return NB;
    }

    static ZoneId zone(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException | NullPointerException e) {
            return ZoneId.of("Europe/Oslo");
        }
    }

    /**
     * @param site адрес сайта без «/» в конце (например https://lekseradar.vercel.app) или пустая строка
     */
    public static Mail compose(String locale, String timezone, String name, List<AssignmentEntity> items,
                               String site, Instant now) {
        Lang l = lang(locale);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern(l.datePattern(), l.locale()).withZone(zone(timezone));
        StringBuilder b = new StringBuilder(l.hello().formatted(name)).append("\n\n");
        if (items.isEmpty()) {
            b.append(l.introNone()).append("\n");
        } else {
            b.append(l.intro()).append("\n\n");
            for (AssignmentEntity a : items) {
                b.append("• ").append(a.getTitle());
                if (a.getCourse() != null && !a.getCourse().isBlank()) b.append(" (").append(a.getCourse()).append(")");
                b.append("\n  ");
                if (a.getDueAt() != null && a.getDueAt().isBefore(now)) b.append(l.overdue()).append(": ");
                else b.append(l.due()).append(": ");
                b.append(a.getDueAt() == null ? "—" : fmt.format(a.getDueAt())).append("\n");
                if (a.getUrl() != null && a.getUrl().startsWith("http")) b.append("  ").append(a.getUrl()).append("\n");
            }
        }
        if (site != null && !site.isBlank()) b.append("\n").append(l.open()).append("\n").append(site).append("/homework\n");
        b.append("\n").append(l.off()).append("\n");
        String subject = items.size() == 1 ? l.subjectOne().formatted(items.get(0).getTitle())
                : l.subjectMany().formatted(items.size());
        if (items.isEmpty()) subject = "LekseRadar";
        return new Mail(subject, b.toString());
    }
}
